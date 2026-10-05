param([Parameter(Mandatory=$true)][string]$Gradle)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$source=Join-Path $env:LOCALAPPDATA 'Android/Sdk'
$mirror=Join-Path $root '.build-navigation-sdk'
$env:JAVA_HOME='C:/Program Files/Android/Android Studio/jbr'
# Keep compiler ZIP-file handling within the workspace instead of the installed SDK.
foreach($relative in @('platforms/android-36','build-tools/35.0.0','platform-tools')) {
    $target=Join-Path $mirror $relative
    $parent=Split-Path $target -Parent
    New-Item -ItemType Directory -Force $parent | Out-Null
    Copy-Item -LiteralPath (Join-Path $source $relative) -Destination $parent -Recurse -Force
}
$env:ANDROID_HOME=$mirror
$env:ANDROID_SDK_ROOT=$mirror
# Clean both modules' generated outputs to avoid stale class discovery in forked javac.
& $Gradle -p (Join-Path $root 'navigation-sdk') --no-daemon :phone:clean :clean :assembleDebug :testDebugUnitTest :phone:assembleDebug :phone:testDebugUnitTest
if($LASTEXITCODE -ne 0) { throw "Navigation library build failed: $LASTEXITCODE" }
Write-Output 'Navigation library, staging phone APK and unit tests passed. No APK installed or SDK navigation activated.'
