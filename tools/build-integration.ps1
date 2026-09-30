param([ValidateSet('glass','phone')][string]$App)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
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
if (Test-Path -LiteralPath $classes) { Remove-Item -LiteralPath $classes -Recurse -Force }
New-Item -ItemType Directory -Force "$build/classes","$build/dex" | Out-Null
Copy-Item -LiteralPath $android -Destination "$build/android.jar" -Force
$android=Join-Path $build 'android.jar'
function Check-Exit { if($LASTEXITCODE -ne 0) {throw "Build failed: $LASTEXITCODE"} }
& "$bt/aapt2.exe" compile --dir "$source/res" -o "$build/res.zip"
Check-Exit
$link=@('link','--manifest',"$source/AndroidManifest.xml",'-I',$android,'-o',"$build/unsigned.apk","$build/res.zip")
if(Test-Path "$source/assets") {$link+=@('-A',"$source/assets")}
& "$bt/aapt2.exe" @link
Check-Exit
$ErrorActionPreference='Continue'
$output=& "$env:JAVA_HOME/bin/javac.exe" '-J-Duser.language=en' -encoding UTF-8 -source 8 -target 8 -classpath $android -d "$build/classes" (Get-ChildItem "$source/src" -Recurse -Filter *.java).FullName 2>&1
$exitCode=$LASTEXITCODE
$ErrorActionPreference='Stop'
$output | Select-Object -First 14
if($exitCode -ne 0) {throw "javac failed: $exitCode"}
& "$env:JAVA_HOME/bin/jar.exe" cf "$build/program.jar" -C "$build/classes" .
Check-Exit
& "$bt/d8.bat" --lib $android --min-api 28 --output "$build/dex" "$build/program.jar"
Check-Exit
& "$env:JAVA_HOME/bin/jar.exe" uf "$build/unsigned.apk" -C "$build/dex" classes.dex
Check-Exit
& "$bt/zipalign.exe" -f 4 "$build/unsigned.apk" "$build/aligned.apk"
Check-Exit
& "$bt/apksigner.bat" sign --ks "$env:USERPROFILE/.android/debug.keystore" --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --out "$build/loki-$App-integration.apk" "$build/aligned.apk"
Check-Exit
& "$bt/apksigner.bat" verify "$build/loki-$App-integration.apk"
Check-Exit
Write-Output "Verified: $build/loki-$App-integration.apk"
