param([switch]$TestRoundTrip)
$ErrorActionPreference = 'Stop'
$base = 'http://127.0.0.1:18765'
$token = ''
$until = (Get-Date).AddMinutes(3)
while (!$token -and (Get-Date) -lt $until) {
    try { $pair = Invoke-RestMethod "$base/pc_pair" -TimeoutSec 3; $token = $pair.token } catch { }
    if (!$token) { Start-Sleep -Seconds 2 }
}
if (!$token) { throw 'Pairing window not available; no data changed.' }
$headers = @{'X-Roki-Token'=$token}
# Request the glasses to refresh their state, then inspect metadata only.
$first = Invoke-RestMethod "$base/custom_snapshot" -Headers $headers -TimeoutSec 5
Start-Sleep -Seconds 12
$snapshot = Invoke-RestMethod "$base/custom_snapshot" -Headers $headers -TimeoutSec 5
[pscustomobject]@{
    currentChars = $snapshot.custom.Length
    pending = $snapshot.pending
    stableDuringRead = ($first.custom -ceq $snapshot.custom)
    glassAudit = $snapshot.glassAudit
} | ConvertTo-Json -Depth 5
if ($TestRoundTrip) {
    if ($snapshot.pending) { throw 'Existing edit pending; test not started.' }
    $original = [string]$snapshot.custom
    $test = $original + "`n" + '# sync-check-' + [guid]::NewGuid().ToString('N')
    function Save-Custom([string]$expected, [string]$replacement) {
        $json = @{base=$expected; custom=$replacement} | ConvertTo-Json -Compress
        $null = Invoke-RestMethod "$base/custom_edit" -Method Post -Headers $headers `
            -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($json)) -TimeoutSec 5
    }
    function Wait-Custom([string]$expected) {
        $deadline = (Get-Date).AddSeconds(60)
        do {
            Start-Sleep -Seconds 2
            $s = Invoke-RestMethod "$base/custom_snapshot" -Headers $headers -TimeoutSec 5
            if ($s.custom -cne $expected) { throw 'Concurrent change detected; refusing to overwrite.' }
            if (!$s.pending) { return $true }
        } while ((Get-Date) -lt $deadline)
        return $false
    }
    try {
        Save-Custom $original $test
        $ack = Wait-Custom $test
        Write-Output "Changed-content glass acknowledgement: $ack"
    } finally {
        $now = Invoke-RestMethod "$base/custom_snapshot" -Headers $headers -TimeoutSec 5
        if ($now.custom -ceq $test) {
            Save-Custom $test $original
            $restored = Wait-Custom $original
            Write-Output "Original content restored and acknowledged: $restored"
        } elseif ($now.custom -ceq $original) {
            Write-Output 'Original content already present.'
        } else {
            throw 'Concurrent edit preserved; manual review needed for temporary sync-check line.'
        }
    }
}
$token = $null
$headers = $null
$first = $null
$snapshot = $null
