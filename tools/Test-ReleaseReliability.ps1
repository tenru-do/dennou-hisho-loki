param([Parameter(Mandatory=$true)][string]$JsonJar)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
& "$PSScriptRoot/Assert-ConversationPromptBaseline.ps1" -App glass
& "$PSScriptRoot/Assert-ConversationPromptBaseline.ps1" -App phone
& "$PSScriptRoot/Test-LocalConversation.ps1" -JsonJar $JsonJar
& "$PSScriptRoot/Test-SecretaryInbox.ps1" -JsonJar $JsonJar
$out=Join-Path $root '.build-release-reliability-tests'
New-Item -ItemType Directory -Force $out | Out-Null
$j='C:/Program Files/Android/Android Studio/jbr/bin'
$g=Join-Path $root 'glass-app/src/com/example/rokidkeyboardbridge'
$p=Join-Path $root 'phone-app/src/com/example/rokidgeminisecretary'
$sources=@("$g/DeferredNotificationDigest.java","$g/CodexNotificationPolicy.java","$g/ConversationFollowupPolicy.java","$g/ConversationTopicBoundary.java","$g/ConversationTaskPolicy.java","$p/ComposerReceiptSummary.java","$p/TrackedWebRequest.java","$p/CalendarReviewSummary.java","$p/ReadingUncertainty.java","$PSScriptRoot/ReleaseReliabilityPhoneTest.java","$PSScriptRoot/ReleaseReliabilityGlassTest.java","$p/CorrectionDraft.java","$p/ConnectionCheckpoint.java","$g/NavigationFreshness.java","$PSScriptRoot/CheckpointPhoneTest.java","$PSScriptRoot/NavigationFreshnessTest.java","$p/VolumeProfilePolicy.java","$p/MeetingActionPolicy.java","$g/ResourceLoadPolicy.java","$PSScriptRoot/TuningPhoneTest.java","$PSScriptRoot/ResourceLoadTest.java")
$sources+=@("$p/QuietHoursPolicy.java","$p/RecordSearchPolicy.java","$PSScriptRoot/RecordsPolicyTest.java")
& "$j/javac.exe" -encoding UTF8 -cp $JsonJar -d $out @sources
if($LASTEXITCODE){throw 'Reliability compilation failed'}
foreach($name in @('com.example.rokidkeyboardbridge.ReleaseReliabilityGlassTest','com.example.rokidgeminisecretary.ReleaseReliabilityPhoneTest','com.example.rokidgeminisecretary.CheckpointPhoneTest','com.example.rokidkeyboardbridge.NavigationFreshnessTest','com.example.rokidgeminisecretary.TuningPhoneTest','com.example.rokidkeyboardbridge.ResourceLoadTest','com.example.rokidgeminisecretary.RecordsPolicyTest')){
 & "$j/java.exe" -cp "$out;$JsonJar" $name
 if($LASTEXITCODE){throw "Reliability regression failed: $name"}
}
$node=Join-Path $env:USERPROFILE '.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node.exe'
if(!(Test-Path -LiteralPath $node)){throw 'Browser test Node runtime is unavailable'}
& $node "$PSScriptRoot/ReleaseReliabilityWebTest.cjs"
if($LASTEXITCODE){throw 'Browser reliability regression failed'}
& $node "$PSScriptRoot/CheckpointWebTest.cjs"
if($LASTEXITCODE){throw 'Checkpoint browser regression failed'}
& $node "$PSScriptRoot/TuningWebTest.cjs"
if($LASTEXITCODE){throw 'Tuning/reference/meeting browser regression failed'}
& $node "$PSScriptRoot/RecordsWebTest.cjs"
if($LASTEXITCODE){throw 'Records browser regression failed'}
& "$PSScriptRoot/Test-AssistantReview.ps1" -JsonJar $JsonJar
& $node "$PSScriptRoot/AssistantReviewWebTest.cjs"
if($LASTEXITCODE){throw 'Assistant review browser regression failed'}
& $node "$PSScriptRoot/ReliabilityCenterWebTest.cjs"
if($LASTEXITCODE){throw 'Reliability center browser regression failed'}
& $node "$PSScriptRoot/MaintenanceCenterWebTest.cjs"
if($LASTEXITCODE){throw 'Maintenance center browser regression failed'}
& $node "$PSScriptRoot/ExperienceCenterWebTest.cjs"
if($LASTEXITCODE){throw 'Experience center browser regression failed'}
Write-Output 'Release reliability regressions PASS. No external API, device capture, calendar mutation, or install was performed.'
