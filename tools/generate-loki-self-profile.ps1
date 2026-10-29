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
$files=@();foreach($name in $names){$p=Join-Path $assets $name;$files+=[ordered]@{asset=$name;sha256=(Get-FileHash -LiteralPath $p -Algorithm SHA256).Hash.ToLower()}}
$frames=@();for($i=0;$i -lt $regions.Count;$i++){$frames+=[ordered]@{id=$i;asset=$names[$sheetIds[$i]];region=$regions[$i]}}
foreach($override in Get-ChildItem -LiteralPath (Join-Path $assets 'cool-smile-v561') -Filter 'frame-*.png'){
 $id=[int]([regex]::Match($override.BaseName,'frame-(\d+)').Groups[1].Value)
 $region=$regions[$id];$asset='cool-smile-v561/'+$override.Name
 $files+=[ordered]@{asset=$asset;sha256=(Get-FileHash -LiteralPath $override.FullName -Algorithm SHA256).Hash.ToLower()}
 $frames[$id]=[ordered]@{id=$id;asset=$asset;region=@(0,0,($region[2]-$region[0]),($region[3]-$region[1]))}
}
$kissFace='loki_kiss_faces_v665.png'
$files+=[ordered]@{asset=$kissFace;sha256=(Get-FileHash -LiteralPath (Join-Path $assets $kissFace) -Algorithm SHA256).Hash.ToLower()}
$face='loki_face_cool_v559.png'
$extraFace='loki_portrait_life_v665.png'
$files+=[ordered]@{asset=$extraFace;sha256=(Get-FileHash -LiteralPath (Join-Path $assets $extraFace) -Algorithm SHA256).Hash.ToLower()}
foreach($bodyAsset in @('loki_body_greeting_v654.png','loki_body_greeting_standing_v667.png','loki_body_greeting_seated_v667.png','loki_relaxed_poses_v670.png','loki_rest_master_v675.png','loki_rest_motion_v675.png','loki_rest_faces_v675.png','loki_rest_wave_v675.png','loki_rest_knee_v676.png','loki_rest_conversation_faces_v677.png','loki_comfort_hug_v678.png','loki_comfort_read_v678.png','loki_comfort_side_v678.png','loki_comfort_stretch_v678.png','loki_poised_gestures_v670.png','loki_relaxed_expressions_v670.png','loki_relaxed_emotions_v670.png','loki_body_leg_change_v669.png','loki_body_actions_v667.png','loki_body_seated_kiss_v667.png','loki_body_overhead_v667.png','loki_body_ear_v672.png','loki_body_affection_v669.png','loki_body_invite_v667.png','loki_body_affection_seated_v669.png','loki_contact_states_v669.png','loki_portrait_sequence_v665.png','loki_notebook_sequence_v667.png')){$files+=[ordered]@{asset=$bodyAsset;sha256=(Get-FileHash -LiteralPath (Join-Path $assets $bodyAsset) -Algorithm SHA256).Hash.ToLower()}}
$profile=[ordered]@{schema=1;comfortPoseCount=4;comfortExpressionFamilies=4;correctedRestBodyFrames=18;correctedRestFacePatches=14;correctedRestExpressionTypes=10;correctedRestAnimationTypes=7;extraBodyMotionFrames=106;extraBodyMotionClips=44;identity='dennou-hisho-loki';fullbodyCount=$regions.Count;faceCount=69;kissPortraitFrames=8;legacyFaceCount=37;portraitGestureCount=24;portraitSequenceAsset='loki_portrait_sequence_v665.png';portraitSequenceGrid=@(4,4);notebookAsset='loki_notebook_sequence_v667.png';portraitGestureAsset=$extraFace;faceAsset=$face;faceSha256=(Get-FileHash -LiteralPath (Join-Path $assets $face) -Algorithm SHA256).Hash.ToLower();files=$files;frames=$frames;references=@([ordered]@{asset='loki_fullbody_000_005.png';region=@(405,758,611,1477);label='登録済み標準全身・通常立ち姿（ID 4）';maxSide=640},[ordered]@{asset=$face;region=@(0,0,192,240);label='採用済み顔モードの通常顔（ID 0）';maxSide=384})}
foreach($app in @('glass','phone')){$dest=Join-Path $root "$app-app/assets";New-Item -ItemType Directory -Force $dest | Out-Null;[IO.File]::WriteAllText((Join-Path $dest 'loki-self-profile.json'),($profile | ConvertTo-Json -Depth 8 -Compress),[Text.UTF8Encoding]::new($false))}
foreach($name in @('loki_fullbody_000_005.png',$face,$extraFace,$kissFace,'loki_body_greeting_v654.png','loki_body_greeting_standing_v667.png','loki_body_greeting_seated_v667.png','loki_relaxed_poses_v670.png','loki_rest_master_v675.png','loki_rest_motion_v675.png','loki_rest_faces_v675.png','loki_rest_wave_v675.png','loki_rest_knee_v676.png','loki_rest_conversation_faces_v677.png','loki_comfort_hug_v678.png','loki_comfort_read_v678.png','loki_comfort_side_v678.png','loki_comfort_stretch_v678.png','loki_poised_gestures_v670.png','loki_relaxed_expressions_v670.png','loki_relaxed_emotions_v670.png','loki_body_leg_change_v669.png','loki_body_actions_v667.png','loki_body_seated_kiss_v667.png','loki_body_overhead_v667.png','loki_body_ear_v672.png','loki_body_affection_v669.png','loki_body_invite_v667.png','loki_body_affection_seated_v669.png','loki_contact_states_v669.png','loki_portrait_sequence_v665.png','loki_notebook_sequence_v667.png')){Copy-Item -LiteralPath (Join-Path $assets $name) -Destination (Join-Path $root "phone-app/assets/$name") -Force}
Write-Output "Self profile: $($regions.Count) full-body frames, 69 faces (37 legacy + 24 portrait gestures + 8 kissing expressions); registered source hashes recorded"
