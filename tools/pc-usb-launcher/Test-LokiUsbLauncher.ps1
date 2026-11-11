param([switch]$ScriptOnly,[switch]$Launch,[string]$ScriptDirectory)
$ErrorActionPreference='Stop'
if($PSVersionTable.PSVersion.Major -ne 5){throw 'Run this check with Windows PowerShell 5.1, the actual launcher runtime.'}
if(!$ScriptDirectory){
 $settings=Join-Path $PSScriptRoot 'launcher.local.json'
 if(Test-Path -LiteralPath $settings){$local=Get-Content -LiteralPath $settings -Raw -Encoding UTF8|ConvertFrom-Json;$ScriptDirectory=[string]$local.scriptDirectory}
 if(!$ScriptDirectory){$ScriptDirectory=$PSScriptRoot}
}
$sources=@((Join-Path $ScriptDirectory 'Start-Loki-USB.ps1'),(Join-Path $ScriptDirectory 'Usb-AdbRecovery.ps1'),$PSCommandPath,(Join-Path $PSScriptRoot 'Build-LokiUsbLauncher.ps1'),(Join-Path $ScriptDirectory 'Test-UsbAdbRecovery.ps1'),(Join-Path $PSScriptRoot 'Test-LokiLauncherRuntime.ps1'))
foreach($script in $sources){
 $bytes=[IO.File]::ReadAllBytes($script)
 if($bytes.Length -lt 3 -or $bytes[0] -ne 0xef -or $bytes[1] -ne 0xbb -or $bytes[2] -ne 0xbf){throw ($script+' must be UTF-8 with BOM.')}
 $null=[Text.UTF8Encoding]::new($false,$true).GetString($bytes,3,$bytes.Length-3)
 $tokens=$null;$parseErrors=$null
 $null=[Management.Automation.Language.Parser]::ParseFile($script,[ref]$tokens,[ref]$parseErrors)
 if($parseErrors.Count){throw ('Windows PowerShell 5.1 parse failed: '+$script+': '+($parseErrors.Message -join '; '))}
}
function Start-LokiLauncherProcess {
 param([string]$Executable,[string]$Arguments='')
 $start=New-Object Diagnostics.ProcessStartInfo
 $start.FileName=$Executable;$start.Arguments=$Arguments;$start.UseShellExecute=$false;$start.CreateNoWindow=$true;$start.WindowStyle='Hidden'
 $process=New-Object Diagnostics.Process;$process.StartInfo=$start;$null=$process.Start();return $process
}
Write-Output 'PASS all startup/recovery/build/test scripts: UTF-8 BOM, strict UTF-8, Windows PowerShell 5.1 parser'
# Run in another process so test clock/process mocks cannot affect the real launcher.
& 'C:/Windows/System32/WindowsPowerShell/v1.0/powershell.exe' -NoProfile -ExecutionPolicy Bypass -File (Join-Path $ScriptDirectory 'Test-UsbAdbRecovery.ps1')
if($LASTEXITCODE){throw 'ADB recovery regression failed'}
if(-not $ScriptOnly){
 $exe=Join-Path $PSScriptRoot 'LokiUsbLauncher.exe'
 $p=Start-LokiLauncherProcess -Executable $exe -Arguments '--self-test'
 if(-not $p.WaitForExit(15000)){try{$p.Kill()}catch{};throw 'Launcher self-test timeout'}
 if($p.ExitCode -ne 0){throw 'Launcher encoding regression test failed'}
 Write-Output 'PASS production launcher accepts UTF-8 BOM and rejects BOM-less/malformed UTF-8'
 & 'C:/Windows/System32/WindowsPowerShell/v1.0/powershell.exe' -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'Test-LokiLauncherRuntime.ps1') -ScriptDirectory $ScriptDirectory
 if($LASTEXITCODE){throw 'Launcher concurrency regression failed'}
 if($Launch){
  $p=Start-LokiLauncherProcess -Executable $exe
  # Wait only for the launcher, never the long-lived browser process tree.
  if(-not $p.WaitForExit(100000)){throw 'Launcher did not finish within 100 seconds; check its error dialog'}
  if($p.ExitCode -ne 0){throw 'Actual launcher failed'}
  $config=Get-Content -LiteralPath (Join-Path $ScriptDirectory 'launcher.local.json') -Raw -Encoding UTF8|ConvertFrom-Json
  $node=if($config.node){[string]$config.node}else{(Get-Command node -ErrorAction Stop).Source}
  & $node (Join-Path $ScriptDirectory 'verify-usb.cjs') --check-web
  if($LASTEXITCODE){throw 'Certificate-verified phone Web response checks failed'}
  Write-Output 'PASS actual pinned executable launch and certificate-verified Web HTML/JavaScript responses'
 }
}
