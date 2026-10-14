param(
    [switch]$Watch,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$HookArguments
)
. (Join-Path $PSScriptRoot 'secure-bridge.ps1')
. (Join-Path $PSScriptRoot 'codex-notify-policy.ps1')


$ErrorActionPreference = 'SilentlyContinue'
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$configPath = Join-Path $scriptDir 'token-codex-notify.local.json'
$logPath = Join-Path $scriptDir 'codex-notify.log'
$outboxPath = Join-Path $scriptDir 'codex-notify-outbox.local.json'
$script:notifyOutbox = @()
if(Test-Path $outboxPath){try{$script:notifyOutbox=@(Get-Content $outboxPath -Raw -Encoding UTF8|ConvertFrom-Json)}catch{}}
function Save-NotifyOutbox {
    $temporary=$outboxPath+'.tmp'
    [IO.File]::WriteAllText($temporary,(ConvertTo-Json -InputObject @($script:notifyOutbox) -Depth 8),[Text.UTF8Encoding]::new($true))
    Move-Item -LiteralPath $temporary -Destination $outboxPath -Force
}
function Queue-NotifyEnvelope([string]$message){
    if(!$message){return}
    try{$data=$message|ConvertFrom-Json;if(@($script:notifyOutbox|Where-Object {$_.id -eq $data.id}).Count){return}
        $script:notifyOutbox+=@{id=$data.id;issuedAt=$data.issuedAt;message=$message;nextAttempt=0L};Save-NotifyOutbox
    }catch{}
}
function Flush-NotifyOutbox {
    $now=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds();$keep=@()
    foreach($item in $script:notifyOutbox){
        if($now-[long]$item.issuedAt -gt 300000 -or [long]$item.issuedAt-$now -gt 30000){continue}
        if([long]$item.nextAttempt -gt $now){$keep+=$item;continue}
        if(!(Send-GlassNotification ([string]$item.message))){$item.nextAttempt=$now+15000;$keep+=$item}
    }
    $script:notifyOutbox=$keep;Save-NotifyOutbox
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

function Find-RokiPhoneHosts {
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
                Write-NotifyLog "sent $target $message"
                return $true
            }
        } catch {
            Write-NotifyLog "failed $target $($_.Exception.Message)"
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
                    Write-NotifyLog "updated phone host $discoveredHost"
                }
                Write-NotifyLog "sent $target $message"
                return $true
            }
        } catch {
            Write-NotifyLog "failed $target $($_.Exception.Message)"
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
                    Write-NotifyLog "sent adb:$serial $message"
                    return $true
                }
            } catch {
                Write-NotifyLog "failed adb:$serial $($_.Exception.Message)"
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
    $createdNew = $false
    $watcherMutex = New-Object Threading.Mutex($true, 'Local\RokiCodexNotifyWatcher', [ref]$createdNew)
    if (!$createdNew) { return }
    $sessionRoot = Join-Path $env:USERPROFILE '.codex\sessions'
    $offsets = @{}
    Get-ChildItem $sessionRoot -Recurse -Filter '*.jsonl' | ForEach-Object {
        $offsets[$_.FullName] = $_.Length
    }
    Write-NotifyLog 'approval watcher started'
    while ($true) {
        Get-ChildItem $sessionRoot -Recurse -Filter '*.jsonl' | ForEach-Object {
            $path = $_.FullName
            $roots=@((Split-Path $scriptDir -Parent));try{$localConfig=Get-Content -Raw -Encoding UTF8 $configPath|ConvertFrom-Json;$roots+=@($localConfig.projectRoots|Where-Object {$_})}catch{}
            if(!(Test-LokiProjectSession $path $roots)){return}
            if (!$offsets.ContainsKey($path)) { $offsets[$path] = 0L }
            if ($_.Length -lt $offsets[$path]) { $offsets[$path] = 0L }
            if ($_.Length -gt $offsets[$path]) {
                $stream = [IO.File]::Open($path, 'Open', 'Read', 'ReadWrite')
                $stream.Seek([long]$offsets[$path], 'Begin') | Out-Null
                $reader = New-Object IO.StreamReader($stream, [Text.Encoding]::UTF8)
                $newText = $reader.ReadToEnd()
                $offsets[$path] = $stream.Length
                $reader.Dispose(); $stream.Dispose()
                $needsAttention = $false
                $completionMessage = ''
                $questionMessages = @()
                foreach ($line in ($newText -split "`r?`n")) {
                    if ([string]::IsNullOrWhiteSpace($line)) { continue }
                    try {
                        $record = $line | ConvertFrom-Json
                        $questionMessages += @(New-LokiQuestionEnvelopes $record $path)
                        # Desktop tasks do not always invoke the user-level notify hook.
                        # Watch the durable task_complete event as a fallback.  The hook
                        # formats the same text, so the phone's 60-second deduplicator
                        # suppresses a second delivery when both mechanisms do fire.
                        if ($record.type -eq 'event_msg' -and $record.payload.type -eq 'task_complete') {
                            $completionMessage = New-LokiCompletionEnvelope ([string]$record.payload.last_agent_message) $path ([DateTimeOffset]::Parse([string]$record.timestamp))
                            continue
                        }
                        if ($record.type -ne 'response_item' -or $record.payload.type -ne 'custom_tool_call') { continue }
                        $toolName = [string]$record.payload.name
                        $toolInput = [string]$record.payload.input
                        if ($toolName -eq 'request_permissions' -or $toolName -eq 'request_user_input' -or
                                ($toolName -eq 'exec' -and ($toolInput -match 'tools\.request_permissions\s*\(' -or
                                $toolInput -match 'tools\.request_user_input\s*\('))) {
                            $needsAttention = $true
                            break
                        }
                    } catch {}
                }
                # Approval/tool activity is not a completed user task; never read it aloud.
                if ($completionMessage) {
                    Queue-NotifyEnvelope $completionMessage
                }
                foreach($questionMessage in $questionMessages){
                    Queue-NotifyEnvelope $questionMessage
                }
            }
        }
        Flush-NotifyOutbox
        Start-Sleep -Seconds 2
    }
}

function Ensure-ApprovalWatcher {
    if (!(Test-Path $configPath)) { return }
    $powershell = Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0\powershell.exe'
    $arguments = @(
        '-NoProfile',
        '-ExecutionPolicy', 'Bypass',
        '-File', ('"' + $PSCommandPath + '"'),
        '-Watch'
    )
    Start-Process -FilePath $powershell -ArgumentList $arguments -WindowStyle Hidden | Out-Null
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
