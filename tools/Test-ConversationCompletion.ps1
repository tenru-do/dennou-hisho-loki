$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$out=Join-Path $root '.build-conversation-completion-tests'
New-Item -ItemType Directory -Force $out | Out-Null
$java='C:/Program Files/Android/Android Studio/jbr/bin'
& "$java/javac.exe" -encoding UTF8 -d $out "$root/glass-app/src/com/example/rokidkeyboardbridge/ConversationCompletionPolicy.java" "$root/tools/ConversationCompletionPolicyTest.java"
if($LASTEXITCODE){throw 'Conversation completion tests failed to compile'}
& "$java/java.exe" -cp $out com.example.rokidkeyboardbridge.ConversationCompletionPolicyTest
if($LASTEXITCODE){throw 'Conversation completion regression'}
