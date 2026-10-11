param([Parameter(Mandatory=$true)][string]$OutputDirectory)
$ErrorActionPreference='Stop'
$data=Get-Content -LiteralPath "$PSScriptRoot/fullbody-scene-rules.json" -Raw -Encoding UTF8 | ConvertFrom-Json
if($data.schema -ne 1){throw 'Unsupported scene schema'}
function Literal([string]$value){return ConvertTo-Json -InputObject $value -Compress}
function Groups($groups){
 $result=@()
 foreach($group in $groups){
  if(@($group).Count -eq 0){throw 'Empty match group'}
  $words=@();foreach($word in $group){if([string]::IsNullOrWhiteSpace($word)){throw 'Empty keyword'};$words+=Literal $word}
  $result+='new String[]{'+($words -join ',')+'}'
 }
 return 'new String[][]{'+($result -join ',')+'}'
}
$kinds=@('NEUTRAL','LISTEN','EXPLAIN','THINK','JOY','CONFIDENT','ANGER','SAD','CRY','SURPRISE','SHY','WORRY','REFUSE','TIRED','BREATH','PANT','RECOVERY','STRAIN','WALK')
$ids=@{};$lines=@('package com.example.rokidkeyboardbridge;','// Generated from tools/fullbody-scene-rules.json. Do not edit.','final class FullBodyConfiguredScenes {',' static boolean groups(String text,String[][] groups){for(String[] group:groups){boolean found=false;for(String word:group)if(text.contains(word)){found=true;break;}if(!found)return false;}return true;}',' static FullBodySpeechStance.Cue evaluate(String p,String a){')
$ex=@();foreach($word in $data.exclusions){if([string]::IsNullOrWhiteSpace($word)){throw 'Empty exclusion'};$ex+=Literal $word}
$lines+=' if(FullBodyScenePolicy.has(p,'+($ex -join ',')+'))return null;'
foreach($rule in $data.rules){
 if($rule.id -notmatch '^[a-z0-9-]+$' -or $ids.ContainsKey($rule.id)){throw "Invalid/duplicate rule id: $($rule.id)"};$ids[$rule.id]=$true
 if($rule.kind -notin $kinds -or $rule.intensity -notin @(0,1,2,3) -or @($rule.promptGroups).Count -eq 0 -or @($rule.answerGroups).Count -eq 0){throw "Invalid scene rule: $($rule.id)"}
 $lines+=' // '+$rule.id
 $lines+=' if(groups(p,'+(Groups $rule.promptGroups)+')&&groups(a,'+(Groups $rule.answerGroups)+'))return new FullBodySpeechStance.Cue(FullBodyPoseCatalog.Kind.'+$rule.kind+','+$rule.intensity+');'
}
$lines+=@(' return null;',' }','}')
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
[IO.File]::WriteAllLines((Join-Path $OutputDirectory 'FullBodyConfiguredScenes.java'),$lines,(New-Object Text.UTF8Encoding($false)))
$test=@('package com.example.rokidkeyboardbridge;','public final class FullBodyConfiguredScenesTest {',' public static void main(String[]args){FullBodySpeechStance.Cue cue;')
$caseIds=@{}
foreach($case in $data.cases){
 if($caseIds.ContainsKey($case.id)){throw 'Duplicate case id'};$caseIds[$case.id]=$true
 if($null -ne $case.expected -and $case.expected -notin $kinds){throw 'Invalid expected kind'}
 $test+=' cue=FullBodyScenePolicy.evaluate('+(Literal $case.prompt)+','+(Literal $case.answer)+');'
 $condition=if($null -eq $case.expected){'cue!=null'}else{'cue==null||cue.kind!=FullBodyPoseCatalog.Kind.'+$case.expected}
 $test+=' if('+$condition+')throw new AssertionError('+(Literal $case.id)+');'
 if($null -ne $case.expected){
  $test+=' {FullBodySpeechStance stance=new FullBodySpeechStance();stance.begin('+(Literal $case.answer)+');stance.scene('+(Literal $case.prompt)+','+(Literal $case.answer)+');int pose=stance.pose('+(Literal $case.answer)+',4);if(pose<0||!FullBodyPoseCatalog.ALL[pose].supports(FullBodyPoseCatalog.Kind.'+$case.expected+'))throw new AssertionError('+(Literal ($case.id+' pose'))+');}'
 }
}
$test+=@(' System.out.println("PASS configurable scenes: '+@($data.rules).Count+' rules, '+@($data.cases).Count+' dialogue regressions");',' }','}')
$testDirectory=Join-Path (Split-Path $OutputDirectory -Parent) 'scene-tests'
New-Item -ItemType Directory -Force -Path $testDirectory | Out-Null
[IO.File]::WriteAllLines((Join-Path $testDirectory 'FullBodyConfiguredScenesTest.java'),$test,(New-Object Text.UTF8Encoding($false)))
Write-Output "Validated $($data.rules.Count) scene rules and $($data.cases.Count) regression cases"
