param([Parameter(Mandatory=$true)][string]$Classes,[string]$Output)
$ErrorActionPreference='Stop'
if(!$Output){$Output=Join-Path $Classes '../fullbody-idle-tests'}
New-Item -ItemType Directory -Force $Output | Out-Null
$java='C:/Program Files/Android/Android Studio/jbr/bin'
& "$java/javac.exe" -encoding UTF-8 -cp $Classes -d $Output "$PSScriptRoot/FullBodyIdlePolicyTest.java"
if($LASTEXITCODE -ne 0){throw 'Full-body idle test compilation failed'}
& "$java/java.exe" -cp "$Output;$Classes" com.example.rokidkeyboardbridge.FullBodyIdlePolicyTest
if($LASTEXITCODE -ne 0){throw 'Full-body idle regression failed'}
