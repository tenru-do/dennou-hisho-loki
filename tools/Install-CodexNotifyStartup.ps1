param([switch]$Remove)
$ErrorActionPreference='Stop'
$scriptPath=Join-Path $PSScriptRoot 'codex-notify-supervisor.ps1'
$configPath=Join-Path $PSScriptRoot 'token-codex-notify.local.json'
$key='HKCU:/Software/Microsoft/Windows/CurrentVersion/Run'
$name='LokiCodexNotifications'
if($Remove){
 $task=Get-ScheduledTask -TaskName $name -ErrorAction SilentlyContinue
 if($task -and @($task.Actions|Where-Object {$_.Arguments.Contains($scriptPath)}).Count){Unregister-ScheduledTask -TaskName $name -Confirm:$false}
 Remove-ItemProperty -Path $key -Name $name -ErrorAction SilentlyContinue;Write-Output 'Notification login startup removed; existing app data unchanged';return}
if(!(Test-Path -LiteralPath $scriptPath) -or !(Test-Path -LiteralPath $configPath)){throw 'Local watcher script/config missing'}
$exe=Join-Path $env:SystemRoot 'System32/WindowsPowerShell/v1.0/powershell.exe'
$command='"'+$exe+'" -NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File "'+$scriptPath+'"'
if(!(Test-Path $key)){New-Item -Path $key -Force|Out-Null}
New-ItemProperty -Path $key -Name $name -Value $command -PropertyType String -Force|Out-Null
$scheduledRecovery=$false;$scheduleError=''
try{
 $taskName='LokiCodexNotifications';$existing=Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
 if($existing -and !@($existing.Actions|Where-Object {$_.Arguments.Contains($scriptPath)}).Count){throw 'Task name belongs to another action'}
 $account=[Security.Principal.WindowsIdentity]::GetCurrent().Name
 $arguments='-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File "'+$scriptPath+'"'
 $action=New-ScheduledTaskAction -Execute $exe -Argument $arguments
 $login=New-ScheduledTaskTrigger -AtLogOn -User $account
 $recovery=New-ScheduledTaskTrigger -Once -At (Get-Date).AddMinutes(1) -RepetitionInterval (New-TimeSpan -Minutes 1)
 $settings=New-ScheduledTaskSettingsSet -MultipleInstances IgnoreNew -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 1) -ExecutionTimeLimit ([TimeSpan]::Zero) -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable
 $principal=New-ScheduledTaskPrincipal -UserId $account -LogonType Interactive -RunLevel Limited
 Register-ScheduledTask -TaskName $taskName -Action $action -Trigger @($login,$recovery) -Settings $settings -Principal $principal -Description 'Restore the local Loki Codex notification supervisor after login, worker failure or supervisor exit.' -Force|Out-Null
 $scheduledRecovery=$true
}catch{$scheduleError=$_.Exception.GetType().Name}

Start-Process -FilePath $exe -ArgumentList @('-NoProfile','-WindowStyle','Hidden','-ExecutionPolicy','Bypass','-File',('"'+$scriptPath+'"')) -WindowStyle Hidden|Out-Null
[pscustomobject]@{loginStartupRegistered=((Get-ItemPropertyValue -Path $key -Name $name) -eq $command);supervisorStartRequested=$true;scheduledRecoveryRegistered=$scheduledRecovery;scheduledRecoveryErrorType=$scheduleError;oldSessionEventsReplayed=$false;localAccountOnly=$true}|ConvertTo-Json
