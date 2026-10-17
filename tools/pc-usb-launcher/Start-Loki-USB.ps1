$ErrorActionPreference='Stop'
$config=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'launcher.local.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$serial=[string]$config.phoneSerial
if(!$serial){throw 'Set phoneSerial in launcher.local.json.'}
$adb=if($config.adb){[string]$config.adb}else{Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'}
$node=if($config.node){[string]$config.node}else{(Get-Command node -ErrorAction Stop).Source}
$state=& $adb -s $serial get-state 2>$null
if($LASTEXITCODE -ne 0 -or $state -ne 'device'){throw 'Connect the configured smartphone and authorize USB debugging.'}
& $adb -s $serial forward tcp:18766 tcp:8766
if($LASTEXITCODE){throw 'USB forwarding failed.'}
& $adb -s $serial shell am start -f 0x14000000 -n com.example.rokidgeminisecretary/.MainActivity | Out-Null
if($LASTEXITCODE){throw 'Could not open Loki on the phone.'}
$pin=& $node (Join-Path $PSScriptRoot 'verify-usb.cjs')
if($LASTEXITCODE -ne 0 -or $pin -notmatch '^[A-Za-z0-9+/]{43}=$'){throw 'Smartphone certificate validation failed. Open Loki and issue a Web connection invitation on the phone.'}
$edge=Join-Path ${env:ProgramFiles(x86)} 'Microsoft/Edge/Application/msedge.exe'
$profile=Join-Path $PSScriptRoot 'private-browser-profile'
Start-Process -FilePath $edge -ArgumentList @('--app=https://127.0.0.1:18766/web/',('"--user-data-dir='+$profile+'"'),('--ignore-certificate-errors-spki-list='+$pin))
Write-Host 'Approve the Web connection on the phone if requested. Keep the smartphone connected by USB.'
