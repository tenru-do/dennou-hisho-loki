param([Parameter(Mandatory=$true)][string]$Classes,[Parameter(Mandatory=$true)][string]$Output)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
New-Item -ItemType Directory -Force $Output | Out-Null
$java=Join-Path $env:JAVA_HOME 'bin'
$tests=@('HeadSafeCropTest','ContextVisualPlanTest','AvatarDirectorTest','CharacterVisualRefinementTest','RestLegRelaxTest','CalendarDispatchBoundaryTest','OffDutyRestTest','MotionScenesTest','CharacterGestureLifeTest','ContextReactionTest','RestNaturalIdleTest','NaturalPosePolicyTest','ComfortPoseTest','RestConversationReactionTest','KneeRestTest','RestAnimationTest','CharacterContinuityTest','RestPoseIntentTest','AvatarPostureContextTest','ShyFidgetMotionTest')
$files=@($tests | ForEach-Object {Join-Path $root "tools/$_.java"})
$tests += 'StepsExerciseAdviceTest'
$files += Join-Path $root 'tools/StepsExerciseAdviceTest.java'
$tests += 'EnergyBalanceNudgeTest'
$files += Join-Path $root 'tools/EnergyBalanceNudgeTest.java'
$tests += 'SeatedDisplayScaleChecks'
$files += Join-Path $root 'tools/SeatedDisplayScaleChecks.java'
$tests += 'SeatedGreetingAtlasTest'
$files += Join-Path $root 'tools/SeatedGreetingAtlasTest.java'
$tests += 'SeatedGreetingGeometryTest'
$files += Join-Path $root 'tools/SeatedGreetingGeometryTest.java'
$tests += 'SeatedAnkleMotionTest'
$files += Join-Path $root 'tools/SeatedAnkleMotionTest.java'
$tests += 'SeatedLegWardrobeMeshTest'
$files += Join-Path $root 'tools/SeatedLegWardrobeMeshTest.java'
$tests += 'SeatedLegActionTest'
$files += Join-Path $root 'tools/SeatedLegActionTest.java'
$tests += 'SeatedLegMotionPolicyTest'
$files += Join-Path $root 'tools/SeatedLegMotionPolicyTest.java'
$tests += 'RestKneeFallbackTest'
$files += Join-Path $root 'tools/RestKneeFallbackTest.java'
$tests += 'CharacterViewportScaleTest'
$files += Join-Path $root 'tools/CharacterViewportScaleTest.java'
$tests += 'KeepAutoExposureTest'
$files += Join-Path $root 'tools/KeepAutoExposureTest.java'
$tests += 'SeatedPresenceMotionTest'
$files += Join-Path $root 'tools/SeatedPresenceMotionTest.java'
$tests += 'SeatedLegTweenTest'
$files += Join-Path $root 'tools/SeatedLegTweenTest.java'
$tests += 'DrapedSeatedMotionTest'
$files += Join-Path $root 'tools/DrapedSeatedMotionTest.java'
& "$java/javac.exe" -encoding UTF-8 -cp $Classes -d $Output @files
if($LASTEXITCODE -ne 0){throw 'Character continuity test compilation failed'}
foreach($test in $tests){& "$java/java.exe" -cp "$Output;$Classes" "com.example.rokidkeyboardbridge.$test";if($LASTEXITCODE -ne 0){throw "Character regression: $test"}}
