param([Parameter(Mandatory=$true)][string]$Serial,[Parameter(Mandatory=$true)][string]$Output,[string]$Poses='all')
$ErrorActionPreference='Stop'
$python=Join-Path $env:USERPROFILE '.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe'
if(!(Test-Path -LiteralPath $python)){$python=(Get-Command python -ErrorAction Stop).Source}
& $python "$PSScriptRoot/motion-review/export.py" --serial $Serial --output $Output --poses $Poses
if($LASTEXITCODE){throw 'Motion review export failed; inspect native-result.txt if created.'}
