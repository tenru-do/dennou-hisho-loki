param([string]$ScriptDirectory)
$ErrorActionPreference='Stop'
if($PSVersionTable.PSVersion.Major -ne 5){throw 'Run this check with Windows PowerShell 5.1.'}
$selfBytes=[IO.File]::ReadAllBytes($PSCommandPath)
if($selfBytes.Length -lt 3 -or $selfBytes[0] -ne 0xef -or $selfBytes[1] -ne 0xbb -or $selfBytes[2] -ne 0xbf){throw 'Runtime test must use UTF-8 BOM.'}
$null=[Text.UTF8Encoding]::new($false,$true).GetString($selfBytes,3,$selfBytes.Length-3)

# The newly compiled WinForms fixture was rejected in the installed environment by CodeIntegrity
# 3077/3033 (Enterprise signing policy). That native replacement was NOT applied. Preserve that
# result separately; this test proves the script implementation using the existing Windows host.
$mutexName='Local\LokiUsbLauncher.ScriptConnect'
$existing=$null
if([Threading.Mutex]::TryOpenExisting($mutexName,[ref]$existing)){
 $existing.Dispose()
 throw 'A Loki script connection is in progress; rerun the isolated test after it completes.'
}
if(!$ScriptDirectory){$ScriptDirectory=$PSScriptRoot}
$source=Join-Path $ScriptDirectory 'Start-Loki-USB.ps1'
$sourceBytes=[IO.File]::ReadAllBytes($source)
if($sourceBytes.Length -lt 3 -or $sourceBytes[0] -ne 0xef -or $sourceBytes[1] -ne 0xbb -or $sourceBytes[2] -ne 0xbf){throw 'Production Start script must use UTF-8 BOM.'}
$sourceText=[Text.UTF8Encoding]::new($false,$true).GetString($sourceBytes,3,$sourceBytes.Length-3)
if(-not $sourceText.Contains($mutexName) -or $sourceText -notmatch '\[switch\]\$CheckConnectionOnly'){
 throw 'The final script must expose CheckConnectionOnly and its script-level named mutex before running this test.'
}
$hasher=[Security.Cryptography.SHA256]::Create()
try{$sourceHash=([BitConverter]::ToString($hasher.ComputeHash($sourceBytes))).Replace('-','').ToLowerInvariant()}finally{$hasher.Dispose()}

