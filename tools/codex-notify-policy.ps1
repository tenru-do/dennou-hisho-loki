function Test-LokiHumanCompletion([string]$message) {
 if([string]::IsNullOrWhiteSpace($message)){return $false}
 return !($message.Trim().StartsWith('{') -or $message -match '(?i)risk|auto.?review|approvals_reviewer|sandbox_permissions')
}
function New-LokiCompletionEnvelope([string]$message,[string]$identity,[DateTimeOffset]$at) {
 $now=[DateTimeOffset]::UtcNow
 if(!(Test-LokiHumanCompletion $message) -or [string]::IsNullOrWhiteSpace($identity) -or ($now-$at).TotalSeconds -gt 300 -or ($at-$now).TotalSeconds -gt 30){return ''}
 $algorithm=[Security.Cryptography.SHA256]::Create()
 try{$id=([BitConverter]::ToString($algorithm.ComputeHash([Text.Encoding]::UTF8.GetBytes($identity+'|'+$at.ToUnixTimeMilliseconds()+'|'+$message)))).Replace('-','').ToLowerInvariant()}finally{$algorithm.Dispose()}
 $excerpt=$message.Trim()
 if($excerpt.StartsWith('```')){$excerpt=''}
 $excerpt=$excerpt -replace '\[([^\]]+)\]\([^)]+\)','$1' -replace '\[\[.*?\]\]','' -replace '[#*`]+','' -replace '\s+',' '
 if($excerpt.Length -gt 180){$excerpt=$excerpt.Substring(0,180)+'。続きはPCで確認してください。'}
 $text='Codexから返答が届きました。'+$excerpt
 return (@{kind='completed';id=$id;issuedAt=$at.ToUnixTimeMilliseconds();message=$text} | ConvertTo-Json -Compress)
}
function New-LokiQuestionEnvelopes($record,[string]$identity) {
 if($record.type -ne 'response_item'){return}
 $p=$record.payload
 if($p.type -ne 'function_call' -or $p.name -notin @('request_user_input_async','request_user_input')){return}
 try{
  $at=[DateTimeOffset]::Parse([string]$record.timestamp);$now=[DateTimeOffset]::UtcNow
  if(($now-$at).TotalSeconds -gt 300 -or ($at-$now).TotalSeconds -gt 30){return}
  $args=[string]$p.arguments|ConvertFrom-Json;$index=0
  foreach($q in $args.questions){
   $text=if($p.name -eq 'request_user_input_async'){[string]$q.title}else{[string]$q.question}
   $text=($text -replace '\s+',' ').Trim()
   if(!(Test-LokiHumanCompletion $text) -or $text.Length -gt 700){continue}
   $key=$identity+'|'+[string]$p.call_id+'|'+$index;$index++
   $hash=[Security.Cryptography.SHA256]::Create()
   try{$id=([BitConverter]::ToString($hash.ComputeHash([Text.Encoding]::UTF8.GetBytes($key)))).Replace('-','').ToLowerInvariant()}finally{$hash.Dispose()}
   @{kind='question';id=$id;issuedAt=$at.ToUnixTimeMilliseconds();message=('Codexから確認です。'+$text)}|ConvertTo-Json -Compress
  }
 }catch{}
}
function Test-LokiProjectSession([string]$path,[string[]]$roots) {
 try{$first=Get-Content -LiteralPath $path -TotalCount 1 | ConvertFrom-Json;if($first.type -ne 'session_meta'){return $false};$cwd=[string]$first.payload.cwd;if([string]$first.payload.source -match 'subagent|review'){return $false};foreach($root in $roots){if([IO.Path]::GetFullPath($cwd).TrimEnd('\') -eq [IO.Path]::GetFullPath($root).TrimEnd('\')){return $true}}}catch{}
 return $false
}
