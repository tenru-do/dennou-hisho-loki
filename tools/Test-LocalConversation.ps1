param([Parameter(Mandatory=$true)][string]$JsonJar)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$out=Join-Path $root '.build-local-conversation-tests'
New-Item -ItemType Directory -Force $out | Out-Null
$java='C:/Program Files/Android/Android Studio/jbr/bin'
$src=Join-Path $root 'glass-app/src/com/example/rokidkeyboardbridge'
$names=@('ConversationEntryPolicy','ConversationReferentPreview','ConversationCorrections','ConversationTopicBoundary','ConversationHandoffCard','ConversationTaskPolicy','NativeSpeechSession','KeepTopicImagePolicy','NewsQueryPolicy','AssistantActivityState','TransferProgressPolicy','VoiceRevisionPolicy','SpeechCitationPolicy','ConversationFollowupPolicy','ImageReferenceHistory','ImageReferencePolicy','ImageReferenceSummary','ImageAttachmentKinds','JourneyHud')
$sources=@($names | ForEach-Object {Join-Path $src ($_.ToString()+'.java')})
$sources+=Join-Path $PSScriptRoot 'LocalConversationRegressionTest.java'
$sources+=Join-Path $src 'DraftRestorePolicy.java'
$sources+=Join-Path $PSScriptRoot 'DraftRestorePolicyTest.java'
$sources+=Join-Path $root 'phone-app/src/com/example/rokidgeminisecretary/ComposerDraftPolicy.java'
$sources+=Join-Path $PSScriptRoot 'ComposerDraftPolicyTest.java'
& "$java/javac.exe" -encoding UTF8 -cp $JsonJar -d $out @sources
if($LASTEXITCODE){throw 'Local test compilation failed'}
& "$java/java.exe" -cp "$out;$JsonJar" com.example.rokidkeyboardbridge.LocalConversationRegressionTest (Join-Path $PSScriptRoot 'local-conversation-fixtures/routing.json')
if($LASTEXITCODE){throw 'Local conversation regression failed'}

& "$java/java.exe" -cp "$out;$JsonJar" com.example.rokidkeyboardbridge.DraftRestorePolicyTest
if($LASTEXITCODE){throw "Draft restore regression failed"}

& "$java/java.exe" -cp "$out;$JsonJar" com.example.rokidgeminisecretary.ComposerDraftPolicyTest
if($LASTEXITCODE){throw "Composer draft regression failed"}

$sourcesNews=Join-Path $PSScriptRoot 'AudioNewsRegressionTest.java'
& "$java/javac.exe" -encoding UTF8 -cp "$out;$JsonJar" -d $out $sourcesNews
if($LASTEXITCODE){throw 'News regression compilation failed'}
& "$java/java.exe" -cp "$out;$JsonJar" com.example.rokidkeyboardbridge.AudioNewsRegressionTest
if($LASTEXITCODE){throw 'News regression failed'}

& "$java/javac.exe" -encoding UTF8 -cp "$out;$JsonJar" -d $out (Join-Path $PSScriptRoot 'AssistantActivityStateTest.java')
if($LASTEXITCODE){throw 'Activity state test compilation failed'}
& "$java/java.exe" -cp "$out;$JsonJar" com.example.rokidkeyboardbridge.AssistantActivityStateTest
if($LASTEXITCODE){throw 'Activity state regression failed'}

& "$java/javac.exe" -encoding UTF8 -cp "$out;$JsonJar" -d $out (Join-Path $PSScriptRoot 'ConversationFlowTest.java')
if($LASTEXITCODE){throw 'Conversation flow test compilation failed'}
& "$java/java.exe" -cp "$out;$JsonJar" com.example.rokidkeyboardbridge.ConversationFlowTest
if($LASTEXITCODE){throw 'Conversation flow regression failed'}
