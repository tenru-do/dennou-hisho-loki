param([switch]$ScriptOnly,[switch]$Launch)
$ErrorActionPreference='Stop'
if($PSVersionTable.PSVersion.Major -ne 5){throw 'Run this check with Windows PowerShell 5.1, the actual launcher runtime.'}
$script=Join-Path $PSScriptRoot 'Start-Loki-USB.ps1'
$bytes=[IO.File]::ReadAllBytes($script)
if($bytes.Length -lt 3 -or $bytes[0] -ne 0xef -or $bytes[1] -ne 0xbb -or $bytes[2] -ne 0xbf){throw 'Start-Loki-USB.ps1 must be UTF-8 with BOM. Do not save as UTF-8 without BOM.'}
$null=[Text.UTF8Encoding]::new($false,$true).GetString($bytes,3,$bytes.Length-3)
$tokens=$null;$parseErrors=$null
$null=[Management.Automation.Language.Parser]::ParseFile($script,[ref]$tokens,[ref]$parseErrors)
if($parseErrors.Count){throw ('Windows PowerShell 5.1 parse failed: '+($parseErrors.Message -join '; '))}
Write-Output 'PASS UTF-8 BOM, strict UTF-8 decoding, Windows PowerShell 5.1 parser'
if(-not $ScriptOnly){
 $exe=Join-Path $PSScriptRoot 'LokiUsbLauncher.exe'
 $p=Start-Process -FilePath $exe -ArgumentList '--self-test' -WindowStyle Hidden -PassThru -Wait
 if($p.ExitCode -ne 0){throw 'Launcher encoding regression test failed'}
 Write-Output 'PASS production launcher accepts UTF-8 BOM and rejects BOM-less or malformed UTF-8 fixtures'
 if($Launch){
  $p=Start-Process -FilePath $exe -WindowStyle Hidden -PassThru
  if(-not $p.WaitForExit(45000)){throw 'Launcher did not finish within 45 seconds'}
  if($p.ExitCode -ne 0){throw 'Actual launcher failed'}
  $node=(Get-Command node -ErrorAction Stop).Source
  & $node (Join-Path $PSScriptRoot 'verify-usb.cjs') | Out-Null
  if($LASTEXITCODE){throw 'Phone certificate verification failed'}
  Write-Output 'PASS actual pinned executable launch and phone Web HTTP responses'
 }
}