$fixtureRoot=Join-Path ([IO.Path]::GetTempPath()) ('LokiLauncherRuntime-'+[Guid]::NewGuid().ToString('N'))
$null=New-Item -ItemType Directory -Path $fixtureRoot
$fixtureScript=Join-Path $fixtureRoot 'Start-Loki-USB.ps1'
$counter=Join-Path $fixtureRoot 'starts.txt'
$windowsPs51='C:/Windows/System32/WindowsPowerShell/v1.0/powershell.exe'
$ownedProcesses=New-Object 'Collections.Generic.List[Diagnostics.Process]'
$fixtureStreams=@{}
try {
 # Exact production bytes are executed. Only adjacent configuration/helper dependencies are fixtures.
 [IO.File]::WriteAllBytes($fixtureScript,$sourceBytes)
 [IO.File]::WriteAllText((Join-Path $fixtureRoot 'launcher.local.json'),'{"phoneSerial":"fixture-phone","adb":"fixture-adb","node":"fixture-node"}',(New-Object Text.UTF8Encoding($true)))
 $helperStub=@'
function Get-LokiAdbMonotonicMs {
 return [long][Math]::Floor(([Diagnostics.Stopwatch]::GetTimestamp()*1000.0)/[Diagnostics.Stopwatch]::Frequency)
}
function Get-LokiAdbRemainingMs {
 param([long]$DeadlineMs)
 return [int][Math]::Max(0,$DeadlineMs-(Get-LokiAdbMonotonicMs))
}
function Confirm-LokiUsbDevice {
 param([string]$Adb,[string]$Serial,[int]$RetryDelayMs=500,[int]$TimeoutMs=30000,[hashtable]$RecoveryState,[switch]$ForceRestart)
 if($Adb -ne 'fixture-adb' -or $Serial -ne 'fixture-phone'){throw 'Only the isolated fake connection is allowed.'}
 [IO.File]::AppendAllText((Join-Path $PSScriptRoot 'starts.txt'),"started`r`n")
 Start-Sleep -Milliseconds 1500
}
function Invoke-LokiAdb {
 param([string]$Adb,[string[]]$Command,[int]$TimeoutMs=5000)
 if($Adb -notin @('fixture-adb','fixture-node')){throw 'A real executable is forbidden in this fixture.'}
 $output=if($Adb -eq 'fixture-node'){('A'*43)+'='}else{''}
 return [pscustomobject]@{Code=0;Output=$output;Error='';Stage='completed';TimedOut=$false}
}
'@
 [IO.File]::WriteAllText((Join-Path $fixtureRoot 'Usb-AdbRecovery.ps1'),$helperStub,(New-Object Text.UTF8Encoding($true)))
 [IO.File]::WriteAllText((Join-Path $fixtureRoot 'verify-usb.cjs'),'// Isolated placeholder, never executed.',(New-Object Text.UTF8Encoding($false)))
 function Start-FixtureProcess {param([string]$Name)
  # Retain the native process handle ourselves: PS5.1 Start-Process can lose ExitCode for a fast duplicate.
  $start=New-Object Diagnostics.ProcessStartInfo
  $start.FileName=$windowsPs51;$start.Arguments='-NoProfile -ExecutionPolicy Bypass -File "'+$fixtureScript+'" -CheckConnectionOnly'
  $start.UseShellExecute=$false;$start.CreateNoWindow=$true;$start.WindowStyle='Hidden'
  $start.RedirectStandardOutput=$true;$start.RedirectStandardError=$true
  $start.StandardOutputEncoding=[Text.Encoding]::Default;$start.StandardErrorEncoding=[Text.Encoding]::Default
  $process=New-Object Diagnostics.Process;$process.StartInfo=$start
  $null=$process.Start()
  $ownedProcesses.Add($process)
  $fixtureStreams[$process.Id]=@{Out=$process.StandardOutput.ReadToEndAsync();Err=$process.StandardError.ReadToEndAsync()}
  return $process
 }
 function Assert-FixtureExit {param([Diagnostics.Process]$Process,[string]$Name)
  # Wait for this exact Windows PowerShell process only; never wait for a descendant/browser tree.
  if(-not $Process.WaitForExit(10000)){throw ($Name+' did not finish within ten seconds.')}
  if($Process.ExitCode -ne 0){
   $stream=$fixtureStreams[$Process.Id].Err
   $detail=if($stream.Wait(200)){$stream.GetAwaiter().GetResult()}else{'stderr was not available within the bounded read'}
   throw ($Name+' failed with exit code '+$Process.ExitCode+': '+$detail)
  }
 }
 $first=Start-FixtureProcess 'first'
 $markerWait=[Diagnostics.Stopwatch]::StartNew()
 while(-not [IO.File]::Exists($counter)){
  if($first.HasExited){Assert-FixtureExit $first 'first';throw 'The first script exited without running the harmless fixture.'}
  if($markerWait.ElapsedMilliseconds -ge 5000){throw 'The harmless fixture did not start within five seconds.'}
  Start-Sleep -Milliseconds 25
 }
 if($first.HasExited){throw 'The first script must still be running when the overlapping launch starts.'}
 $second=Start-FixtureProcess 'second'
 Assert-FixtureExit $second 'second'
 Assert-FixtureExit $first 'first'
 $lines=[IO.File]::ReadAllLines($counter)
 if($lines.Count -ne 1 -or $lines[0] -ne 'started'){throw 'Overlapping scripts entered connection confirmation more than once.'}
 Write-Output 'PASS two overlapping Windows PowerShell 5.1 processes exit zero and enter connection confirmation exactly once'

 $third=Start-FixtureProcess 'third'
 Assert-FixtureExit $third 'third'
 $lines=[IO.File]::ReadAllLines($counter)
 if($lines.Count -ne 2 -or $lines[0] -ne 'started' -or $lines[1] -ne 'started'){throw 'The script mutex was not released for the later independent launch.'}
 if([Convert]::ToBase64String([IO.File]::ReadAllBytes($fixtureScript)) -cne [Convert]::ToBase64String($sourceBytes)){
  throw 'The executed Start script differs from the exact production snapshot.'
 }
 if([Convert]::ToBase64String([IO.File]::ReadAllBytes($source)) -cne [Convert]::ToBase64String($sourceBytes)){
  throw 'The production Start script changed during the test; rerun against the final source.'
 }
 Write-Output 'PASS later script launch enters once after mutex release; no new executable, compilation, adb, Android, browser or certificate access'
 Write-Output ('PASS exact production Start-Loki-USB.ps1 SHA256 '+$sourceHash)
 Write-Output 'NOTE the unapproved new WinForms executable remains unapplied; this result validates only the PowerShell script path'
}finally{
 # Stop only exact process objects created above, without process-name matching or child-tree termination.
 foreach($ownedProcess in $ownedProcesses){
  try{if(-not $ownedProcess.HasExited){$ownedProcess.Kill();$null=$ownedProcess.WaitForExit(1000)}}finally{$ownedProcess.Dispose()}
 }
 $resolvedFixture=[IO.Path]::GetFullPath($fixtureRoot)
 $resolvedTemp=[IO.Path]::GetFullPath([IO.Path]::GetTempPath()).TrimEnd([char]92)+[char]92
 if(-not $resolvedFixture.StartsWith($resolvedTemp,[StringComparison]::OrdinalIgnoreCase) -or [IO.Path]::GetFileName($resolvedFixture) -notmatch '\ALokiLauncherRuntime-[0-9a-f]{32}\z'){
  throw 'Refusing cleanup outside the isolated PowerShell fixture directory.'
 }
 $cleanupUntil=[DateTime]::UtcNow.AddSeconds(3)
 do {
  try{Remove-Item -LiteralPath $resolvedFixture -Recurse -Force -ErrorAction Stop;break}catch{
   if([DateTime]::UtcNow -ge $cleanupUntil){throw}
   Start-Sleep -Milliseconds 100
  }
 }while($true)
}
