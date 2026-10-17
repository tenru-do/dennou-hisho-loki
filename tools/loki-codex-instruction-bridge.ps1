$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'secure-bridge.ps1')
. (Join-Path $PSScriptRoot 'codex-stdio-rpc.ps1')
$targetFile=Join-Path $PSScriptRoot 'codex-instruction-target.local.json'
$ledgerFile=Join-Path $PSScriptRoot 'codex-instruction-ledger.local.json'
$config=Get-Content (Join-Path $PSScriptRoot 'token-codex-notify.local.json') -Raw -Encoding UTF8|ConvertFrom-Json
$headers=@{'X-Roki-Token'=[string]$config.token}
$adb=Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
$lastForward=[DateTime]::MinValue;$ledger=@{}
if(Test-Path $ledgerFile){$saved=Get-Content $ledgerFile -Raw -Encoding UTF8|ConvertFrom-Json;foreach($p in $saved.PSObject.Properties){$ledger[$p.Name]=$p.Value}}
function Save-Ledger {
 $temporary=$ledgerFile+'.tmp'
 [IO.File]::WriteAllText($temporary,($ledger|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false))
 Move-Item -LiteralPath $temporary -Destination $ledgerFile -Force
}
function Phone([string]$endpoint,$body=$null){
 if(([DateTime]::UtcNow-$lastForward).TotalSeconds -ge 30 -and $config.phoneSerial){
  $model=(& $adb -s $config.phoneSerial shell getprop ro.product.model 2>$null|Out-String).Trim()
  if($model -and $model -notmatch '(?i)RG[-_ ]?glasses'){
   $pkg=(& $adb -s $config.phoneSerial shell pm path com.example.rokidgeminisecretary 2>$null|Out-String)
   if($pkg.StartsWith('package:')){& $adb -s $config.phoneSerial forward tcp:18765 tcp:8765|Out-Null;& $adb -s $config.phoneSerial forward tcp:18766 tcp:8766|Out-Null}
  }
  $script:lastForward=[DateTime]::UtcNow
 }
 $url=if($config.phoneSerial){'http://127.0.0.1:18765/'+$endpoint}else{'http://'+[string]$config.phoneHost+':8765/'+$endpoint}
 if($null -eq $body){return Invoke-LokiSecureRest -Uri $url -Headers $headers -TimeoutSec 5}
 $json=$body|ConvertTo-Json -Depth 12 -Compress
 $json=[regex]::Replace($json,'[^\x00-\x7f]',{param($m)'\u'+('{0:x4}' -f [int][char]$m.Value)})
 return Invoke-LokiSecureRest -Method Post -Uri $url -Headers $headers -ContentType 'application/json' -Body $json -TimeoutSec 5
}
function Status([string]$text,[string]$identity,[long]$issued){
 $message=@{kind='status';id=$identity;issuedAt=$issued;message=$text}|ConvertTo-Json -Compress
 $uri=if($config.phoneSerial){'http://127.0.0.1:18765/command'}else{'http://'+[string]$config.phoneHost+':8765/command'}
 Invoke-LokiSecureRest -Method Post -Uri $uri -Headers $headers -ContentType 'application/x-www-form-urlencoded; charset=UTF-8' -Body ('command='+[Uri]::EscapeDataString('__CODEX_NOTIFY__:'+$message)) -TimeoutSec 5|Out-Null
}
$created=$false;$mutex=[Threading.Mutex]::new($true,'Local\LokiCodexInstructionBridge',[ref]$created)
if(!$created){exit 0}
try{
 while($true){
  try{
   if(!(Test-Path $targetFile)){Start-Sleep -Seconds 2;continue}
   if(!$script:LokiRpcProcess -or $script:LokiRpcProcess.HasExited){Start-LokiCodexRpc}
   $target=Get-Content $targetFile -Raw -Encoding UTF8|ConvertFrom-Json
   $thread=(Invoke-LokiCodexRpc 'thread/read' @{threadId=[string]$target.threadId;includeTurns=$false}).thread
   if($thread.id -ne $target.threadId -or $thread.name -ne $target.title){throw 'Registered target changed; re-register it'}
   Phone 'codex_register' @{threadId=$target.threadId;title=$target.title}|Out-Null
   $pending=Phone 'codex_pending'
   foreach($request in $pending.requests){
    $id=[string]$request.requestId;$now=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    if(!$ledger.ContainsKey($id)){
     $age=$now-[long]$request.createdAt
     $actual=(Invoke-LokiCodexRpc 'thread/read' @{threadId=[string]$request.threadId;includeTurns=$false}).thread
     if($age -lt 0 -or $age -gt 60000 -or $actual.id -ne $request.threadId -or $actual.name -ne $request.title){
      $ledger[$id]=@{state='rejected';at=$now};Save-Ledger
     }else{
      # Persist before dispatch. An unknown result is never automatically resent.
      $ledger[$id]=@{state='dispatching';at=$now};Save-Ledger
      try{
       $result=Invoke-LokiCodexRpc 'thread/queue/add' @{threadId=[string]$request.threadId;clientUserMessageId=$id;input=@(@{type='text';text=[string]$request.text;text_elements=@()})}
       if($result.queuedSubmission.clientUserMessageId -ne $id){throw 'Codex receipt mismatch'}
       $ledger[$id]=@{state='queued';at=$now;submissionId=$result.queuedSubmission.id};Save-Ledger
      }catch{
       $ledger[$id]=@{state='unknown';at=$now};Save-Ledger
      }
     }
    }
    $receipt=$ledger[$id]
    $text=if($receipt.state -eq 'queued'){'Codexの『'+[string]$request.title+'』に指示を追加しました。作業開始待ちです。'}elseif($receipt.state -eq 'rejected'){'指示の期限または送信先を確認できず、Codexへ送信していません。'}else{'Codexへの送信結果を確認できません。重複を防ぐため自動で再送しません。PCで確認してください。'}
    Status $text ($id+'-'+$receipt.state) ([long]$receipt.at)
    Phone 'codex_ack' @{requestId=$id}|Out-Null
   }
  }catch{
   $failure=@{time=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds();error=$_.Exception.GetType().Name;message=([string]$_.Exception.Message).Replace([string]$config.token,'[redacted]')}
   [IO.File]::WriteAllText((Join-Path $PSScriptRoot 'codex-instruction-status.local.json'),($failure|ConvertTo-Json),[Text.UTF8Encoding]::new($false))
   Stop-LokiCodexRpc
  }
  Start-Sleep -Seconds 2
 }
}finally{Stop-LokiCodexRpc;$mutex.Dispose()}
