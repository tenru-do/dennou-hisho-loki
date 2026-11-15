$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$review=Get-Content -LiteralPath (Join-Path $root 'artwork/recovery-artwork-v754.json') -Raw -Encoding UTF8|ConvertFrom-Json
foreach($pair in @(@($review.reference,$review.referenceSha256),@($review.comparison,$review.comparisonSha256))){if((Get-FileHash -LiteralPath (Join-Path $root $pair[0]) -Algorithm SHA256).Hash.ToLower() -ne $pair[1]){throw 'Recovery artwork reference/comparison changed'}}
if(!$review.manualWardrobeAndAnatomyReviewed -or $review.assets.Count -ne 16){throw 'Missing recovery artwork review'}
foreach($a in $review.assets){if($a.asset -notmatch '^recovery-20261114/(standing|seated)-0[1-8]\.png$' -or $a.unit -le 0 -or $a.center -le 0 -or $a.center -ge 1024){throw 'Invalid recovery asset calibration'};if((Get-FileHash -LiteralPath (Join-Path $root ('glass-app/assets/'+$a.asset)) -Algorithm SHA256).Hash.ToLower() -ne $a.sha256){throw 'Recovery asset differs from reviewed original'}}
Write-Output 'PASS 16 recovery assets, reviewed original hashes, calibration and wardrobe comparison'
