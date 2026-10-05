param([string]$Adb = '', [switch]$SmokeTest)
if (!$Adb) {
    $adbCommand = Get-Command adb.exe -ErrorAction SilentlyContinue
    if ($adbCommand) { $Adb = $adbCommand.Source }
    else {
        foreach ($candidate in @(
            (Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'),
            (Join-Path $env:USERPROFILE 'Documents\Rokid_Zoom_Camera\android-sdk\platform-tools\adb.exe'))) {
            if (Test-Path -LiteralPath $candidate) { $Adb = $candidate; break }
        }
    }
}
Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
[System.Windows.Forms.Application]::EnableVisualStyles()
$script:bridge = 'http://127.0.0.1:18765'
$script:token = ''
$script:serial = ''
$script:seen = New-Object 'System.Collections.Generic.HashSet[string]'
function Request-Loki([string]$Path, $Body = $null) {
    if (!$script:token) { throw '先に接続してください' }
    $request = [System.Net.HttpWebRequest]::Create($script:bridge + $Path)
    $request.Timeout = 2500
    $request.ReadWriteTimeout = 2500
    $request.Headers.Add('X-Roki-Token', $script:token)
    if ($null -ne $Body) {
        $request.Method = 'POST'
        $request.ContentType = 'application/json; charset=utf-8'
        $bytes = [Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Compress))
        $request.ContentLength = $bytes.Length
        $stream = $request.GetRequestStream()
        $stream.Write($bytes,0,$bytes.Length)
        $stream.Dispose()
    }
    $response = $request.GetResponse()
    $reader = New-Object IO.StreamReader($response.GetResponseStream(),[Text.Encoding]::UTF8)
    try { return ($reader.ReadToEnd() | ConvertFrom-Json) }
    finally { $reader.Dispose(); $response.Dispose() }
}
$form = New-Object Windows.Forms.Form
$form.Text = '電脳秘書ロキ — PC会話'
$form.Size = New-Object Drawing.Size(840,720)
$form.StartPosition = 'CenterScreen'
$form.Font = New-Object Drawing.Font('Yu Gothic UI',11)
$panel = New-Object Windows.Forms.FlowLayoutPanel
$panel.Dock = 'Top'; $panel.Height = 46
$connect = New-Object Windows.Forms.Button
$connect.Text = 'USB接続'; $connect.AutoSize = $true
$settings = New-Object Windows.Forms.Button
$settings.Text = 'Wi-Fi接続'; $settings.AutoSize = $true
$custom = New-Object Windows.Forms.Button
$custom.Text = 'カスタム指示'; $custom.AutoSize = $true
$panel.Controls.AddRange(@($connect,$settings,$custom))
$status = New-Object Windows.Forms.Label
$status.Dock = 'Bottom'; $status.Height = 30; $status.Text = 'スマホとグラスでロキを起動して接続してください。'
$history = New-Object Windows.Forms.RichTextBox
$history.Dock = 'Fill'; $history.ReadOnly = $true; $history.BackColor = [Drawing.Color]::White
$entryPanel = New-Object Windows.Forms.Panel
$entryPanel.Dock = 'Bottom'; $entryPanel.Height = 125
$entry = New-Object Windows.Forms.TextBox
$entry.Multiline = $true; $entry.Dock = 'Fill'; $entry.MaxLength = 3000; $entry.ScrollBars = 'Vertical'
$send = New-Object Windows.Forms.Button
$send.Text = '送信'; $send.Dock = 'Right'; $send.Width = 85
$entryPanel.Controls.Add($entry); $entryPanel.Controls.Add($send)
$form.Controls.Add($history); $form.Controls.Add($entryPanel); $form.Controls.Add($status); $form.Controls.Add($panel)
$connect.Add_Click({
    try {
        $devices = & $Adb devices
        $found = $false
        foreach ($line in $devices) {
            if ($line -notmatch '^(\S+)\s+device$') { continue }
            $serial = $Matches[1]
            $package = & $Adb -s $serial shell pm path com.example.rokidgeminisecretary 2>$null
            if (!$package -or $package -notmatch 'package:') { continue }
            & $Adb -s $serial forward tcp:18765 tcp:8765 | Out-Null
            $paired = Invoke-RestMethod -Uri 'http://127.0.0.1:18765/pc_pair' -TimeoutSec 3
            $token = $paired.token
            if ($token.Length -lt 16) { throw 'スマホの「PC接続」を押してから再度USB接続してください' }
            $script:serial = $serial; $script:token = $token
            $script:bridge = 'http://127.0.0.1:18765'; $found = $true
            break
        }
        if (!$found) { throw 'USBデバッグを許可したロキのスマホが見つかりません' }
        $null = Request-Loki '/custom_snapshot'
        $status.Text = 'USB接続済み。回答は同じロキから届きます。'
    } catch { $status.Text = 'スマホの操作欄で「PC接続」を押してから、USB接続を押してください。' }
})
$settings.Add_Click({
    $dialog = New-Object Windows.Forms.Form
    $dialog.Text = '同じWi-Fiのスマホへ接続'; $dialog.Size = New-Object Drawing.Size(540,215)
    $layout = New-Object Windows.Forms.FlowLayoutPanel; $layout.Dock = 'Fill'; $layout.FlowDirection = 'TopDown'
    $address = New-Object Windows.Forms.TextBox; $address.Width = 480; $address.Text = 'http://192.168.0.2:8765'
    $key = New-Object Windows.Forms.TextBox; $key.Width = 480; $key.UseSystemPasswordChar = $true
    $hint = New-Object Windows.Forms.Label; $hint.AutoSize = $true; $hint.Text = 'スマホの接続先URLと連携トークン（このPCには保存しません）'
    $ok = New-Object Windows.Forms.Button; $ok.Text = '接続'
    $ok.Add_Click({
        try {
            $uri = [uri]$address.Text
            if ($uri.Scheme -notin @('http','https') -or $uri.UserInfo) { throw 'URLが不正です' }
            $script:bridge = $uri.AbsoluteUri.TrimEnd('/'); $script:token = $key.Text.Trim()
            $null = Request-Loki '/custom_snapshot'; $status.Text = 'Wi-Fi接続済み'; $dialog.Close()
        } catch { $hint.Text = $_.Exception.Message }
    })
    $layout.Controls.AddRange(@($hint,$address,$key,$ok)); $dialog.Controls.Add($layout)
    $null = $dialog.ShowDialog($form)
})
$send.Add_Click({
    try {
        $value = $entry.Text.Trim(); if (!$value) { return }
        $result = Request-Loki '/command' @{command=$value}
        if (!$result.queued) { throw '送信できませんでした' }
        $entry.Clear(); $status.Text = '送信済み。待機中の入力も順に受け付けます。'
    } catch { $status.Text = $_.Exception.Message }
})
$entry.Add_KeyDown({ if ($_.Control -and $_.KeyCode -eq 'Enter') { $send.PerformClick(); $_.SuppressKeyPress = $true } })
$custom.Add_Click({
    try {
        $snapshot = Request-Loki '/custom_snapshot'
        $dialog = New-Object Windows.Forms.Form; $dialog.Text = 'ロキのカスタム指示'; $dialog.Size = New-Object Drawing.Size(760,620)
        $editor = New-Object Windows.Forms.TextBox; $editor.Multiline = $true; $editor.ScrollBars = 'Vertical'; $editor.Dock = 'Fill'; $editor.MaxLength = 12000; $editor.Text = $snapshot.custom
        $save = New-Object Windows.Forms.Button; $save.Dock = 'Bottom'; $save.Height = 42; $save.Text = 'スマホとグラスへ反映'
        $save.Add_Click({
            try { $null = Request-Loki '/custom_edit' @{custom=$editor.Text; base=$snapshot.custom}; $dialog.Close(); $status.Text = '指示を保存しました。グラスの同期を待っています。' }
            catch { [Windows.Forms.MessageBox]::Show($_.Exception.Message) | Out-Null }
        })
        $dialog.Controls.Add($editor); $dialog.Controls.Add($save); $null = $dialog.ShowDialog($form)
    } catch { $status.Text = $_.Exception.Message }
})
$timer = New-Object Windows.Forms.Timer; $timer.Interval = 4000
$timer.Add_Tick({
    if (!$script:token) { return }
    try {
        $memory = Request-Loki '/desktop_state'
        $status.Text = "$($memory.state) $($memory.message) " + $(if ($memory.waitMs -gt 0) { "あと $([math]::Ceiling($memory.waitMs / 1000)) 秒" } else { '' })
        foreach ($item in $memory.entries) {
            $id = "$($item.time)|$($item.kind)|$($item.message)"
            if ($script:seen.Add($id)) {
                $history.AppendText("$($item.kind): $($item.message)`r`n`r`n")
                $history.SelectionStart = $history.TextLength; $history.ScrollToCaret()
            }
        }
    } catch { $status.Text = '接続待ち。スマホとグラスの起動・通信を確認してください。' }
})
$form.Add_FormClosed({ $timer.Stop(); $script:token = ''; if ($script:serial) { & $Adb -s $script:serial forward --remove tcp:18765 | Out-Null } })
if ($SmokeTest) {
    Write-Output "PC form created: $($form.Controls.Count) root controls"
    $timer.Dispose(); $form.Dispose(); return
}
$timer.Start()
$null = $form.ShowDialog()
