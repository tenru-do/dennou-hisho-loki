param([switch]$SendTest)
$ErrorActionPreference='Stop'
$base='http://127.0.0.1:18765'
$pair=Invoke-RestMethod "$base/pc_pair" -TimeoutSec 3
$headers=@{'X-Roki-Token'=$pair.token}
try {
    foreach ($query in @('Rokid Glasses', ([string][char]0x30c6+[char]0x30b9+[char]0x30c8), 'unrelated-weather-forecast')) {
        $result=Invoke-RestMethod ($base+'/imported_memory?q='+[uri]::EscapeDataString($query)) -Headers $headers -TimeoutSec 5
        [pscustomobject]@{query=$query; count=@($result.entries).Count; chars=(@($result.entries | ForEach-Object {$_.summary.Length}) | Measure-Object -Sum).Sum} | ConvertTo-Json -Compress
    }
    if ($SendTest) {
        $since=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
        $word=[string][char]0x30c6+[char]0x30b9+[char]0x30c8
        $body=[Text.Encoding]::UTF8.GetBytes((@{command=$word} | ConvertTo-Json -Compress))
        $sent=Invoke-RestMethod "$base/command" -Method Post -Headers $headers -ContentType 'application/json; charset=utf-8' -Body $body -TimeoutSec 5
        Write-Output ('Queued: '+$sent.queued)
        $deadline=(Get-Date).AddSeconds(75)
        do {
            Start-Sleep -Seconds 5
            $state=Invoke-RestMethod "$base/desktop_state" -Headers $headers -TimeoutSec 5
            $new=@($state.entries | Where-Object {$_.time -ge $since})
            if ($new.Count -ge 2) { break }
        } while ((Get-Date) -lt $deadline)
        $new | ForEach-Object { [pscustomobject]@{kind=$_.kind; chars=$_.message.Length} } | ConvertTo-Json -Compress
        Write-Output ('RuntimeState: '+$state.state)
    }
} finally { $headers=$null; $pair=$null }
