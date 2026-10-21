param([Parameter(Mandatory=$true)][string]$JsonJar)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$out=Join-Path $root '.build-review-tests'
New-Item -ItemType Directory -Force $out | Out-Null
$p=Join-Path $root 'phone-app/src/com/example/rokidgeminisecretary'
$f=Join-Path $root 'tools/secretary-inbox-tests'
$j='C:/Program Files/Android/Android Studio/jbr/bin'
$sources=@("$f/android/content/Context.java","$f/android/util/AtomicFile.java","$f/com/example/rokidgeminisecretary/DeviceSecrets.java","$f/com/example/rokidgeminisecretary/CalendarWriter.java")
$sources+=@('ReviewStore','UsageLedger','CancellableWork','MeetingConsent','TranscriptTimeline','PersonalNotes','ReadingHistory','ReadingCorrectionContext','ImageReferenceSummary','ImageAttachmentKinds','BookmarkStore','BookmarkNotes','UnifiedRecords','RecordSearchPolicy','SecretaryInbox','SecretaryInboxPresentation','PhotoVaultCipher') | ForEach-Object {"$p/$_.java"}
$sources+=@("$root/glass-app/src/com/example/rokidkeyboardbridge/ConversationTrace.java","$PSScriptRoot/ConversationTraceTest.java")
$sources+=@(Get-ChildItem "$PSScriptRoot/review-tests" -Recurse -Filter '*.java').FullName
& "$j/javac.exe" -encoding UTF8 -cp $JsonJar -d $out @sources
if($LASTEXITCODE){throw 'Review test compilation failed'}
& "$j/java.exe" -cp "$out;$JsonJar" com.example.rokidgeminisecretary.AssistantReviewStoreTest
if($LASTEXITCODE){throw 'Review regression failed'}

& "$j/java.exe" -cp "$out;$JsonJar" com.example.rokidkeyboardbridge.ConversationTraceTest
if($LASTEXITCODE){throw "Trace regression failed"}

& "$j/java.exe" -cp "$out;$JsonJar" com.example.rokidgeminisecretary.ReliabilityStoreTest
if($LASTEXITCODE){throw "Reliability store regression failed"}
