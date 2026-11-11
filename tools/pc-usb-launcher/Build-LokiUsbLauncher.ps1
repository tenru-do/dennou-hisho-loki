param([switch]$VerifyLaunch,[switch]$KeepExistingExecutable,[string]$ScriptDirectory)
$ErrorActionPreference='Stop'
$ps51='C:/Windows/System32/WindowsPowerShell/v1.0/powershell.exe'
if(!$ScriptDirectory){
 $settings=Join-Path $PSScriptRoot 'launcher.local.json'
 if(Test-Path -LiteralPath $settings){$local=Get-Content -LiteralPath $settings -Raw -Encoding UTF8|ConvertFrom-Json;$ScriptDirectory=[string]$local.scriptDirectory}
 if(!$ScriptDirectory){$ScriptDirectory=$PSScriptRoot}
}
& $ps51 -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'Test-LokiUsbLauncher.ps1') -ScriptOnly -ScriptDirectory $ScriptDirectory
if($LASTEXITCODE){throw 'Launcher script/recovery preflight failed. Existing executable preserved.'}
if($KeepExistingExecutable){
 $existing=Join-Path $PSScriptRoot 'LokiUsbLauncher.exe'
 if(-not (Test-Path -LiteralPath $existing)){throw 'No existing trusted launcher to preserve.'}
 # Script-only changes can retain the already allowed executable; never disable application control.
 if($VerifyLaunch){& $ps51 -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'Test-LokiUsbLauncher.ps1') -Launch -ScriptDirectory $ScriptDirectory}
 else{& $ps51 -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'Test-LokiUsbLauncher.ps1') -ScriptDirectory $ScriptDirectory}
 if($LASTEXITCODE){throw 'Existing launcher and updated script verification failed'}
 Write-Output 'PASS updated scripts through the existing trusted executable; binary/icon/pinned path unchanged'
 exit 0
}
$temp=Join-Path $PSScriptRoot ('LokiUsbLauncher-build-'+[Guid]::NewGuid().ToString('N')+'.exe')
$target=Join-Path $PSScriptRoot 'LokiUsbLauncher.exe'
$backup='';$replaced=$false
try {
 $arguments=@('/nologo','/target:winexe','/reference:System.Windows.Forms.dll',('/out:'+$temp))
 $icon=Join-Path $PSScriptRoot 'loki-usb-current-face.ico'
 if(Test-Path -LiteralPath $icon){$arguments+=('/win32icon:'+$icon)}
 $arguments+=(Join-Path $PSScriptRoot 'LokiUsbLauncher.cs')
 & 'C:/Windows/Microsoft.NET/Framework64/v4.0.30319/csc.exe' @arguments
 if($LASTEXITCODE){throw 'Launcher compilation failed'}
 $p=Start-Process -FilePath $temp -ArgumentList '--self-test' -WindowStyle Hidden -PassThru
 if(-not $p.WaitForExit(15000)){try{$p.Kill()}catch{};throw 'Encoding preflight timeout'}
 if($p.ExitCode -ne 0){throw 'Encoding regression failed. Existing executable preserved.'}
 if(Test-Path -LiteralPath $target){$backup=Join-Path $PSScriptRoot ('LokiUsbLauncher-before-'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmss')+'-'+[Guid]::NewGuid().ToString('N').Substring(0,8)+'.exe');Copy-Item -LiteralPath $target -Destination $backup}
 Copy-Item -LiteralPath $temp -Destination $target -Force;$replaced=$true
 if($VerifyLaunch){& $ps51 -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'Test-LokiUsbLauncher.ps1') -Launch -ScriptDirectory $ScriptDirectory}
 else{& $ps51 -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'Test-LokiUsbLauncher.ps1') -ScriptDirectory $ScriptDirectory}
 if($LASTEXITCODE){throw 'Final launcher verification failed'}
 Write-Output 'PASS launcher build and verification; existing pinned path retained'
}catch{
 if($replaced -and $backup -and (Test-Path -LiteralPath $backup)){Copy-Item -LiteralPath $backup -Destination $target -Force;Write-Output 'Previous executable restored after failed verification.'}
 throw
}finally{if(Test-Path -LiteralPath $temp){Remove-Item -LiteralPath $temp -Force}}
