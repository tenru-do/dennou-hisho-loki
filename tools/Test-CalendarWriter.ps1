param([Parameter(Mandatory=$true)][string]$JsonJar)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
if(!(Test-Path -LiteralPath $JsonJar)){throw 'Pass a local org.json jar with -JsonJar'}
$out=Join-Path $root '.build-calendar-writer-tests'
New-Item -ItemType Directory -Force $out | Out-Null
$java='C:/Program Files/Android/Android Studio/jbr/bin'
$sources=@(Get-ChildItem "$PSScriptRoot/calendar-writer-tests" -Recurse -Filter '*.java' | ForEach-Object FullName)
$sources+="$root/phone-app/src/com/example/rokidgeminisecretary/CalendarWriter.java"
& "$java/javac.exe" -encoding UTF8 -cp $JsonJar -d $out @sources
if($LASTEXITCODE){throw 'Calendar tests failed to compile'}
& "$java/java.exe" -cp "$out;$JsonJar" com.example.rokidgeminisecretary.CalendarPreflightTest
if($LASTEXITCODE){throw 'Calendar preflight test failed'}
