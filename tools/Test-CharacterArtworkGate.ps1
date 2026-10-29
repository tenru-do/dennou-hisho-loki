param([Parameter(Mandatory=$true)][string]$Output)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$source=Join-Path $root 'artwork/character-artwork-review.json'
New-Item -ItemType Directory -Force $Output | Out-Null
& "$PSScriptRoot/Assert-CharacterArtwork.ps1"
$cases=@('reference','asset','missing-evidence','evidence-hash','outside-review')
foreach($case in $cases){
 $m=Get-Content -LiteralPath $source -Raw -Encoding UTF8 | ConvertFrom-Json
 $entry=@($m.assets | Where-Object {$_.name -eq 'loki_identity_portraits_v686.png'})[0]
 switch($case){
  'reference' {$m.referenceSha256='0'*64}
  'asset' {$entry.sha256='0'*64}
  'missing-evidence' {$entry.evidence=''}
  'evidence-hash' {$entry.evidenceSha256='0'*64}
  'outside-review' {$entry.evidence='README.md'}
 }
 $path=Join-Path $Output "$case.json"
 $m | ConvertTo-Json -Depth 30 | Set-Content -LiteralPath $path -Encoding UTF8
 $rejected=$false
 try {& "$PSScriptRoot/Assert-CharacterArtwork.ps1" -ManifestFile $path | Out-Null} catch {$rejected=$true}
 if(!$rejected){throw "Artwork gate accepted invalid $case evidence"}
}
Write-Output 'PASS: valid registry plus 5 tampered/incomplete artwork rejection checks'
