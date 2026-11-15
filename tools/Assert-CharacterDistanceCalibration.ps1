param([string]$Root=(Split-Path $PSScriptRoot -Parent))
$ErrorActionPreference='Stop'
$record=Get-Content -LiteralPath (Join-Path $Root 'artwork/reference/character-distance-v750.json') -Raw | ConvertFrom-Json
$table=Join-Path $Root $record.calibrationFile
if((Get-FileHash -LiteralPath $table -Algorithm SHA256).Hash.ToLowerInvariant() -ne $record.calibrationSha256){throw 'Character distance calibration changed: review historical non-enlargement fixtures and native comparisons before updating the record.'}
$count=0
foreach($entry in $record.assets.PSObject.Properties){
 $file=Join-Path (Join-Path $Root 'glass-app/assets') $entry.Name
 if(!(Test-Path -LiteralPath $file) -or (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash.ToLowerInvariant() -ne $entry.Value){throw "Character source changed without distance review: $($entry.Name)"}
 $count++
}
Write-Host "PASS character distance source guard: $count reviewed assets; stable reference, no automatic maximum promotion."
