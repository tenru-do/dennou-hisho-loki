param(
    [switch]$Watch,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$HookArguments
)
. (Join-Path $PSScriptRoot 'secure-bridge.ps1')
. (Join-Path $PSScriptRoot 'codex-notify-policy.ps1')
. (Join-Path $PSScriptRoot 'codex-notify-runtime.ps1')


$ErrorActionPreference = 'SilentlyContinue'
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$configPath = Join-Path $scriptDir 'token-codex-notify.local.json'
$logPath = Join-Path $scriptDir 'codex-notify.log'
$outboxPath = Join-Path $scriptDir 'codex-notify-outbox.local.json'
$script:notifyOutbox = @(Read-LokiNotifyState $outboxPath @())
foreach($row in $script:notifyOutbox){if(!$row.id -or !$row.message -or !$row.issuedAt){throw 'Invalid notification outbox; file preserved'}}
function Save-NotifyOutbox { Save-LokiNotifyState $outboxPath @($script:notifyOutbox) }
function Queue-NotifyEnvelope([string]$message){
 if(!$message){return};$data=$message|ConvertFrom-Json -ErrorAction Stop
 if(@($script:notifyOutbox|Where-Object {$_.id -eq $data.id}).Count){Save-NotifyOutbox;return}
 $script:notifyOutbox+=@{id=$data.id;issuedAt=$data.issuedAt;message=$message;nextAttempt=0L};Save-NotifyOutbox
}
function Flush-NotifyOutbox {
 if(!$script:notifyOutbox.Count){return}
 $now=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds();$keep=@();$changed=$false
 foreach($item in $script:notifyOutbox){
  if([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()-$now -gt 12000){$keep+=$item;continue}
  if($now-[long]$item.issuedAt -gt 300000 -or [long]$item.issuedAt-$now -gt 30000){$changed=$true;continue}
  if([long]$item.nextAttempt -gt $now){$keep+=$item;continue}
  $sent=$false;try{$sent=Send-GlassNotification ([string]$item.message)}catch{Write-NotifyLog ('send exception '+$_.Exception.GetType().Name)}
  $changed=$true;if(!$sent){$item.nextAttempt=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()+15000;$keep+=$item}
 }
 if($changed){$before=$script:notifyOutbox;$script:notifyOutbox=$keep;try{Save-NotifyOutbox}catch{$script:notifyOutbox=$before;throw}}
}
$adbCandidates = @(
    $(if ($env:ANDROID_HOME) { Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe' }),
    $(if ($env:ANDROID_SDK_ROOT) { Join-Path $env:ANDROID_SDK_ROOT 'platform-tools\adb.exe' }),
    $(Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe')
) | Where-Object { $_ -and (Test-Path $_) }
$adb = $adbCandidates | Select-Object -First 1
$forwardPort = 18765
$computerUseHookPattern = Join-Path $env:LOCALAPPDATA 'OpenAI\Codex\runtimes\cua_node\*\bin\node_modules\@oai\sky\bin\windows\codex-computer-use.exe'
$originalHook = Get-ChildItem -Path $computerUseHookPattern -File |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1 -ExpandProperty FullName

function Write-NotifyLog([string]$text) {
    $dir = Split-Path $logPath
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    Add-Content -Path $logPath -Encoding UTF8 -Value ("{0:o} {1}" -f (Get-Date), $text)
}

$script:lastDiscoveryAt=0L
function Find-RokiPhoneHosts {
 $now=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds();if($now-$script:lastDiscoveryAt -lt 60000){return @()};$script:lastDiscoveryAt=$now
    $prefixes = New-Object 'System.Collections.Generic.HashSet[string]'
    foreach ($nic in [Net.NetworkInformation.NetworkInterface]::GetAllNetworkInterfaces()) {
        if ($nic.OperationalStatus -ne [Net.NetworkInformation.OperationalStatus]::Up) { continue }
        $props = $nic.GetIPProperties()
        $hasIpv4Gateway = @($props.GatewayAddresses | Where-Object {
            $_.Address.AddressFamily -eq [Net.Sockets.AddressFamily]::InterNetwork
        }).Count -gt 0
        if (!$hasIpv4Gateway) { continue }
        foreach ($unicast in $props.UnicastAddresses) {
            if ($unicast.Address.AddressFamily -ne [Net.Sockets.AddressFamily]::InterNetwork) { continue }
            $parts = $unicast.Address.ToString().Split('.')
            if ($parts.Count -eq 4) { [void]$prefixes.Add("$($parts[0]).$($parts[1]).$($parts[2])") }
        }
    }
    $attempts = @()
    foreach ($prefix in $prefixes) {
        foreach ($last in 1..254) {
            $hostAddress = "$prefix.$last"
            $client = New-Object Net.Sockets.TcpClient
            $attempts += [pscustomobject]@{
                Host = $hostAddress
                Client = $client
                Async = $client.BeginConnect($hostAddress, 8765, $null, $null)
            }
        }
    }
    Start-Sleep -Milliseconds 700
    $openHosts = @()
    foreach ($attempt in $attempts) {
        try {
            if ($attempt.Async.IsCompleted -and $attempt.Client.Connected) {
                $attempt.Client.EndConnect($attempt.Async)
                $openHosts += $attempt.Host
            }
        } catch {} finally {
            $attempt.Client.Close()
        }
    }
    return @($openHosts | Select-Object -Unique)
}

function Send-GlassNotification([string]$message) {
    if ([string]::IsNullOrWhiteSpace($message) -or !(Test-Path $configPath)) { return $false }
    $cfg = Get-Content -Raw -Encoding UTF8 $configPath | ConvertFrom-Json
    # Keep the HTTP body ASCII-only. The Android bridge's compact HTTP parser
    # counts Content-Length bytes, so raw multibyte Japanese JSON can block it.
    $body = 'command=' + [Uri]::EscapeDataString('__CODEX_NOTIFY__:' + $message.Trim())
    $contentType = 'application/x-www-form-urlencoded; charset=UTF-8'
    $headers = @{ 'X-Roki-Token' = [string]$cfg.token }
    $tried = New-Object 'System.Collections.Generic.HashSet[string]'
    $targets = @()
    if ($cfg.phoneHost) { $targets += "http://$($cfg.phoneHost):8765/command" }
    foreach ($target in $targets) {
        [void]$tried.Add($target)
        try {
            $result = Invoke-LokiSecureRest -Method Post -Uri $target -Headers $headers -ContentType $contentType -Body $body -TimeoutSec 4
            if ($result.ok) {
                Write-NotifyLog 'sent network notification'
                return $true
            }
        } catch {
            Write-NotifyLog ('network send failed '+$_.Exception.GetType().Name)
        }
    }
    foreach ($discoveredHost in (Find-RokiPhoneHosts)) {
        $target = "http://$discoveredHost`:8765/command"
        if ($tried.Contains($target)) { continue }
        [void]$tried.Add($target)
        try {
            $result = Invoke-LokiSecureRest -Method Post -Uri $target -Headers $headers -ContentType $contentType -Body $body -TimeoutSec 3
            if ($result.ok) {
                if ([string]$cfg.phoneHost -ne $discoveredHost) {
                    $cfg.phoneHost = $discoveredHost
                    $cfg | ConvertTo-Json | Set-Content -Path $configPath -Encoding UTF8
                    Write-NotifyLog 'updated phone host'
                }
                Write-NotifyLog 'sent network notification'
                return $true
            }
        } catch {
            Write-NotifyLog ('network send failed '+$_.Exception.GetType().Name)
        }
    }
    if ($adb -and (Test-Path $adb)) {
        $candidateSerials = @()
        if ($cfg.phoneSerial) { $candidateSerials += [string]$cfg.phoneSerial }
        $candidateSerials += @(& $adb devices | ForEach-Object {
            if ($_ -match '^(\S+)\s+device$') { $Matches[1] }
        })
        foreach ($serial in @($candidateSerials | Select-Object -Unique)) {
            & $adb -s $serial forward "tcp:$forwardPort" 'tcp:8765' | Out-Null
            & $adb -s $serial forward tcp:18766 'tcp:8766' | Out-Null
            if ($LASTEXITCODE -ne 0) { continue }
            $target = "http://127.0.0.1:$forwardPort/command"
            try {
                $result = Invoke-LokiSecureRest -Method Post -Uri $target -Headers $headers -ContentType $contentType -Body $body -TimeoutSec 4
                if ($result.ok) {
                    Write-NotifyLog 'sent USB notification'
                    return $true
                }
            } catch {
                Write-NotifyLog ('USB send failed '+$_.Exception.GetType().Name)
            }
        }
    }
    return $false
}

function Format-CompleteMessage([string]$lastMessage) {
    $cfg = Get-Content -Raw -Encoding UTF8 $configPath | ConvertFrom-Json
    $completeText = if ($cfg.completeMessage) { [string]$cfg.completeMessage } else { 'Codex task completed.' }
    $last = ($lastMessage -replace '\s+', ' ').Trim()
    if ($last.Length -gt 110) { $last = $last.Substring(0, 110) + '...' }
    if ($last) { return $completeText + ' ' + $last }
    return $completeText
}

function Get-HookMessage([string[]]$arguments) {
    $jsonText = ($arguments | Where-Object { $_ -and $_.Trim().StartsWith('{') } | Select-Object -Last 1)
    if (!$jsonText) { return Format-CompleteMessage '' }
    try {
        $event = $jsonText | ConvertFrom-Json
        $last = [string]$event.PSObject.Properties['last-assistant-message'].Value
        if (!$last) { $last = [string]$event.last_agent_message }
        return Format-CompleteMessage $last
    } catch {}
    return Format-CompleteMessage ''
}

function Watch-CodexApprovals {
 $createdNew=$false;$watcherMutex=New-Object Threading.Mutex($true,'Local\RokiCodexNotifyWatcher',[ref]$createdNew)
 if(!$createdNew){$watcherMutex.Dispose();return}
 $checkpointPath=Join-Path $scriptDir 'codex-notify-checkpoint.local.json';$healthPath=Join-Path $scriptDir 'codex-notify-health.local.json'
 $sessionRoot=Join-Path $env:USERPROFILE '.codex\sessions';$offsets=@{}; $anchors=@{};$discarding=@{};$known=@();$scanAt=0L;$lastHealth=0L
 try{
  $saved=Read-LokiNotifyState $checkpointPath $null;$firstBoot=$null -eq $saved
  if($saved -and [int]$saved.version -ne 1){throw 'Unsupported notification checkpoint; file preserved'}
  if($saved){foreach($entry in @($saved.files)){$offsets[[string]$entry.path]=[long]$entry.offset;$discarding[[string]$entry.path]=[bool]$entry.discarding;$anchors[[string]$entry.path]=[string]$entry.anchor}}
  Write-NotifyLog 'approval watcher started'
  while($true){
   try{
    $now=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    if($now-$scanAt -ge 15000){
     $localConfig=Get-Content -LiteralPath $configPath -Raw -Encoding UTF8 -ErrorAction Stop|ConvertFrom-Json -ErrorAction Stop
     $roots=@((Split-Path $scriptDir -Parent))+@($localConfig.projectRoots|Where-Object {$_});$eligible=@()
     foreach($file in @(Get-ChildItem -LiteralPath $sessionRoot -Recurse -Filter '*.jsonl' -ErrorAction Stop)){
      if(!(Test-LokiProjectSession $file.FullName $roots)){continue};$eligible+=$file.FullName
      if(!$offsets.ContainsKey($file.FullName)){$offsets[$file.FullName]=if($firstBoot){$file.Length}else{0L};$discarding[$file.FullName]=$false;$anchors[$file.FullName]=""}
     }
     $known=$eligible;$scanAt=$now
     if($firstBoot){$rows=@(foreach($path in $known){@{path=$path;offset=$offsets[$path];discarding=$discarding[$path];anchor=$anchors[$path]}});Save-LokiNotifyState $checkpointPath @{version=1;files=$rows};$firstBoot=$false}
    }
    foreach($path in $known){
     try{
      $chunk=[LokiNotifyRuntime]::Read($path,[long]$offsets[$path],[bool]$discarding[$path],4194304,[string]$anchors[$path])
      if($chunk.Oversized){Write-NotifyLog 'oversized session record skipped'}
      foreach($line in $chunk.Text.Split([char]10)){
       if([string]::IsNullOrWhiteSpace($line)){continue}
       try{$record=$line|ConvertFrom-Json -ErrorAction Stop}catch{Write-NotifyLog 'malformed complete session record skipped';continue}
       foreach($question in @(New-LokiQuestionEnvelopes $record $path)){Queue-NotifyEnvelope $question}
       if($record.type -eq 'event_msg' -and $record.payload.type -eq 'task_complete'){
        $envelope=New-LokiCompletionEnvelope ([string]$record.payload.last_agent_message) $path ([DateTimeOffset]::Parse([string]$record.timestamp));Queue-NotifyEnvelope $envelope
       }
      }
      if($chunk.Next -ne $offsets[$path] -or $chunk.Discarding -ne $discarding[$path] -or $chunk.Anchor -ne $anchors[$path]){
       # Durable outbox first, checkpoint second. Restart replays keep the same IDs.
       $offsets[$path]=$chunk.Next;$discarding[$path]=$chunk.Discarding;$anchors[$path]=$chunk.Anchor
       $rows=@(foreach($file in $known){@{path=$file;offset=$offsets[$file];discarding=$discarding[$file];anchor=$anchors[$file]}});Save-LokiNotifyState $checkpointPath @{version=1;files=$rows}
      }
     }catch{Write-NotifyLog ('session read retry '+$_.Exception.GetType().Name)}
    }
    if($now-$lastHealth -ge 10000){Write-LokiNotifyHealth $healthPath 'polling' $script:notifyOutbox.Count $known.Count;$lastHealth=$now}
    Flush-NotifyOutbox
   }catch{
    Write-NotifyLog ('watch cycle retry '+$_.Exception.GetType().Name)
    Write-LokiNotifyHealth $healthPath 'retrying' $script:notifyOutbox.Count $known.Count ($_.Exception.GetType().Name)
   }
   Start-Sleep -Seconds 2
  }
 }catch{
  Write-NotifyLog ('watcher stopped '+$_.Exception.GetType().Name)
  Write-LokiNotifyHealth $healthPath 'faulted' $script:notifyOutbox.Count $known.Count ($_.Exception.GetType().Name)
  throw
 }finally{$watcherMutex.ReleaseMutex();$watcherMutex.Dispose()}
}


function Ensure-ApprovalWatcher {
 if(!(Test-Path -LiteralPath $configPath)){return}
 $powershell=Join-Path $env:SystemRoot 'System32/WindowsPowerShell/v1.0/powershell.exe'
 $supervisor=Join-Path $PSScriptRoot 'codex-notify-supervisor.ps1'
 Start-Process -FilePath $powershell -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-File',('"'+$supervisor+'"')) -WindowStyle Hidden|Out-Null
}

if ($Watch) {
    Watch-CodexApprovals
    exit 0
}

# Codex Desktop may restart without preserving the long-running approval
# watcher. Every normal turn-ended hook quietly ensures that one watcher is
# alive; the named mutex makes duplicate processes exit immediately.
Ensure-ApprovalWatcher

# Preserve the hook already installed by Codex Computer Use.
if (Test-Path $originalHook) {
    & $originalHook 'turn-ended' @HookArguments
}
# Completion delivery is owned by the single durable-event watcher, not both hook and watcher.
