param([string]$JavaDirectory='C:/Program Files/Android/Android Studio/jbr/bin')
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$out=Join-Path $root '.build-keep-session-tests'
New-Item -ItemType Directory -Force "$out/classes" | Out-Null
& "$JavaDirectory/javac.exe" '-J-Duser.language=en' -encoding UTF8 -d "$out/classes" "$root/glass-app/src/com/example/rokidkeyboardbridge/CameraPriority.java" "$root/glass-app/src/com/example/rokidkeyboardbridge/KeepSessionController.java" "$PSScriptRoot/KeepSessionControllerTest.java"
if($LASTEXITCODE){throw 'KEEP session test compilation failed'}
& "$JavaDirectory/java.exe" -cp "$out/classes" com.example.rokidkeyboardbridge.KeepSessionControllerTest
if($LASTEXITCODE){throw 'KEEP session lifecycle checks failed'}
