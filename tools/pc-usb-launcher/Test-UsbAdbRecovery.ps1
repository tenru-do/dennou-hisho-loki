param()
$ErrorActionPreference='Stop'
if($PSVersionTable.PSVersion.Major -ne 5){throw 'Run this check with Windows PowerShell 5.1.'}
$helper=Join-Path $PSScriptRoot 'Usb-AdbRecovery.ps1'
foreach($source in @($helper,$PSCommandPath)){
 $bytes=[IO.File]::ReadAllBytes($source)
 if($bytes.Length -lt 3 -or $bytes[0] -ne 0xef -or $bytes[1] -ne 0xbb -or $bytes[2] -ne 0xbf){throw ($source+' must use UTF-8 BOM.')}
 $null=[Text.UTF8Encoding]::new($false,$true).GetString($bytes,3,$bytes.Length-3)
 $tokens=$null;$parseErrors=$null
 $null=[Management.Automation.Language.Parser]::ParseFile($source,[ref]$tokens,[ref]$parseErrors)
 if($parseErrors.Count){throw ($source+': '+($parseErrors.Message -join '; '))}
}
. $helper
$script:checks=0
function Check {param([bool]$Condition,[string]$Message)
 $script:checks++;if(-not $Condition){throw ('Recovery check failed: '+$Message)}
}

