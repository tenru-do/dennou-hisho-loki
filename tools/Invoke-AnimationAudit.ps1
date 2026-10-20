param(
 [Parameter(Mandatory=$true,ParameterSetName='Existing')][string]$Review,
 [Parameter(Mandatory=$true,ParameterSetName='Device')][string]$Serial,
 [Parameter(Mandatory=$true)][string]$Output,
 [Parameter(ParameterSetName='Device')][string]$Poses='all',
 [string]$Python=(Join-Path $env:USERPROFILE '.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe'),
 [string]$Thresholds=''
)
$ErrorActionPreference='Stop'
if(!(Test-Path -LiteralPath $Python)){throw 'Specify a Python executable with Pillow and NumPy using -Python.'}
$tool=Join-Path $PSScriptRoot 'motion-review'
& $Python -c 'import PIL,numpy'
if($LASTEXITCODE){throw 'Pillow and NumPy must already be installed. This command does not download dependencies.'}
if($PSCmdlet.ParameterSetName -eq 'Device'){
 if($Thresholds){
  & $Python -X utf8 "$tool/export.py" --serial $Serial --output $Output --poses $Poses
  if($LASTEXITCODE){throw 'Native export failed; no audit success was recorded.'}
  & $Python -X utf8 "$tool/audit.py" --review $Output --output (Join-Path $Output 'auto-audit') --thresholds $Thresholds
 }else{
  & $Python -X utf8 "$tool/export.py" --serial $Serial --output $Output --poses $Poses --audit
 }
}else{
 $arguments=@('-X','utf8',"$tool/audit.py",'--review',$Review,'--output',$Output)
 if($Thresholds){$arguments+=@('--thresholds',$Thresholds)}
 & $Python @arguments
}
if($LASTEXITCODE -eq 2){throw 'Audit found missing/corrupt export data. The report distinguishes it from visual review candidates.'}
if($LASTEXITCODE){throw 'Animation audit failed. Existing images and installed applications have not been replaced.'}
