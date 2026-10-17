param([Parameter(Mandatory=$true)][string]$Serial,[string]$Adb=(Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'),[string]$OutputFile)
$ErrorActionPreference='Stop'
$model=(& $Adb -s $Serial shell getprop ro.product.model | Out-String).Trim()
if($LASTEXITCODE -ne 0 -or $model -ne 'RG-glasses'){throw 'Expected connected Rokid glasses; refusing other target'}
$package=& $Adb -s $Serial shell dumpsys package com.example.rokidkeyboardbridge
$version=[regex]::Match(($package -join "`n"),'versionCode=(\d+)').Groups[1].Value
$appPid=(& $Adb -s $Serial shell pidof com.example.rokidkeyboardbridge | Out-String).Trim()
$result=[ordered]@{version=$version;running=($appPid -match '^\d+$');conversation='NO_RECENT_EVIDENCE';events=@();scope='Current process only; no prompt, history, API key, location or device serial included'}
if($appPid -match '^\d+$'){
 $lines=& $Adb -s $Serial logcat -d "--pid=$appPid" -s RokidKeyboardAI
 $events=@()
 foreach($line in $lines){
  if($line -match 'Gemini safety stop model=([a-zA-Z0-9.\-_]+) reason=([A-Z0-9_]+)'){$events+=@{type='SAFETY_STOP';model=$Matches[1];reason=$Matches[2]};$result.conversation='SAFETY_STOP'}
  elseif($line -match 'search grounding sources=(\d+)'){$events+=@{type='RESPONSE_RECEIVED';sources=[int]$Matches[1]};$result.conversation='RESPONSE_RECEIVED'}
 }
 $result.events=@($events | Select-Object -Last 5)
}
$json=$result | ConvertTo-Json -Depth 6
if($OutputFile){[IO.File]::WriteAllText([IO.Path]::GetFullPath($OutputFile),$json,[Text.UTF8Encoding]::new($false))}
Write-Output $json
