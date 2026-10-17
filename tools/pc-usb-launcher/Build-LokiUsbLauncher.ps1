param([switch]$VerifyLaunch)
$ErrorActionPreference='Stop'
$ps51='C:/Windows/System32/WindowsPowerShell/v1.0/powershell.exe'
& $ps51 -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'Test-LokiUsbLauncher.ps1') -ScriptOnly
if($LASTEXITCODE){throw 'Launcher script preflight failed. Build stopped.'}
$temp=Join-Path $PSScriptRoot ('LokiUsbLauncher-build-'+[Guid]::NewGuid().ToString('N')+'.exe')
& 'C:/Windows/Microsoft.NET/Framework64/v4.0.30319/csc.exe' /nologo /target:winexe /reference:System.Windows.Forms.dll ('/out:'+$temp) (Join-Path $PSScriptRoot 'LokiUsbLauncher.cs')
if($LASTEXITCODE){throw 'Launcher compilation failed'}
$p=Start-Process -FilePath $temp -ArgumentList '--self-test' -WindowStyle Hidden -PassThru -Wait
if($p.ExitCode -ne 0){throw 'Encoding regression failed. Existing launcher preserved.'}
Copy-Item -LiteralPath $temp -Destination (Join-Path $PSScriptRoot 'LokiUsbLauncher.exe') -Force
Remove-Item -LiteralPath $temp
if($VerifyLaunch){& $ps51 -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'Test-LokiUsbLauncher.ps1') -Launch}
else{& $ps51 -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'Test-LokiUsbLauncher.ps1')}
if($LASTEXITCODE){throw 'Final launcher verification failed'}