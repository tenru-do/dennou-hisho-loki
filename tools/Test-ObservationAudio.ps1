param([Parameter(Mandatory=$true)][string]$JsonJar)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$dest=Join-Path ([IO.Path]::GetTempPath()) ('loki-observation-tests-'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $dest | Out-Null
$java='C:/Program Files/Android/Android Studio/jbr/bin'
$files=@('tools/ObservationAudioPolicyTest.java','tools/RadioObservationStoreTest.java','glass-app/src/com/example/rokidkeyboardbridge/ObservationAudioPolicy.java','glass-app/src/com/example/rokidkeyboardbridge/VoiceCaptureTimingPolicy.java','glass-app/src/com/example/rokidkeyboardbridge/MeetingPcmRing.java','phone-app/src/com/example/rokidgeminisecretary/RadioObservationStore.java') | ForEach-Object {Join-Path $root $_}
& "$java/javac.exe" -encoding UTF8 -cp $JsonJar -d $dest @files
if($LASTEXITCODE -ne 0){throw 'Observation test compilation failed'}
foreach($test in @('com.example.rokidkeyboardbridge.ObservationAudioPolicyTest','com.example.rokidgeminisecretary.RadioObservationStoreTest')){
 & "$java/java.exe" -cp "$dest;$JsonJar" $test
 if($LASTEXITCODE -ne 0){throw "Observation test failed: $test"}
}
