param([Parameter(Mandatory=$true)][string]$Serial,[string]$OutputDirectory)
$ErrorActionPreference='Stop'
$repo=Split-Path $PSScriptRoot -Parent
$java='C:/Program Files/Android/Android Studio/jbr/bin'
$sdk='C:/Users/user/AppData/Local/Android/Sdk/build-tools/37.0.0'
$adb='C:/Users/user/Documents/Rokid_Zoom_Camera/android-sdk/platform-tools/adb.exe'
$output=if($OutputDirectory){$OutputDirectory}else{"$repo/.build-integration/scene-verification"}
New-Item -ItemType Directory -Force $output | Out-Null
Remove-Item "$output/device-tests-verified.txt" -ErrorAction SilentlyContinue
$build="$repo/.build-integration/glass"
$test="$repo/.build-integration/fullbody-rules-v433-test"
New-Item -ItemType Directory -Force $test | Out-Null
$files=Get-ChildItem "$repo/tools" -Filter 'FullBody*Test.java' | Select-Object -ExpandProperty FullName
& "$java/javac.exe" -encoding UTF8 -cp "$build/android.jar;$build/classes" -d $test @files "$build/scene-tests/FullBodyConfiguredScenesTest.java"
if($LASTEXITCODE -ne 0){throw 'Test compilation failed'}
Get-ChildItem "$build/classes/com/example/rokidkeyboardbridge" -Filter 'FullBody*.class' | ForEach-Object {Copy-Item -LiteralPath $_.FullName -Destination "$test/com/example/rokidkeyboardbridge" -Force}
$env:TEMP='C:/Users/user/Documents/Codex/2026-10-08/rokid-2/work-build-temp';$env:TMP=$env:TEMP
& "$java/jar.exe" "-J-Djava.io.tmpdir=$($env:TEMP)" cf "$test/test.jar" -C $test com
if($LASTEXITCODE -ne 0){throw 'Jar failed'}
$env:JAVA_HOME='C:/Program Files/Android/Android Studio/jbr'
$env:TEMP='C:/Users/user/Documents/Codex/2026-10-08/rokid-2/work-build-temp';$env:TMP=$env:TEMP
& "$sdk/d8.bat" --lib "$build/android.jar" --min-api 28 --output $test "$test/test.jar"
if($LASTEXITCODE -ne 0){throw 'D8 failed'}
Copy-Item "$build/loki-glass-integration.apk" "$output/loki-glass-v433.apk" -Force
Copy-Item "$test/classes.dex" "$output/android-verification.dex" -Force
& $adb -s $Serial push "$output/loki-glass-v433.apk" /data/local/tmp/loki-candidate.apk
& $adb -s $Serial push "$test/classes.dex" /data/local/tmp/loki-fullbody-test.dex
$classes=@('FullBodyConfiguredScenesTest','FullBodySceneTest','FullBodyStandingPenTest','FullBodySpeechViewTest','FullBodyCatalogTest','FullBodyPoseVariationTest')
foreach($name in $classes){
 $result=& $adb -s $Serial shell "CLASSPATH=/data/local/tmp/loki-fullbody-test.dex app_process /system/bin com.example.rokidkeyboardbridge.$name /data/local/tmp/loki-candidate.apk" 2>&1
 $result | Out-File "$output/device-tests-verified.txt" -Append -Encoding utf8
 $result | Where-Object {$_ -match 'PASS|Exception|Assertion|Error'}
 if($LASTEXITCODE -ne 0){throw "$name failed"}
}








