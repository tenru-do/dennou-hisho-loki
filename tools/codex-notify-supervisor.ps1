param([switch]$Once)
$ErrorActionPreference='Stop'
$created=$false;$mutex=New-Object Threading.Mutex($true,'Local\RokiCodexNotifySupervisor',[ref]$created)
if(!$created){$mutex.Dispose();return}
. (Join-Path $PSScriptRoot 'codex-notify-runtime.ps1')

$watcher=Join-Path $PSScriptRoot 'codex-glass-notify.ps1';$healthPath=Join-Path $PSScriptRoot 'codex-notify-health.local.json';$statusPath=Join-Path $PSScriptRoot 'codex-notify-supervisor.local.json'
$exe=Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0\powershell.exe';$attempt=0;$child=$null;$lastExitReason="";$lastExitAt=0L;$lastExitCode=0;
$eventPath=Join-Path $PSScriptRoot 'codex-notify-events.local.jsonl'
try{
 Write-LokiNotifyEvent $eventPath 'supervisor_started'
 $session=[Diagnostics.Process]::GetCurrentProcess().SessionId
 foreach($old in @(Get-CimInstance Win32_Process -Filter "Name='powershell.exe'")){
  if($old.ProcessId -eq $PID -or $old.SessionId -ne $session -or !$old.CommandLine){continue}
  if($old.CommandLine -match ([regex]::Escape($watcher)) -and $old.CommandLine -match '(?i)\s-Watch(?:\s|$)'){
   Write-LokiNotifyEvent $eventPath 'previous_worker_replaced' $old.ProcessId
   Stop-Process -Id $old.ProcessId -ErrorAction SilentlyContinue
  }
 }
 do{
  $started=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds();$child=Start-Process -FilePath $exe -ArgumentList @('-NoProfile','-WindowStyle','Hidden','-ExecutionPolicy','Bypass','-File',('"'+$watcher+'"'),'-Watch') -WindowStyle Hidden -PassThru
  Save-LokiNotifyState $statusPath @{pid=$PID;childPid=$child.Id;at=$started;phase='running';restarts=$attempt;lastExitReason=$lastExitReason;lastExitAt=$lastExitAt;lastExitCode=$lastExitCode;eventWriteFailed=[bool]$script:notifyEventWriteFailed}
  Write-LokiNotifyEvent $eventPath 'worker_started' $child.Id
  $stalled=$false
  while(!$child.WaitForExit(5000)){
   $now=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds();$health=$null;try{$health=Read-LokiNotifyState $healthPath $null}catch{}
   if(Get-LokiNotifyResumeDecision $health $now $child.Id $started){
    # Stop only the exact child process object created above, never Codex or Android apps.
    $child.Kill();$child.WaitForExit();$stalled=$true;break
   }
  }
  $attempt++;$now=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds();$lastExitReason=if($stalled){'heartbeat_timeout'}else{'worker_exit'};$lastExitAt=$now;$lastExitCode=$child.ExitCode;Write-LokiNotifyEvent $eventPath $lastExitReason $child.Id $lastExitCode
  Save-LokiNotifyState $statusPath @{pid=$PID;childPid=$child.Id;at=$now;phase=if($stalled){'heartbeat_timeout'}else{'worker_exit'};exitCode=$child.ExitCode;restarts=$attempt}
  if($Once){break};if($now-$started -gt 300000){$attempt=1};Start-Sleep -Seconds ([Math]::Min(60,5*[Math]::Max(1,$attempt)))
 }while($true)
}finally{$mutex.ReleaseMutex();$mutex.Dispose()}
