param(
    [switch]$Watch,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$HookArguments
)

$ErrorActionPreference = 'SilentlyContinue'
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$configPath = Join-Path $scriptDir 'token-codex-notify.local.json'
$logPath = Join-Path $scriptDir 'codex-notify.log'
$adb = 'C:\Users\user\Documents\Rokid_Zoom_Camera\android-sdk\platform-tools\adb.exe'
$phoneSerial = 'RFGL343HCYX'
$forwardPort = 18765
$originalHook = 'C:\Users\user\AppData\Local\OpenAI\Codex\runtimes\cua_node\cd454f7c85348168\bin\node_modules\@oai\sky\bin\windows\codex-computer-use.exe'

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
            $result = Invoke-RestMethod -Method Post -Uri $target -Headers $headers -ContentType $contentType -Body $body -TimeoutSec 4
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
            $result = Invoke-RestMethod -Method Post -Uri $target -Headers $headers -ContentType $contentType -Body $body -TimeoutSec 3
            if ($result.ok) {
                Write-NotifyLog "sent $target $message"
                return $true
            }
        } catch {
            Write-NotifyLog "failed $target $($_.Exception.Message)"
        }
    }
    if (Test-Path $adb) {
        & $adb -s $phoneSerial forward "tcp:$forwardPort" 'tcp:8765' | Out-Null
        $target = "http://127.0.0.1:$forwardPort/command"
        try {
            $result = Invoke-RestMethod -Method Post -Uri $target -Headers $headers -ContentType $contentType -Body $body -TimeoutSec 4
            if ($result.ok) {
                Write-NotifyLog "sent $target $message"
                return $true
            }
        } catch {
            Write-NotifyLog "failed $target $($_.Exception.Message)"
        }
    }
    return $false
}

function Get-HookMessage([string[]]$arguments) {
    $cfg = Get-Content -Raw -Encoding UTF8 $configPath | ConvertFrom-Json
    $completeText = if ($cfg.completeMessage) { [string]$cfg.completeMessage } else { 'Codex task completed.' }
    $jsonText = ($arguments | Where-Object { $_ -and $_.Trim().StartsWith('{') } | Select-Object -Last 1)
    if (!$jsonText) { return $completeText }
    try {
        $event = $jsonText | ConvertFrom-Json
        $last = [string]$event.PSObject.Properties['last-assistant-message'].Value
        if (!$last) { $last = [string]$event.last_agent_message }
        $last = ($last -replace '\s+', ' ').Trim()
        if ($last.Length -gt 110) { $last = $last.Substring(0, 110) + '...' }
        if ($last) { return $completeText + ' ' + $last }
    } catch {}
    return $completeText
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
                foreach ($line in ($newText -split "`r?`n")) {
                    if ([string]::IsNullOrWhiteSpace($line)) { continue }
                    try {
                        $record = $line | ConvertFrom-Json
                        # Completion is sent only by the turn-ended hook below. Watching
                        # task_complete here as well caused duplicate and early notices.
                        if ($record.type -eq 'event_msg' -and $record.payload.type -eq 'task_complete') { continue }
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
                if ($needsAttention) {
                    $cfg = Get-Content -Raw -Encoding UTF8 $configPath | ConvertFrom-Json
                    $permissionText = if ($cfg.permissionMessage) { [string]$cfg.permissionMessage } else { 'Permission needed. Check Codex on the PC.' }
                    Send-GlassNotification $permissionText | Out-Null
                }
            }
        }
        Start-Sleep -Seconds 2
    }
}

if ($Watch) {
    Watch-CodexApprovals
    exit 0
}

# Preserve the hook already installed by Codex Computer Use.
if (Test-Path $originalHook) {
    & $originalHook 'turn-ended' @HookArguments
}
Send-GlassNotification (Get-HookMessage $HookArguments) | Out-Null
