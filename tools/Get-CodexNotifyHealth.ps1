param([switch]$AsJson)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'codex-notify-runtime.ps1')
$now=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$owner=Read-LokiNotifyState (Join-Path $PSScriptRoot 'codex-notify-supervisor.local.json') $null
$health=Read-LokiNotifyState (Join-Path $PSScriptRoot 'codex-notify-health.local.json') $null
$parent=$null;$worker=$null
if($owner){$parent=Get-Process -Id $owner.pid -ErrorAction SilentlyContinue;$worker=Get-Process -Id $owner.childPid -ErrorAction SilentlyContinue}
$age=if($health){[math]::Round(($now-[long]$health.at)/1000,1)}else{$null}
$healthy=$null -ne $parent -and $null -ne $worker -and $health -and $owner.childPid -eq $health.pid -and $age -ge 0 -and $age -lt 30 -and $health.phase -eq 'polling'
$task=Get-ScheduledTask -TaskName LokiCodexNotifications -ErrorAction SilentlyContinue
$result=[pscustomobject]@{status=if($healthy){'healthy'}else{'needs_attention'};supervisorRunning=($null -ne $parent);workerRunning=($null -ne $worker);heartbeatAgeSeconds=$age;phase=if($health){$health.phase}else{'missing'};queuedNotifications=if($health){$health.queued}else{$null};watchedSessions=if($health){$health.files}else{$null};scheduledRecoveryEnabled=($null -ne $task -and $task.State -ne 'Disabled');restartsSinceSupervisorStart=if($owner){$owner.restarts}else{$null};errorType=if($health){$health.errorType}else{''}}
if($AsJson){$result|ConvertTo-Json}else{$result}
if(!$healthy){exit 2}
