param([Parameter(Mandatory=$true)][string]$Classes,[Parameter(Mandatory=$true)][string]$Output)
$ErrorActionPreference='Stop'
New-Item -ItemType Directory -Force $Output | Out-Null
& "$env:JAVA_HOME/bin/javac.exe" -encoding UTF-8 -cp $Classes -d $Output "$PSScriptRoot/NewsSpeechTextTest.java"
if($LASTEXITCODE -ne 0){throw 'News speech test compilation failed'}
& "$env:JAVA_HOME/bin/java.exe" -cp "$Output;$Classes" com.example.rokidgeminisecretary.NewsSpeechTextTest
if($LASTEXITCODE -ne 0){throw 'News speech regression'}
