param([string]$Output)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'codex-notify-runtime.ps1')
. (Join-Path $PSScriptRoot 'codex-notify-policy.ps1')
$testRoot=Join-Path ([IO.Path]::GetTempPath()) ('loki-notify-check-'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $testRoot|Out-Null
$checks=0
function Check([bool]$value,[string]$name){$script:checks++;if(!$value){throw ('FAIL '+$name)}}
$utf8=New-Object Text.UTF8Encoding($false)
try{
 $file=Join-Path $testRoot 'tail.jsonl';$text='{"type":"event_msg","payload":{"type":"task_complete","last_agent_message":"日本語の合成通知"}}'+[char]10;$bytes=$utf8.GetBytes($text)
 for($split=1;$split -lt $bytes.Length;$split++){
  [IO.File]::WriteAllBytes($file,[byte[]]$bytes[0..($split-1)]);$a=[LokiNotifyRuntime]::Read($file,0,$false,4194304)
  Check ($a.Text -eq '' -and $a.Next -eq 0) 'partial record retained'
  $f=[IO.File]::Open($file,'Append','Write','ReadWrite');try{$f.Write($bytes,$split,$bytes.Length-$split)}finally{$f.Dispose()}
  $b=[LokiNotifyRuntime]::Read($file,$a.Next,$a.Discarding,4194304,$a.Anchor);Check ($b.Text -ceq $text -and $b.Next -eq $bytes.Length) 'split UTF8 record delivered once'
  $c=[LokiNotifyRuntime]::Read($file,$b.Next,$false,4194304,$b.Anchor);Check ($c.Text -eq '' -and $c.Next -eq $b.Next) 'no duplicate on idle poll'
 }
 [IO.File]::WriteAllText($file,$text,$utf8);$a=[LokiNotifyRuntime]::Read($file,0,$false,4194304)
 [IO.File]::WriteAllText($file,('{"new":true}'+[char]10),$utf8);$b=[LokiNotifyRuntime]::Read($file,$a.Next,$false,4194304,$a.Anchor);Check ($b.Truncated -and $b.Text.Contains('"new"')) 'short truncation'
 [IO.File]::WriteAllText($file,$text,$utf8);$a=[LokiNotifyRuntime]::Read($file,0,$false,4194304);$replacement=$text.Replace('日本語','書換え')+$text
 [IO.File]::WriteAllText($file,$replacement,$utf8);$b=[LokiNotifyRuntime]::Read($file,$a.Next,$false,4194304,$a.Anchor);Check ($b.Truncated -and $b.Text -ceq $replacement) 'same-length or larger replacement detected'
 [IO.File]::WriteAllText($file,('x'*160)+[char]10+'{"valid":true}'+[char]10,$utf8)
 $a=[LokiNotifyRuntime]::Read($file,0,$false,64);Check ($a.Oversized -and $a.Discarding) 'oversized record begins discard'
 $b=[LokiNotifyRuntime]::Read($file,$a.Next,$a.Discarding,64,$a.Anchor);$c=[LokiNotifyRuntime]::Read($file,$b.Next,$b.Discarding,64,$b.Anchor);Check ($c.Text.Contains('"valid"') -and !$c.Discarding) 'oversized remainder skipped to next complete record'
 $state=Join-Path $testRoot 'state.local.json';Save-LokiNotifyState $state @{value=1};Save-LokiNotifyState $state @{value=2};Check ((Read-LokiNotifyState $state $null).value -eq 2) 'atomic latest state'
 [IO.File]::WriteAllText($state,'{broken',$utf8);Check ((Read-LokiNotifyState $state $null).value -eq 1) 'corrupt primary recovers backup'
 [IO.File]::WriteAllText(($state+'.bak'),'{broken',$utf8);$failed=$false;try{$null=Read-LokiNotifyState $state @()}catch{$failed=$true};Check $failed 'corrupt state is preserved and rejected'
 Check (([IO.File]::ReadAllText($state)) -eq '{broken') 'failed state read did not overwrite data'
 Check (!(Get-LokiNotifyResumeDecision @{pid=123;at=200000} 210000 123 100000)) 'fresh heartbeat does not restart'
 Check (Get-LokiNotifyResumeDecision @{pid=123;at=1000} 210000 123 1000) 'stale heartbeat restarts'
 Check (!(Get-LokiNotifyResumeDecision @{pid=999;at=210000} 210000 123 200000)) 'unrelated PID gets startup grace'
 Check (Get-LokiNotifyResumeDecision @{pid=999;at=210000} 500000 123 200000) 'wrong PID cannot keep dead worker healthy'
 $now=[DateTimeOffset]::UtcNow;$env1=New-LokiCompletionEnvelope '合成の完了テスト' 'synthetic-session' $now;$env2=New-LokiCompletionEnvelope '合成の完了テスト' 'synthetic-session' $now
 Check (($env1|ConvertFrom-Json).id -eq ($env2|ConvertFrom-Json).id) 'restart replay stable ID'
 Check (!(New-LokiCompletionEnvelope '古い合成通知' 'synthetic-session' $now.AddMinutes(-6))) 'expired completions not replayed'
 Check (!(New-LokiCompletionEnvelope '未来の合成通知' 'synthetic-session' $now.AddMinutes(2))) 'future invalid completion excluded'
 $question=@{type='response_item';timestamp=$now.ToString('o');payload=@{type='function_call';name='request_user_input_async';call_id='test-call';arguments=(@{questions=@(@{title='合成の確認です'})}|ConvertTo-Json -Depth 5 -Compress)}}
 Check (@(New-LokiQuestionEnvelopes $question 'synthetic-session').Count -eq 1) 'human question remains supported'
 $approval=@{type='response_item';timestamp=$now.ToString('o');payload=@{type='custom_tool_call';name='exec';input='risk auto_review'}}
 Check (@(New-LokiQuestionEnvelopes $approval 'synthetic-session').Count -eq 0) 'approval review never becomes spoken completion'
 $result=@{passed=$checks;scope='synthetic files, UTF8 boundaries, restart checkpoints, corruption, heartbeat decisions and envelope rules';externalApiCalls=0;deviceChanges=0}
 if($Output){$result|ConvertTo-Json|Set-Content -LiteralPath $Output -Encoding UTF8};$result|ConvertTo-Json
}finally{
 # Only the uniquely created test folder and its own files are removed.
 $resolved=[IO.Path]::GetFullPath($testRoot);$expected=[IO.Path]::GetFullPath([IO.Path]::GetTempPath())
 if(!$resolved.StartsWith($expected,[StringComparison]::OrdinalIgnoreCase) -or !(Split-Path $resolved -Leaf).StartsWith('loki-notify-check-')){throw 'Unsafe cleanup target'}
 Remove-Item -LiteralPath $resolved -Recurse -Force
}
