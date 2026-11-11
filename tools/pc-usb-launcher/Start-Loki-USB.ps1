param([switch]$CheckConnectionOnly)
$ErrorActionPreference='Stop'
[Console]::OutputEncoding=[Text.Encoding]::Default
$OutputEncoding=New-Object Text.UTF8Encoding($false)
# Keep the existing trusted launcher executable; coordinate its script children here.
$connectGate=New-Object Threading.Mutex($false,'Local\LokiUsbLauncher.ScriptConnect')
$gateHeld=$false
try{$gateHeld=$connectGate.WaitOne(0)}catch [Threading.AbandonedMutexException]{$gateHeld=$true}
if(-not $gateHeld){$connectGate.Dispose();exit 0}
$stage='接続設定の読み込み'
try {
 $config=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'launcher.local.json') -Raw -Encoding UTF8 | ConvertFrom-Json
 $serial=[string]$config.phoneSerial
 if(!$serial){throw 'launcher.local.json の phoneSerial を設定してください。'}
 $adb=if($config.adb){[string]$config.adb}else{Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'}
 $node=if($config.node){[string]$config.node}else{(Get-Command node -ErrorAction Stop).Source}
 $helper=Join-Path $PSScriptRoot 'Usb-AdbRecovery.ps1'
 $helperBytes=[IO.File]::ReadAllBytes($helper)
 if($helperBytes.Length -lt 3 -or $helperBytes[0] -ne 0xef -or $helperBytes[1] -ne 0xbb -or $helperBytes[2] -ne 0xbf){throw 'ADB復旧スクリプトはBOM付きUTF-8で保存してください。'}
 $null=[Text.UTF8Encoding]::new($false,$true).GetString($helperBytes,3,$helperBytes.Length-3)
 . $helper
 $launchDeadline=(Get-LokiAdbMonotonicMs)+80000
 $recovery=@{ResetCount=0}
 function Invoke-LokiUsbStep {
  param([string]$Label,[string[]]$Command,[int]$TimeoutMs=5000,[switch]$RecoverTransport,[switch]$ResetOnTimeout)
  $remaining=Get-LokiAdbRemainingMs -DeadlineMs $launchDeadline
  if($remaining -le 0){throw '接続処理の制限時間80秒を超えました。'}
  $result=Invoke-LokiAdb -Adb $adb -Command $Command -TimeoutMs ([Math]::Min($TimeoutMs,$remaining))
  if($result.Code -ne 0 -and $RecoverTransport -and (($ResetOnTimeout -and $result.Code -eq 124) -or ($result.Error+' '+$result.Output) -match '(?i)(device.*not found|device.*offline|no devices|cannot connect to daemon|server.*out of date|protocol fault|transport.*(?:closed|error)|connection reset)')){
   Confirm-LokiUsbDevice -Adb $adb -Serial $serial -RecoveryState $recovery -ForceRestart -TimeoutMs ([Math]::Max(1,[Math]::Min(30000,(Get-LokiAdbRemainingMs -DeadlineMs $launchDeadline))))
   $result=Invoke-LokiAdb -Adb $adb -Command $Command -TimeoutMs ([Math]::Min($TimeoutMs,(Get-LokiAdbRemainingMs -DeadlineMs $launchDeadline)))
  }
  if($result.Code -ne 0){
   $detail=if($result.Code -eq 124){'ADBの応答が制限時間を超えました。'}elseif($result.Error){$result.Error}else{$result.Output}
   if($detail.Length -gt 360){$detail=$detail.Substring(0,360)}
   throw ($Label+'に失敗しました（終了コード '+$result.Code+'）。'+$detail)
  }
  return $result.Output
 }
 $stage='スマホのUSB接続確認'
 Confirm-LokiUsbDevice -Adb $adb -Serial $serial -RecoveryState $recovery
 $stage='USB接続経路の作成'
 $null=Invoke-LokiUsbStep -Label $stage -Command @('-s',$serial,'forward','tcp:18766','tcp:8766') -RecoverTransport -ResetOnTimeout
 $stage='スマホのロキ起動'
 $null=Invoke-LokiUsbStep -Label $stage -Command @('-s',$serial,'shell','am','start','-f','0x14000000','-n','com.example.rokidgeminisecretary/.MainActivity') -TimeoutMs 8000 -RecoverTransport
 if(-not $CheckConnectionOnly){
  # A current exact button label only. Never approve pairing or another app's dialog.
  $remote='/sdcard/loki-pc-usb-connect-'+[Guid]::NewGuid().ToString('N')+'.xml'
  try {
   function Read-LokiUi {
    $null=Invoke-LokiUsbStep -Label '共有画面の確認' -Command @('-s',$serial,'shell','uiautomator','dump',$remote) -TimeoutMs 7000
    [xml]$tree=Invoke-LokiUsbStep -Label '共有画面の読み込み' -Command @('-s',$serial,'shell','cat',$remote) -TimeoutMs 2000
    return ,$tree
   }
   function Tap-LokiNode($nodes){
    if(@($nodes).Count -ne 1){return $false}
    $item=@($nodes)[0]
    if($item.bounds -notmatch '^\[(\d+),(\d+)\]\[(\d+),(\d+)\]$'){return $false}
    $x=[int](([int]$Matches[1]+[int]$Matches[3])/2);$y=[int](([int]$Matches[2]+[int]$Matches[4])/2)
    $null=Invoke-LokiUsbStep -Label '共有画面を開く操作' -Command @('-s',$serial,'shell','input','tap',([string]$x),([string]$y))
    return $true
   }
   $ui=Read-LokiUi
   if($ui.SelectNodes('//node[@package="com.example.rokidgeminisecretary"]').Count -gt 0 -and -not $ui.SelectSingleNode('//node[@content-desc="ロキWeb接続用QRコード"]')){
    $buttons=$ui.SelectNodes('//node[@package="com.example.rokidgeminisecretary" and (@text="タブレット・PCを接続する（QR・再接続）" or @text="PC・タブレットで写真・会話を共有")]')
    if($buttons.Count -eq 0){
     $settings=$ui.SelectNodes('//node[@package="com.example.rokidgeminisecretary" and @content-desc="設定を開く"]')
     if(Tap-LokiNode $settings){$ui=Read-LokiUi;$buttons=$ui.SelectNodes('//node[@package="com.example.rokidgeminisecretary" and (@text="タブレット・PCを接続する（QR・再接続）" or @text="PC・タブレットで写真・会話を共有")]')}
    }
    if(-not (Tap-LokiNode $buttons)){Write-Host '必要な場合は、スマホのロキ → 設定 →「タブレット・PCを接続する（QR・再接続）」を開いてください。'}
   }
  }catch{Write-Host 'スマホの共有画面を自動表示できませんでした。必要な場合はロキの設定から「タブレット・PCを接続する（QR・再接続）」を開いてください。'}
  finally{$null=Invoke-LokiAdb -Adb $adb -Command @('-s',$serial,'shell','rm','-f',$remote) -TimeoutMs 1500}
 }
 $stage='スマホの証明書照合'
 $pin=''
 for($attempt=0;$attempt -lt 3;$attempt++){
  $check=Invoke-LokiAdb -Adb $node -Command @((Join-Path $PSScriptRoot 'verify-usb.cjs')) -TimeoutMs ([Math]::Min(12000,(Get-LokiAdbRemainingMs -DeadlineMs $launchDeadline)))
  if($check.Code -eq 0 -and $check.Output -match '^[A-Za-z0-9+/]{43}=$'){$pin=$check.Output;break}
  if(($check.Error+' '+$check.Output) -match '(?i)(certificate changed|certificate mismatch|verification required)'){throw '保存済みのスマホ証明書と一致しません。接続を中止しました。'}
  if($attempt -lt 2){Start-Sleep -Milliseconds 500}
 }
 if(!$pin){throw 'スマホの証明書を照合できませんでした。ロキの起動状態とUSB接続を確認してください。'}
 if($CheckConnectionOnly){Write-Output 'PASS USB transport and saved smartphone certificate';exit 0}
 $stage='PC画面を開く'
 $edge=Join-Path ${env:ProgramFiles(x86)} 'Microsoft/Edge/Application/msedge.exe'
 $profile=Join-Path $PSScriptRoot 'private-browser-profile'
 # Only the previously verified phone key, confined to the existing dedicated profile.
 Start-Process -FilePath $edge -ArgumentList @('--app=https://127.0.0.1:18766/web/',('"--user-data-dir='+$profile+'"'),('--ignore-certificate-errors-spki-list='+$pin))
 Write-Host 'スマホにWeb端末の接続確認が出た場合は許可してください。'
}catch{
 [Console]::Error.WriteLine(($stage+'：'+$_.Exception.Message))
 exit 1
}
finally{
 if($gateHeld){$connectGate.ReleaseMutex()}
 $connectGate.Dispose()
}
