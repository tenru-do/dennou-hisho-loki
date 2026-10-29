param([string]$Output)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'codex-notify-runtime.ps1')
. (Join-Path $PSScriptRoot 'codex-notify-policy.ps1')
$root=Join-Path ([IO.Path]::GetTempPath()) ('loki-notify-recovery-'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $root|Out-Null
$exe=Join-Path $env:SystemRoot 'System32/WindowsPowerShell/v1.0/powershell.exe'
$utf8=New-Object Text.UTF8Encoding($true);$plain=New-Object Text.UTF8Encoding($false);$processes=@();$checks=0
function Check([bool]$value,[string]$name){$script:checks++;if(!$value){throw ('FAIL '+$name)}}
function Wait-For($condition,[int]$seconds=12){$watch=[Diagnostics.Stopwatch]::StartNew();do{if(& $condition){return};Start-Sleep -Milliseconds 100}while($watch.Elapsed.TotalSeconds -lt $seconds);throw 'Timed out waiting for synthetic recovery'}
function Append([string]$path,[string]$value){[IO.File]::AppendAllText($path,$value,$plain)}
function Sent-Count{if(Test-Path (Join-Path $root 'sent.txt')){return @(Get-Content (Join-Path $root 'sent.txt')).Count};return 0}
function Launch-Worker{
 $p=Start-Process -FilePath $exe -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-File',('"'+(Join-Path $root 'worker.ps1')+'"')) -WindowStyle Hidden -PassThru -RedirectStandardError (Join-Path $root ('err-'+[guid]::NewGuid().ToString('N')+'.txt'))
 $script:processes+=@($p);return $p
}
function Stop-Owned($p){if($p -and !$p.HasExited){$p.Kill();$p.WaitForExit()}}
function Event-Line([string]$label){return (@{type='event_msg';timestamp=[DateTimeOffset]::UtcNow.ToString('o');payload=@{type='task_complete';last_agent_message=$label}}|ConvertTo-Json -Depth 6 -Compress)+[char]10}
try{
 $logs=Join-Path $root 'sessions';New-Item -ItemType Directory $logs|Out-Null
 $meta=(@{type='session_meta';payload=@{cwd=$root;source='cli'}}|ConvertTo-Json -Depth 5 -Compress)+[char]10
 $log=Join-Path $logs 'one.jsonl';$other=Join-Path $logs 'two.jsonl'
 [IO.File]::WriteAllText($log,$meta+(Event-Line 'old synthetic result'),$plain);[IO.File]::WriteAllText($other,$meta,$plain)
 Save-LokiNotifyState (Join-Path $root 'config.json') @{projectRoots=@($root)}
 $tokens=$null;$errors=$null;$tree=[Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot 'codex-glass-notify.ps1'),[ref]$tokens,[ref]$errors);Check (!$errors.Count) 'production watcher syntax'
 $functions=''
 foreach($name in @('Save-NotifyOutbox','Queue-NotifyEnvelope','Flush-NotifyOutbox','Watch-CodexApprovals')){
  $node=$tree.FindAll({param($n)$n -is [Management.Automation.Language.FunctionDefinitionAst]},$false)|Where-Object {$_.Name -eq $name}|Select-Object -First 1
  if(!$node){throw ('Missing production function '+$name)};$functions+=$node.Extent.Text+[char]10
 }
 $functions=$functions.Replace("'Local\RokiCodexNotifyWatcher'","'Local\LokiSyntheticWatcher-"+[guid]::NewGuid().ToString('N')+"'").Replace("Join-Path "+'$env:USERPROFILE '+ "'.codex\sessions'","Join-Path "+'$scriptDir '+ "'sessions'").Replace('-ge 15000','-ge 250').Replace('-ge 10000','-ge 200').Replace('Start-Sleep -Seconds 2','Start-Sleep -Milliseconds 100').Replace('+15000','+500')
 $header=". '"+(Join-Path $PSScriptRoot 'codex-notify-runtime.ps1')+"'"+[char]10+". '"+(Join-Path $PSScriptRoot 'codex-notify-policy.ps1')+"'"+[char]10
 $header+='$ErrorActionPreference=''Stop'';$scriptDir=$PSScriptRoot;$configPath=Join-Path $scriptDir ''config.json'';$outboxPath=Join-Path $scriptDir ''outbox.json'';$script:notifyOutbox=@(Read-LokiNotifyState $outboxPath @())'+[char]10
 $stub='function Write-NotifyLog([string]$t){[IO.File]::AppendAllText((Join-Path $scriptDir ''events.txt''),$t+[char]10)}'+[char]10+
 'function Send-GlassNotification([string]$message){if(!(Test-Path (Join-Path $scriptDir ''online''))){return $false};$data=$message|ConvertFrom-Json;[IO.File]::AppendAllText((Join-Path $scriptDir ''sent.txt''),$data.id+[char]10);return $true}'+[char]10
 [IO.File]::WriteAllText((Join-Path $root 'worker.ps1'),$header+$stub+$functions+[char]10+'Watch-CodexApprovals',$utf8)
 [IO.File]::WriteAllText((Join-Path $root 'online'),'yes',$plain)
 $worker=Launch-Worker;Wait-For {Test-Path (Join-Path $root 'codex-notify-checkpoint.local.json')};Check ((Sent-Count) -eq 0) 'initial boot skips historical completion'
 $line=Event-Line 'split synthetic result';$data=$plain.GetBytes($line);$half=[int]($data.Length/2);$f=[IO.File]::Open($log,'Append','Write','ReadWrite');try{$f.Write($data,0,$half)}finally{$f.Dispose()}
 Start-Sleep -Milliseconds 500;Check ((Sent-Count) -eq 0) 'partial event never emitted'
 $f=[IO.File]::Open($log,'Append','Write','ReadWrite');try{$f.Write($data,$half,$data.Length-$half)}finally{$f.Dispose()}
 Wait-For {(Sent-Count) -eq 1};Check ((Sent-Count) -eq 1) 'completed event emitted once'
 Remove-Item -LiteralPath (Join-Path $root 'online')
 Append $log (Event-Line 'offline synthetic result')
 Wait-For {try{@(Get-Content (Join-Path $root 'outbox.json') -Raw|ConvertFrom-Json).Count -eq 1}catch{$false}}
 $before=Get-Content (Join-Path $root 'outbox.json') -Raw|ConvertFrom-Json;Stop-Owned $worker
 $worker=Launch-Worker;Start-Sleep -Milliseconds 500;Check ((Sent-Count) -eq 1) 'offline restart does not pretend delivery'
 $after=Get-Content (Join-Path $root 'outbox.json') -Raw|ConvertFrom-Json;Check ($before[0].id -eq $after[0].id) 'offline outbox identity retained across process death'
 [IO.File]::WriteAllText((Join-Path $root 'online'),'yes',$plain);Wait-For {(Sent-Count) -eq 2};Check ((Sent-Count) -eq 2) 'connection restoration retries once'
 Stop-Owned $worker;$worker=Launch-Worker;Start-Sleep -Milliseconds 700;Check ((Sent-Count) -eq 2) 'checkpoint avoids duplicate after second restart'
 $lease=[IO.File]::Open($other,'Open','ReadWrite','None')
 try{Append $log ('{broken'+[char]10+(Event-Line 'after malformed record'));Wait-For {(Sent-Count) -eq 3};Check (!$worker.HasExited) 'locked peer file and malformed record do not stop watcher'}finally{$lease.Dispose()}
 Stop-Owned $worker
 # Run a real supervisor against a harmless worker with deliberately shortened test timeouts.
 $fixture=Join-Path $root 'supervisor';New-Item -ItemType Directory $fixture|Out-Null
 $runtime=[IO.File]::ReadAllText((Join-Path $PSScriptRoot 'codex-notify-runtime.ps1')).Replace('$timeout=180000','$timeout=1200')
 [IO.File]::WriteAllText((Join-Path $fixture 'codex-notify-runtime.ps1'),$runtime,$utf8)
 $supervisor=[IO.File]::ReadAllText((Join-Path $PSScriptRoot 'codex-notify-supervisor.ps1')).Replace('Local\RokiCodexNotifySupervisor','Local\LokiSyntheticSupervisor-'+[guid]::NewGuid().ToString('N')).Replace('WaitForExit(5000)','WaitForExit(200)')
 [IO.File]::WriteAllText((Join-Path $fixture 'codex-notify-supervisor.ps1'),$supervisor,$utf8)
 [IO.File]::WriteAllText((Join-Path $fixture 'codex-glass-notify.ps1'),'param([switch]$Watch) Start-Sleep -Seconds 30',$utf8)
 $super=Start-Process -FilePath $exe -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-File',('"'+(Join-Path $fixture 'codex-notify-supervisor.ps1')+'"'),'-Once') -WindowStyle Hidden -PassThru;$processes+=@($super)
 Wait-For {Test-Path (Join-Path $fixture 'codex-notify-supervisor.local.json')}
 $child=(Read-LokiNotifyState (Join-Path $fixture 'codex-notify-supervisor.local.json') $null).childPid
 Check ($super.WaitForExit(10000)) 'stalled synthetic worker detected within deadline'
 $state=Read-LokiNotifyState (Join-Path $fixture 'codex-notify-supervisor.local.json') $null
 Check ($state.phase -eq 'heartbeat_timeout' -and $state.restarts -eq 1) 'supervisor records exact stall reason'
 Check (!(Get-Process -Id $child -ErrorAction SilentlyContinue)) 'only synthetic child terminated'
 $result=@{passed=$checks;scope='production watcher functions with synthetic sessions and stubbed delivery; process death, resume, offline retry, malformed and locked files; real supervisor stall detection using isolated worker';externalApiCalls=0;deviceChanges=0}
 if($Output){$result|ConvertTo-Json|Set-Content -LiteralPath $Output -Encoding UTF8};$result|ConvertTo-Json
}catch{
 foreach($errorFile in @(Get-ChildItem $root -Filter 'err-*.txt')){Get-Content -LiteralPath $errorFile.FullName -Tail 8}
 throw
}finally{
 foreach($p in $processes){Stop-Owned $p}
 $resolved=[IO.Path]::GetFullPath($root);$parent=[IO.Path]::GetFullPath([IO.Path]::GetTempPath())
 if(!$resolved.StartsWith($parent,[StringComparison]::OrdinalIgnoreCase) -or !(Split-Path $resolved -Leaf).StartsWith('loki-notify-recovery-')){throw 'Unsafe cleanup target'}
 Remove-Item -LiteralPath $resolved -Recurse -Force
}