# This executable is a private test fixture, never adb and never an Android device.
$fixtureRoot=Join-Path ([IO.Path]::GetTempPath()) ('LokiAdbRecoveryTest-'+[Guid]::NewGuid().ToString('N'))
$null=New-Item -ItemType Directory -Path $fixtureRoot
$fixture=Join-Path $fixtureRoot 'native-fixture.exe'
$fixtureSource=@'
using System;
using System.Diagnostics;
using System.IO;
using System.Reflection;
using System.Text;
using System.Threading;
class LokiUsbNativeFixture {
 static int Main(string[] args) {
  Console.SetOut(new StreamWriter(Console.OpenStandardOutput(),new UTF8Encoding(false)){AutoFlush=true});
  Console.SetError(new StreamWriter(Console.OpenStandardError(),new UTF8Encoding(false)){AutoFlush=true});
  if(args.Length==0) return 0;
  if(args[0]=="capture") { Console.Out.WriteLine("native stdout 日本語 \u2713 \uD83D\uDE80");Console.Error.WriteLine("native stderr 日本語 \u2713");return 7; }
  if(args[0]=="sleep") { Thread.Sleep(5000);return 0; }
  if(args[0]=="pipe-child") { Thread.Sleep(1600);return 0; }
  if(args[0]=="holdpipe") {
   var info=new ProcessStartInfo(Assembly.GetExecutingAssembly().Location,"pipe-child");
   info.UseShellExecute=false;info.CreateNoWindow=true;
   Process.Start(info);return 0;
  }
  if(args[0]=="args") {
   for(int i=1;i<args.Length;i++) Console.Out.WriteLine((i-1)+":"+Convert.ToBase64String(Encoding.UTF8.GetBytes(args[i])));
   return 0;
  }
  return 3;
 }
}
'@
try {
 Add-Type -TypeDefinition $fixtureSource -OutputAssembly $fixture -OutputType ConsoleApplication
 $result=Invoke-LokiAdb -Adb $fixture -Command @('capture')
 Check ($result.Code -eq 7 -and $result.Output -ceq 'native stdout 日本語 ✓ 🚀' -and $result.Error -ceq 'native stderr 日本語 ✓') 'UTF-8 stdout/stderr must survive independently of CP932 under ErrorActionPreference Stop'
 Check ($result.Stage -eq 'completed' -and -not $result.TimedOut) 'nonzero native exit is structured, not a PowerShell exception'
 $arguments=@('', 'two words', '"quoted"', 'C:\trailing\', 'a\"b', '& | < > $ %', '日本語')
 $result=Invoke-LokiAdb -Adb $fixture -Command (@('args')+$arguments)
 Check ($result.Code -eq 0) 'argument fixture must start'
 $lines=@($result.Output -split '\r?\n')
 Check ($lines.Count -eq $arguments.Count) 'argument quoting must preserve argument count including empty strings'
 for($i=0;$i -lt $arguments.Count;$i++){
  $parts=$lines[$i].Split(':',2)
  $decoded=[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($parts[1]))
  Check ($parts[0] -eq [string]$i -and $decoded -ceq $arguments[$i]) ('argument roundtrip '+$i)
 }
 $result=Invoke-LokiAdb -Adb (Join-Path $fixtureRoot 'missing.exe') -Command @('devices')
 Check ($result.Code -eq 127 -and $result.Stage -eq 'start-failed' -and -not $result.TimedOut) 'launch failure must be structured'
 $timer=[Diagnostics.Stopwatch]::StartNew()
 $result=Invoke-LokiAdb -Adb $fixture -Command @('sleep') -TimeoutMs 220
 $timer.Stop()
 Check ($result.Code -eq 124 -and $result.Stage -eq 'process-timeout' -and $result.TimedOut) 'hung native process must time out'
 Check ($timer.ElapsedMilliseconds -lt 1300) 'hung process must return without waiting five seconds'
 $timer.Restart()
 $result=Invoke-LokiAdb -Adb $fixture -Command @('holdpipe') -TimeoutMs 300
 $timer.Stop()
 Check ($result.Code -eq 124 -and $result.Stage -eq 'stream-timeout' -and $result.TimedOut) 'inherited open stream must share the command deadline'
 Check ($timer.ElapsedMilliseconds -lt 1300) 'stream draining must not wait for the descendant to exit'
 $result=Invoke-LokiAdb -Adb $fixture -Command @('capture') -TimeoutMs 0
 Check ($result.Code -eq 124 -and $result.Stage -eq 'process-timeout') 'no remaining budget must not start a command'
 Write-Output 'PASS real Windows PowerShell 5.1 native fixture: stderr, quoting, launch error, process and stream deadlines'
}finally{
 # A finite 1.6-second pipe holder can outlive its deliberately timed-out parent. Only delete our fixture.
 $resolvedFixture=[IO.Path]::GetFullPath($fixtureRoot)
 $resolvedTemp=[IO.Path]::GetFullPath([IO.Path]::GetTempPath()).TrimEnd([char]92)+[char]92
 if(-not $resolvedFixture.StartsWith($resolvedTemp,[StringComparison]::OrdinalIgnoreCase) -or [IO.Path]::GetFileName($resolvedFixture) -notmatch '\ALokiAdbRecoveryTest-[0-9a-f]{32}\z'){
  throw 'Refusing cleanup outside the private native-fixture directory.'
 }
 $cleanupUntil=[DateTime]::UtcNow.AddSeconds(3)
 do {
  try {Remove-Item -LiteralPath $fixtureRoot -Recurse -Force -ErrorAction Stop;break}catch{
   if([DateTime]::UtcNow -ge $cleanupUntil){throw}
   Start-Sleep -Milliseconds 100
  }
 }while($true)
}

# A virtual monotonic clock verifies elapsed recovery windows, rather than making sleep a no-op.
& {
 function Get-LokiAdbMonotonicMs {return [long]$script:case.Elapsed}
 function Start-Sleep {param([int]$Milliseconds)
  Check ($Milliseconds -gt 0) 'a retry must advance virtual time'
  $script:case.Elapsed+=$Milliseconds
 }
 function Invoke-LokiAdb {param($Adb,$Command,$TimeoutMs=5000)
  Check ($Adb -eq 'fixture-only-no-adb') 'all recovery commands must use only the supplied executable'
  Check ($TimeoutMs -gt 0 -and $TimeoutMs -le 5000 -and $TimeoutMs -le ($script:case.Deadline-$script:case.Elapsed)) 'each command shares the remaining total budget'
  $key=$Command -join ' '
  $script:case.Calls.Add([pscustomobject]@{Command=$key;At=$script:case.Elapsed;Budget=$TimeoutMs})
  if($key -eq 'kill-server'){$script:case.ResetAt=$script:case.Elapsed}
  if($key -eq 'reconnect offline'){$script:case.ReconnectAt=$script:case.Elapsed}
  $output='List of devices attached';$code=0;$stage='completed'
  if($key -eq 'devices'){
   switch($script:case.Mode){
    'healthy' {$output+="`r`nfixture`tdevice`r`nother device"}
    'startup' {if($script:case.Elapsed -ge 2000){$output+="`nfixture device"}}
    'offline' {if($script:case.ReconnectAt -ge 0){$output+="`nfixture device"}else{$output+="`nfixture offline"}}
    'slow-reset' {if($script:case.ResetAt -ge 0 -and $script:case.Elapsed-$script:case.ResetAt -ge $script:case.LateMs){$output+="`nfixture device"}}
    'unauthorized' {$output+="`nfixture unauthorized"}
    'later-unauthorized' {if($script:case.ReconnectAt -ge 0){$output+="`nfixture unauthorized"}}
    'wrong-serial' {$output+="`nother device`nfixture-more device"}
    'nonzero' {$output+="`nfixture device";$code=1}
    'timeout' {$script:case.Elapsed+=$TimeoutMs;$code=124;$stage='process-timeout'}
    'start-failed' {$code=127;$stage='start-failed'}
   }
  }
  return [pscustomobject]@{Code=$code;Output=$output;Error='fixture error';Stage=$stage;TimedOut=($code -eq 124)}
 }
 function Begin-Case {param([string]$Mode,[int]$Timeout=30000,[int]$LateMs=0)
  $script:case=@{Mode=$Mode;Elapsed=0;Deadline=$Timeout;LateMs=$LateMs;ResetAt=-1;ReconnectAt=-1;Calls=(New-Object 'Collections.Generic.List[object]')}
 }
 function Run-Case {param([hashtable]$State,[switch]$ForceRestart,[int]$Delay=500,[string]$Serial='fixture')
  $failure=''
  try {Confirm-LokiUsbDevice -Adb 'fixture-only-no-adb' -Serial $Serial -RetryDelayMs $Delay -TimeoutMs $script:case.Deadline -RecoveryState $State -ForceRestart:$ForceRestart}catch{$failure=$_.Exception.Message}
  return $failure
 }
 function Count-Command {param([string]$Command)
  return @($script:case.Calls | Where-Object {$_.Command -eq $Command}).Count
 }
 Begin-Case 'healthy';$failure=Run-Case
 Check (-not $failure -and $script:case.Elapsed -eq 0 -and $script:case.Calls.Count -eq 1) 'healthy target returns immediately with no mutation'
 Begin-Case 'startup';$failure=Run-Case
 Check (-not $failure -and $script:case.Elapsed -eq 2000 -and (Count-Command 'kill-server') -eq 0 -and (Count-Command 'reconnect offline') -eq 0) 'slow initial enumeration succeeds without reset'
 Begin-Case 'offline';$failure=Run-Case
 Check (-not $failure -and (Count-Command 'reconnect offline') -eq 1 -and (Count-Command 'kill-server') -eq 0) 'offline target can recover without restarting the server'
 Check ($script:case.ReconnectAt -eq 3000) 'normal start has a real three-second discovery window'
 foreach($late in @(1500,6000,12000)){
  Begin-Case 'slow-reset' -LateMs $late;$state=@{ResetCount=0};$failure=Run-Case -State $state
  Check (-not $failure -and $script:case.ResetAt -eq 6000 -and $script:case.Elapsed -eq (6000+$late)) ('post-reset USB enumeration delayed '+$late+'ms still succeeds')
  Check ($state.ResetCount -eq 1 -and (Count-Command 'kill-server') -eq 1) 'exactly one shared reset is used'
  Check ($script:case.ResetAt-$script:case.ReconnectAt -eq 3000) 'reconnect receives three seconds before reset'
 }
 foreach($mode in @('missing','wrong-serial','nonzero')){
  Begin-Case $mode;$state=@{ResetCount=0};$failure=Run-Case -State $state
  Check ([bool]$failure -and $script:case.Elapsed -eq 30000 -and (Count-Command 'kill-server') -eq 1 -and $state.ResetCount -eq 1) ($mode+' must fail within one total deadline and one reset')
  Check ($script:case.Calls.Count -lt 70) 'missing target retries are bounded'
 }
 Begin-Case 'unauthorized';$failure=Run-Case
 Check ($failure -like '*USBデバッグ許可待ち*' -and $script:case.Calls.Count -eq 1 -and (Count-Command 'kill-server') -eq 0) 'unauthorized target stops immediately without restarting'
 Begin-Case 'unauthorized';$state=@{ResetCount=0};$failure=Run-Case -State $state -ForceRestart
 Check ($failure -like '*USBデバッグ許可待ち*' -and $state.ResetCount -eq 0 -and $script:case.Calls.Count -eq 1) 'even force-restart checks authorization before reserving a reset'
 Begin-Case 'later-unauthorized';$failure=Run-Case
 Check ($failure -like '*USBデバッグ許可待ち*' -and (Count-Command 'reconnect offline') -eq 1 -and (Count-Command 'kill-server') -eq 0) 'authorization appearing during polling prevents the later reset'
 Begin-Case 'timeout' -Timeout 11000;$failure=Run-Case
 Check ([bool]$failure -and $failure -like '*タイムアウト*' -and $script:case.Elapsed -eq 11000) 'hung commands consume the same total deadline'
 Check ((Count-Command 'kill-server') -eq 0) 'spent command budget must not trigger a late reset'
 Begin-Case 'start-failed';$failure=Run-Case
 Check ($failure -like '*ADBを開始できません*' -and $script:case.Calls.Count -eq 1 -and $script:case.Elapsed -eq 0) 'invalid executable is a launch error, not a cable diagnosis'
 Begin-Case 'missing' -Timeout 1000;$failure=Run-Case
 Check ([bool]$failure -and $script:case.Elapsed -eq 1000 -and (Count-Command 'reconnect offline') -eq 0 -and (Count-Command 'kill-server') -eq 0) 'short total budget cannot be extended by recovery actions'
 Begin-Case 'missing' -Timeout 100;$failure=Run-Case -Delay 0
 Check ([bool]$failure -and $script:case.Elapsed -eq 100 -and $script:case.Calls.Count -le 101) 'zero legacy delay is clamped and cannot create an infinite loop'
 Begin-Case 'healthy';$state=@{ResetCount=0};$failure=Run-Case -State $state
 Check (-not $failure -and $state.ResetCount -eq 0) 'healthy first stage leaves the shared reset available'
 $failure=Run-Case -State $state -ForceRestart
 Check (-not $failure -and $state.ResetCount -eq 1 -and (Count-Command 'kill-server') -eq 1) 'transport retry can request the one shared reset'
 $failure=Run-Case -State $state -ForceRestart
 Check (-not $failure -and (Count-Command 'kill-server') -eq 1) 'a later force request must not reset twice'
 Begin-Case 'missing' -Timeout 2000;$failure=Run-Case -State @{ResetCount=1} -ForceRestart
 Check ([bool]$failure -and (Count-Command 'kill-server') -eq 0 -and $script:case.Elapsed -eq 2000) 'an already-used reset only polls until the new bounded deadline'
 Begin-Case 'healthy';$failure=Run-Case -Serial "fixture`n"
 Check ([bool]$failure -and $script:case.Calls.Count -eq 0) 'invalid configured serial never invokes a process'
 Write-Output 'PASS virtual-time recovery: healthy, authorization, delayed USB enumeration, offline, wrong serial, failed commands, shared restart and total deadline'
}
Write-Output ('PASS '+$script:checks+' ADB recovery checks; no real adb or Android operations were performed')
