param()
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$assets=Join-Path $root 'glass-app/assets'
$java=[IO.File]::ReadAllText((Join-Path $root 'glass-app/src/com/example/rokidkeyboardbridge/FullBodyRealisticAssets.java'))
$namesText=[regex]::Match($java,'(?s)String\[\] NAMES=\{(.*?)\};').Groups[1].Value
$names=@([regex]::Matches($namesText,'"([^"]+\.png)"') | ForEach-Object {$_.Groups[1].Value})
$sheetText=[regex]::Match($java,'(?s)int\[\] SHEET=\{(.*?)\};').Groups[1].Value
$sheetIds=@([regex]::Matches($sheetText,'\d+') | ForEach-Object {[int]$_.Value})
$regionText=[regex]::Match($java,'(?s)int\[\]\[\] REGIONS=\{(.*?)\n\};').Groups[1].Value
$regions=@([regex]::Matches($regionText,'\{(\d+),(\d+),(\d+),(\d+)\}') | ForEach-Object {,@([int]$_.Groups[1].Value,[int]$_.Groups[2].Value,[int]$_.Groups[3].Value,[int]$_.Groups[4].Value)})
if($names.Count -lt 1 -or $sheetIds.Count -ne $regions.Count -or $regions.Count -lt 400){throw 'Image registration schema mismatch'}
$files=@();foreach($name in $names){$p=Join-Path $assets $name;$files+=@{asset=$name;sha256=(Get-FileHash -LiteralPath $p -Algorithm SHA256).Hash.ToLower()}}
$frames=@();for($i=0;$i -lt $regions.Count;$i++){$frames+=@{id=$i;asset=$names[$sheetIds[$i]];region=$regions[$i]}}
foreach($override in Get-ChildItem -LiteralPath (Join-Path $assets 'cool-smile-v561') -Filter 'frame-*.png'){
 $id=[int]([regex]::Match($override.BaseName,'frame-(\d+)').Groups[1].Value)
 $region=$regions[$id];$asset='cool-smile-v561/'+$override.Name
 $files+=@{asset=$asset;sha256=(Get-FileHash -LiteralPath $override.FullName -Algorithm SHA256).Hash.ToLower()}
 $frames[$id]=@{id=$id;asset=$asset;region=@(0,0,($region[2]-$region[0]),($region[3]-$region[1]))}
}
$face='loki_face_cool_v559.png'
$profile=@{schema=1;identity='dennou-hisho-loki';fullbodyCount=$regions.Count;faceCount=37;faceAsset=$face;faceSha256=(Get-FileHash -LiteralPath (Join-Path $assets $face) -Algorithm SHA256).Hash.ToLower();files=$files;frames=$frames;references=@(@{asset='loki_fullbody_000_005.png';region=@(405,758,611,1477);label='登録済み標準全身・通常立ち姿（ID 4）';maxSide=640},@{asset=$face;region=@(0,0,192,240);label='採用済み顔モードの通常顔（ID 0）';maxSide=384})}
foreach($app in @('glass','phone')){$dest=Join-Path $root "$app-app/assets";New-Item -ItemType Directory -Force $dest | Out-Null;[IO.File]::WriteAllText((Join-Path $dest 'loki-self-profile.json'),($profile | ConvertTo-Json -Depth 8 -Compress),[Text.UTF8Encoding]::new($false))}
foreach($name in @('loki_fullbody_000_005.png',$face)){Copy-Item -LiteralPath (Join-Path $assets $name) -Destination (Join-Path $root "phone-app/assets/$name") -Force}
Write-Output "Self profile: $($regions.Count) full-body frames, 37 faces; registered source hashes recorded"
