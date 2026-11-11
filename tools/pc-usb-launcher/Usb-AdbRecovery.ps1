# Compatible with Windows PowerShell 5.1. Capture native stderr instead of throwing NativeCommandError.
function Get-LokiAdbMonotonicMs {
 return [long][Math]::Floor(([Diagnostics.Stopwatch]::GetTimestamp() * 1000.0) / [Diagnostics.Stopwatch]::Frequency)
}
function Get-LokiAdbRemainingMs {
 param([long]$DeadlineMs)
 return [int][Math]::Min([int]::MaxValue,[Math]::Max(0,$DeadlineMs-(Get-LokiAdbMonotonicMs)))
}
function ConvertTo-LokiAdbArgument {
 param([AllowEmptyString()][string]$Argument)
 if($null -eq $Argument){$Argument=''}
 if($Argument.IndexOf([char]0) -ge 0){throw 'ADBの引数に使用できない文字があります。'}
 # Windows CRT backslash rules also preserve empty arguments and trailing backslashes.
 $quoted=New-Object Text.StringBuilder
 [void]$quoted.Append('"');$slashes=0
 foreach($character in $Argument.ToCharArray()){
  if($character -eq [char]92){$slashes++;continue}
  if($character -eq [char]34){
   [void]$quoted.Append([char]92,($slashes*2+1));[void]$quoted.Append([char]34)
  }else{
   if($slashes){[void]$quoted.Append([char]92,$slashes)}
   [void]$quoted.Append($character)
  }
  $slashes=0
 }
 if($slashes){[void]$quoted.Append([char]92,($slashes*2))}
 [void]$quoted.Append('"')
 return $quoted.ToString()
}
function Invoke-LokiAdb {
 param([string]$Adb,[string[]]$Command,[int]$TimeoutMs=5000)
 $deadline=(Get-LokiAdbMonotonicMs)+[Math]::Max(0,$TimeoutMs)
 if($TimeoutMs -le 0){return [pscustomobject]@{Code=124;Output='';Error='ADB応答タイムアウト';Stage='process-timeout';TimedOut=$true}}
 $start=New-Object Diagnostics.ProcessStartInfo
 $start.FileName=$Adb
 $arguments=@(foreach($argument in $Command){ConvertTo-LokiAdbArgument -Argument $argument})
 $start.Arguments=$arguments -join ' ';$start.UseShellExecute=$false;$start.CreateNoWindow=$true
 $start.RedirectStandardOutput=$true;$start.RedirectStandardError=$true
 # adb shell text/XML and the launcher's Node verifier both emit UTF-8, independently of the Windows code page.
 $utf8=New-Object Text.UTF8Encoding($false,$true)
 $start.StandardOutputEncoding=$utf8;$start.StandardErrorEncoding=$utf8
 $process=New-Object Diagnostics.Process;$process.StartInfo=$start
 $started=$false
 try {
  try {$started=$process.Start()}catch{
   return [pscustomobject]@{Code=127;Output='';Error=('ADBを開始できません: '+$_.Exception.Message);Stage='start-failed';TimedOut=$false}
  }
  if(-not $started){return [pscustomobject]@{Code=127;Output='';Error='ADBを開始できません。';Stage='start-failed';TimedOut=$false}}
  $stdout=$process.StandardOutput.ReadToEndAsync();$stderr=$process.StandardError.ReadToEndAsync()
  $remaining=Get-LokiAdbRemainingMs -DeadlineMs $deadline
  if($remaining -le 0 -or -not $process.WaitForExit($remaining)){
   try{if(-not $process.HasExited){$process.Kill()}}catch{}
   return [pscustomobject]@{Code=124;Output='';Error='ADB応答タイムアウト';Stage='process-timeout';TimedOut=$true}
  }
  # An exited child can leave inherited output handles open. Bound stream draining too.
  foreach($streamTask in @($stdout,$stderr)){
   if(-not $streamTask.IsCompleted){
    $remaining=Get-LokiAdbRemainingMs -DeadlineMs $deadline
    if($remaining -le 0 -or -not $streamTask.Wait($remaining)){
     return [pscustomobject]@{Code=124;Output='';Error='ADB出力の待機がタイムアウトしました。';Stage='stream-timeout';TimedOut=$true}
    }
   }
  }
  return [pscustomobject]@{Code=$process.ExitCode;Output=$stdout.GetAwaiter().GetResult().Trim();Error=$stderr.GetAwaiter().GetResult().Trim();Stage='completed';TimedOut=$false}
 }catch{
  return [pscustomobject]@{Code=1;Output='';Error=('ADB出力の取得に失敗しました: '+$_.Exception.Message);Stage='io-failed';TimedOut=$false}
 }finally{
  $process.Dispose()
 }
}
function Invoke-LokiAdbBeforeDeadline {
 param([string]$Adb,[string[]]$Command,[long]$DeadlineMs)
 $remaining=Get-LokiAdbRemainingMs -DeadlineMs $DeadlineMs
 return Invoke-LokiAdb -Adb $Adb -Command $Command -TimeoutMs ([Math]::Min(5000,$remaining))
}
function Restart-LokiAdbOnce {
 param([string]$Adb,[hashtable]$RecoveryState,[long]$DeadlineMs)
 if($RecoveryState.ResetCount -ge 1 -or (Get-LokiAdbRemainingMs -DeadlineMs $DeadlineMs) -le 0){return}
 # Reserve the attempt before invoking kill, including a failed/timed-out attempt.
 $RecoveryState.ResetCount=1
 $null=Invoke-LokiAdbBeforeDeadline -Adb $Adb -Command @('kill-server') -DeadlineMs $DeadlineMs
 if((Get-LokiAdbRemainingMs -DeadlineMs $DeadlineMs) -gt 0){
  $null=Invoke-LokiAdbBeforeDeadline -Adb $Adb -Command @('start-server') -DeadlineMs $DeadlineMs
 }
}
function Confirm-LokiUsbDevice {
 param([string]$Adb,[string]$Serial,[ValidateRange(0,5000)][int]$RetryDelayMs=500,
  [ValidateRange(1,60000)][int]$TimeoutMs=30000,[hashtable]$RecoveryState,[switch]$ForceRestart)
 if($Serial -notmatch '\A[A-Za-z0-9._:-]+\z'){throw 'USB接続先の設定が不正です。'}
 if($null -eq $RecoveryState){$RecoveryState=@{ResetCount=0}}
 if(-not $RecoveryState.ContainsKey('ResetCount')){$RecoveryState.ResetCount=0}
 try{$resetCount=[int]$RecoveryState.ResetCount}catch{throw 'ADB復旧状態の設定が不正です。'}
 if($resetCount -lt 0 -or $resetCount -gt 1){throw 'ADB復旧状態の設定が不正です。'}
 $RecoveryState.ResetCount=$resetCount
 $deadline=(Get-LokiAdbMonotonicMs)+$TimeoutMs
 $phase='initial';$phaseUntil=0;$lastState='未検出';$lastCommandError=''
 $forceRestartPending=[bool]$ForceRestart -and $resetCount -lt 1
 if($ForceRestart){$phase='reset-wait'}
 while((Get-LokiAdbRemainingMs -DeadlineMs $deadline) -gt 0){
  $list=Invoke-LokiAdbBeforeDeadline -Adb $Adb -Command @('devices') -DeadlineMs $deadline
  if($list.Code -eq 0){
   $match=[regex]::Match($list.Output,'(?m)^'+[regex]::Escape($Serial)+'[ \t]+([^\s]+)[ \t]*\r?$')
   $lastState=if($match.Success){$match.Groups[1].Value}else{'未検出'}
   if($lastState -eq 'unauthorized'){throw 'スマホのUSBデバッグ許可待ちです。スマホのロックを解除し、PCへの接続を許可してください。'}
   if($lastState -eq 'device' -and -not $forceRestartPending){return}
  }else{
   $lastCommandError=if($list.Code -eq 124){'ADBの応答または出力待機がタイムアウトしました。'}else{'ADBの端末一覧の取得に失敗しました。'}
   if($list.PSObject.Properties['Stage'] -and $list.Stage -eq 'start-failed'){
    throw ('ADBを開始できません。起動設定のADBの場所を確認してください。 '+$list.Error)
   }
  }
  if((Get-LokiAdbRemainingMs -DeadlineMs $deadline) -le 0){break}
  $now=Get-LokiAdbMonotonicMs
  if($forceRestartPending){
   # Even an explicit transport retry checks authorization before touching the global server.
   Restart-LokiAdbOnce -Adb $Adb -RecoveryState $RecoveryState -DeadlineMs $deadline
   $forceRestartPending=$false;$phase='reset-wait'
  }elseif($phase -eq 'initial'){
   $null=Invoke-LokiAdbBeforeDeadline -Adb $Adb -Command @('start-server') -DeadlineMs $deadline
   $phase='start-wait';$phaseUntil=(Get-LokiAdbMonotonicMs)+3000
  }elseif($phase -eq 'start-wait' -and $now -ge $phaseUntil){
   $null=Invoke-LokiAdbBeforeDeadline -Adb $Adb -Command @('reconnect','offline') -DeadlineMs $deadline
   $phase='reconnect-wait';$phaseUntil=(Get-LokiAdbMonotonicMs)+3000
  }elseif($phase -eq 'reconnect-wait' -and $now -ge $phaseUntil){
   Restart-LokiAdbOnce -Adb $Adb -RecoveryState $RecoveryState -DeadlineMs $deadline
   $phase='reset-wait'
  }
  $delay=[Math]::Min([Math]::Max(1,$RetryDelayMs),(Get-LokiAdbRemainingMs -DeadlineMs $deadline))
  if($phase -in @('start-wait','reconnect-wait')){$delay=[Math]::Min($delay,[Math]::Max(1,$phaseUntil-(Get-LokiAdbMonotonicMs)))}
  if($delay -gt 0){Start-Sleep -Milliseconds ([int]$delay)}
 }
 $recovery=if($RecoveryState.ResetCount -eq 1){'ADBの再接続・1回の再起動後も'}else{'ADBの再接続後も'}
 throw ($recovery+'スマホを確認できませんでした（状態: '+$lastState+'）。'+$lastCommandError+' USBケーブルとスマホの接続許可を確認してください。別の端末への自動切替は行いません。')
}
