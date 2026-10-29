param([string]$ManifestFile)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
if(!$ManifestFile){$ManifestFile=Join-Path $root 'artwork/character-artwork-review.json'}
$review=Get-Content -LiteralPath $ManifestFile -Raw -Encoding UTF8 | ConvertFrom-Json
$reference=Join-Path $root 'artwork/reference/loki-user-approved-base-20261027.png'
if((Get-FileHash -LiteralPath $reference -Algorithm SHA256).Hash.ToLower() -ne $review.referenceSha256){throw 'Character reference changed: review identity and proportions before building'}
$renderer=[IO.File]::ReadAllText((Join-Path $root 'glass-app/src/com/example/rokidkeyboardbridge/FullBodyGestureRenderer.java'))
$renderer+=[IO.File]::ReadAllText((Join-Path $root 'glass-app/src/com/example/rokidkeyboardbridge/RestArtworkRenderer.java'))
$renderer+=[IO.File]::ReadAllText((Join-Path $root 'glass-app/src/com/example/rokidkeyboardbridge/ComfortPoseGeometry.java'))
$renderer+=[IO.File]::ReadAllText((Join-Path $root 'glass-app/src/com/example/rokidkeyboardbridge/StandingGestureAtlas.java'))
$renderer+=[IO.File]::ReadAllText((Join-Path $root 'glass-app/src/com/example/rokidkeyboardbridge/PortraitExpressionRenderer.java'))
$renderer+=[IO.File]::ReadAllText((Join-Path $root 'glass-app/src/com/example/rokidkeyboardbridge/PostureBridgeRenderer.java'))
$renderer+=[IO.File]::ReadAllText((Join-Path $root 'glass-app/src/com/example/rokidkeyboardbridge/SeatedGestureAtlas.java'))
$names=@([regex]::Matches($renderer,'"(loki_[a-zA-Z0-9_]+\.png)"') | ForEach-Object {$_.Groups[1].Value} | Sort-Object -Unique)
foreach($name in $names){
 $entry=@($review.assets | Where-Object {$_.name -eq $name})
 if($entry.Count -ne 1){throw "Unreviewed character artwork: $name. Generate a comparison with tools/Review-CharacterArtwork.py and review face, shoulders, legs and screen-size preview before registering its hash."}
 if((Get-FileHash -LiteralPath (Join-Path $root "glass-app/assets/$name") -Algorithm SHA256).Hash.ToLower() -ne $entry[0].sha256){throw "Character artwork changed without proportion/identity review: $name"}
 if($name -match '_v(\d+)\.png$' -and [int]$Matches[1] -ge 686){
  if(!$entry[0].evidence -or !$entry[0].evidenceSha256){throw "Missing image review evidence: $name"}
  $proofPath=[IO.Path]::GetFullPath((Join-Path $root $entry[0].evidence))
  if(!$proofPath.StartsWith((Join-Path $root 'artwork/reviews/'),[StringComparison]::OrdinalIgnoreCase)){throw 'Image review evidence must stay inside artwork/reviews'}
  if((Get-FileHash -LiteralPath $proofPath -Algorithm SHA256).Hash.ToLower() -ne $entry[0].evidenceSha256){throw "Image review evidence changed: $name"}
  $proof=Get-Content -LiteralPath $proofPath -Raw -Encoding UTF8 | ConvertFrom-Json
  if($proof.assetSha256 -ne $entry[0].sha256 -or $proof.referenceSha256 -ne $review.referenceSha256 -or !$proof.manualIdentityReviewed -or !$proof.manualAnatomyReviewed -or $proof.reviewedFrames -ne $proof.columns*$proof.rows){throw "Incomplete or stale identity/anatomy review: $name"}
  foreach($item in @($proof.contactSheet,$proof.landmarks)){
   $itemPath=[IO.Path]::GetFullPath((Join-Path $root $item.path))
   if(!$itemPath.StartsWith((Join-Path $root 'artwork/reviews/'),[StringComparison]::OrdinalIgnoreCase) -or (Get-FileHash -LiteralPath $itemPath -Algorithm SHA256).Hash.ToLower() -ne $item.sha256){throw "Missing or changed comparison/landmarks: $name"}
  }
  $landmarks=Get-Content -LiteralPath (Join-Path $root $proof.landmarks.path) -Raw -Encoding UTF8 | ConvertFrom-Json
  if($landmarks.frames.Count -ne $proof.reviewedFrames){throw "Missing per-frame review: $name"}
  foreach($frame in $landmarks.frames){
   $c=$frame.cropLTRB;$h=$frame.headBounds
   if($c.Count -ne 4 -or $h.Count -ne 4 -or $c[0] -lt 0 -or $c[1] -lt 0 -or $c[2] -gt $landmarks.imageWidth -or $c[3] -gt $landmarks.imageHeight -or $c[2] -le $c[0] -or $c[3] -le $c[1] -or $h[0] -lt 0 -or $h[1] -lt 0 -or $h[2] -le 0 -or $h[3] -le 0 -or $h[0]+$h[2] -gt $c[2]-$c[0] -or $h[1]+$h[3] -gt $c[3]-$c[1]){throw "Invalid crop/head review bounds: $name"}
  }
 }
}
Write-Output "Character artwork regression gate PASS: $($names.Count) registered atlases; reference unchanged. Existing snapshots do not certify anatomy; new images require visual and landmark review."
