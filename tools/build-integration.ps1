param([ValidateSet('glass','phone','camera','audio-relay')][string]$App,[switch]$Development,[switch]$Release,[string]$KeyStorePath,[string]$KeyAlias)
$ErrorActionPreference='Stop'
if($Release){
    if($Development){throw 'Release cannot be combined with Development'}
    if(!$KeyStorePath -or !$KeyAlias -or !(Test-Path -LiteralPath $KeyStorePath) -or !$env:LOKI_RELEASE_STORE_PASSWORD -or !$env:LOKI_RELEASE_KEY_PASSWORD){throw 'Release requires an explicit keystore, alias and LOKI_RELEASE_STORE_PASSWORD/LOKI_RELEASE_KEY_PASSWORD environment variables'}
}
$root=Split-Path $PSScriptRoot -Parent
if($App -eq 'phone' -or $App -eq 'glass'){& "$PSScriptRoot/Assert-ConversationPromptBaseline.ps1" -App $App}
if($App -eq 'phone' -or $App -eq 'glass'){& "$PSScriptRoot/generate-loki-self-profile.ps1"}
if($App -eq 'glass'){& "$PSScriptRoot/Test-ConversationCompletion.ps1"}
$sdk=Join-Path $env:LOCALAPPDATA 'Android/Sdk'
$env:JAVA_HOME='C:/Program Files/Android/Android Studio/jbr'
$env:PATH="$env:JAVA_HOME/bin;$env:PATH"
$bt=Join-Path $sdk 'build-tools/37.0.0'
$android=Join-Path $sdk 'platforms/android-35/android.jar'
$source=Join-Path $root "$App-app"
$build=Join-Path $root ".build-integration/$App"
$classes=[System.IO.Path]::GetFullPath((Join-Path $build 'classes'))
$expectedBuild=[System.IO.Path]::GetFullPath((Join-Path $root ".build-integration/$App"))
if (-not $classes.StartsWith($expectedBuild.TrimEnd('\')+'\', [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Refusing to clean a classes directory outside this build workspace'
}
if (Test-Path -LiteralPath $expectedBuild) { Remove-Item -LiteralPath $expectedBuild -Recurse -Force }
New-Item -ItemType Directory -Force "$build/classes","$build/dex" | Out-Null
Copy-Item -LiteralPath $android -Destination "$build/android.jar" -Force
$android=Join-Path $build 'android.jar'
function Check-Exit { if($LASTEXITCODE -ne 0) {throw "Build failed: $LASTEXITCODE"} }
& "$bt/aapt2.exe" compile --dir "$source/res" -o "$build/res.zip"
Check-Exit
$buildManifest=Join-Path $build 'AndroidManifest.xml'
$manifestText=[IO.File]::ReadAllText("$source/AndroidManifest.xml")
if($Development){$manifestText=$manifestText -replace 'android:debuggable="false"','android:debuggable="true"'}
[IO.File]::WriteAllText($buildManifest,$manifestText,[Text.UTF8Encoding]::new($false))
$link=@('link','--manifest',$buildManifest,'-I',$android,'-o',"$build/unsigned.apk","$build/res.zip")
New-Item -ItemType Directory -Force "$build/generated" | Out-Null
$link+=@('--java',"$build/generated")
if(Test-Path "$source/assets") {$link+=@('-A',"$source/assets")}
& "$bt/aapt2.exe" @link
Check-Exit
if($App -eq 'phone' -and (Test-Path "$source/assets/loki-web")) {
    & "$env:JAVA_HOME/bin/java.exe" "$root/tools/NormalizeWebAssets.java" "$build/unsigned.apk"
    Check-Exit
}
New-Item -ItemType Directory -Force "$build/compiler" | Out-Null
$ErrorActionPreference='Continue'
$bootstrap=& "$env:JAVA_HOME/bin/javac.exe" '-J-Duser.language=en' -encoding UTF-8 -d "$build/compiler" "$root/tools/CompileAndroid.java" 2>&1
$bootstrapExit=$LASTEXITCODE
$ErrorActionPreference='Stop'
foreach($entry in $bootstrap){Write-Output ([string]$entry)}
if($bootstrapExit -ne 0){throw "compiler bootstrap failed: $bootstrapExit"}
if($App -eq 'glass') {
    & "$root/tools/generate-fullbody-scenes.ps1" -OutputDirectory "$build/generated"
}
$sourceFiles=@((Get-ChildItem "$source/src" -Recurse -Filter *.java).FullName)+(Get-ChildItem "$build/generated" -Recurse -Filter *.java).FullName
$sourceList=Join-Path $build 'java-sources.txt'
[IO.File]::WriteAllLines($sourceList,[string[]]$sourceFiles,(New-Object Text.UTF8Encoding($false)))
$ErrorActionPreference='Continue'
$output=& "$env:JAVA_HOME/bin/java.exe" '-Duser.language=en' -cp "$build/compiler" CompileAndroid $android "$build/classes" $sourceList "$build/compiler-diagnostics.log" 2>&1
$exitCode=$LASTEXITCODE
$ErrorActionPreference='Stop'
foreach($entry in $output){Write-Output ([string]$entry)}
if($exitCode -ne 0){throw "Android compilation failed: $exitCode"}
& "$env:JAVA_HOME/bin/jar.exe" cf "$build/program.jar" -C "$build/classes" .
Check-Exit
& "$bt/d8.bat" --lib $android --min-api 28 --output "$build/dex" "$build/program.jar"
Check-Exit
& "$env:JAVA_HOME/bin/jar.exe" uf "$build/unsigned.apk" -C "$build/dex" classes.dex
Check-Exit
& "$bt/zipalign.exe" -f 4 "$build/unsigned.apk" "$build/aligned.apk"
Check-Exit
if($Release){
    & "$bt/apksigner.bat" sign --ks $KeyStorePath --ks-key-alias $KeyAlias --ks-pass env:LOKI_RELEASE_STORE_PASSWORD --key-pass env:LOKI_RELEASE_KEY_PASSWORD --out "$build/loki-$App-integration.apk" "$build/aligned.apk"
}else{
    & "$bt/apksigner.bat" sign --ks "$env:USERPROFILE/.android/debug.keystore" --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --out "$build/loki-$App-integration.apk" "$build/aligned.apk"
}
Check-Exit
& "$bt/apksigner.bat" verify "$build/loki-$App-integration.apk"
Check-Exit
Write-Output "Verified: $build/loki-$App-integration.apk"
