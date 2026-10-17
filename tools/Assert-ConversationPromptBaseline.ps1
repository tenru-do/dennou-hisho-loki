param([Parameter(Mandatory=$true)][ValidateSet('glass','phone')][string]$App,[string]$BaselineFile)
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Split-Path $PSScriptRoot -Parent))
if(!$BaselineFile){$BaselineFile=Join-Path $root 'docs/conversation-prompt-baseline.json'}
$baseline=Get-Content -LiteralPath $BaselineFile -Raw -Encoding UTF8 | ConvertFrom-Json
if($baseline.schema -ne 1 -or !$baseline.providerVerified -or !$baseline.response){throw 'Missing provider-verified conversation baseline'}
$count=0
foreach($entry in $baseline.entries){
 if($entry.app -ne $App){continue}
 $path=[IO.Path]::GetFullPath((Join-Path $root $entry.path))
 if(!$path.StartsWith($root.TrimEnd('\')+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'Prompt guard path outside source tree'}
 $text=[IO.File]::ReadAllText($path).Replace("`r`n","`n").Replace("`r","`n")
 if($entry.start){$start=$text.IndexOf([string]$entry.start,[StringComparison]::Ordinal);if($start -lt 0){throw "Prompt guard start missing: $($entry.path)"};$end=$text.IndexOf([string]$entry.end,$start,[StringComparison]::Ordinal);if($end -lt $start){throw "Prompt guard end missing: $($entry.path)"};$text=$text.Substring($start,$end-$start)}
 $sha=[Security.Cryptography.SHA256]::Create();try{$hash=([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($text)))).Replace('-','').ToLowerInvariant()}finally{$sha.Dispose()}
 if($hash -ne $entry.sha256){throw "Conversation prompt changed: $($entry.path). Build stopped. Review the complete payload and obtain a new authorized provider/device check before updating docs/conversation-prompt-baseline.json; do not merely refresh hashes."}
 $count++
}
if(!$count){throw 'No protected prompt sections for requested app'}
Write-Output "Conversation prompt baseline PASS: $App, $count protected sections (provider/device baseline $($baseline.verifiedOn.glass)/$($baseline.verifiedOn.phone)). This build's live validation is a separate step."
