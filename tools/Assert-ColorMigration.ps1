param([string]$ManifestFile,[string]$AssetsRoot)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
if(!$ManifestFile){$ManifestFile=Join-Path $root 'artwork/color-migration-reviewed.json'}
if(!$AssetsRoot){$AssetsRoot=Join-Path $root 'glass-app/assets'}
$review=Get-Content -LiteralPath $ManifestFile -Raw -Encoding UTF8 | ConvertFrom-Json
$names=@{}
foreach($entry in $review.assets){
 if(!$entry.name -or $entry.name -notmatch '^[A-Za-z0-9_./-]+\.png$' -or $entry.name -match '(^|/)\.\.(/|$)' -or $names.ContainsKey($entry.name)){throw 'Invalid or duplicate color migration entry'}
 $names[$entry.name]=$true
 $base=[IO.Path]::GetFullPath($AssetsRoot).TrimEnd('\','/')
 $path=[IO.Path]::GetFullPath((Join-Path $base $entry.name))
 if(!$path.StartsWith($base+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw 'Color artwork path outside asset root'}
 if(!(Test-Path -LiteralPath $path) -or (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLower() -ne $entry.sha256){throw ('Reviewed color artwork reverted or changed: '+$entry.name)}
}
if($review.runtimeMonochromeMigrationComplete){
 Add-Type -AssemblyName System.Drawing
 $runtimePaths=[Collections.Generic.HashSet[string]]::new()
 foreach($source in Get-ChildItem -LiteralPath (Join-Path $root 'glass-app/src') -Recurse -Filter '*.java'){
  foreach($match in [regex]::Matches([IO.File]::ReadAllText($source.FullName),'"(loki_[A-Za-z0-9_/-]+\.png)"')){
   $candidate=$match.Groups[1].Value
   if(Test-Path -LiteralPath (Join-Path $AssetsRoot $candidate)){$null=$runtimePaths.Add($candidate)}
  }
 }
 foreach($override in Get-ChildItem -LiteralPath (Join-Path $AssetsRoot 'cool-smile-v561') -Filter '*.png'){$null=$runtimePaths.Add('cool-smile-v561/'+$override.Name)}
 foreach($name in $runtimePaths){
  if(!$names.ContainsKey($name)){throw "Missing migrated color asset: $name"}
  $image=[Drawing.Bitmap]::new((Join-Path $AssetsRoot $name))
  try{
   $color=0;$visible=0
   for($y=0;$y -lt 40;$y++){for($x=0;$x -lt 32;$x++){
    $pixel=$image.GetPixel([int]($x*($image.Width-1)/31),[int]($y*($image.Height-1)/39))
    if($pixel.A -gt 100 -and $pixel.R -gt 45){$visible++;if($pixel.R-$pixel.G -ge 5 -and $pixel.G-$pixel.B -ge 2){$color++}}
   }}
   if($visible -eq 0 -or $color -lt 3){throw "Monochrome artwork reintroduced: $name"}
  }finally{$image.Dispose()}
 }
 Write-Output ('Runtime color regression guard PASS: '+$runtimePaths.Count+' current source/active override assets')
}
Write-Output ('Reviewed color migration guard PASS: '+$names.Count+' assets; historical archives are distinct from runtime coverage.')
