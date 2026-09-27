$ErrorActionPreference='Stop'
$adb=Join-Path $env:USERPROFILE 'Documents\Rokid_Zoom_Camera\android-sdk\platform-tools\adb.exe'
function Tap-Text([string]$label) {
    & $adb -s RFGL343HCYX shell uiautomator dump /sdcard/loki-ui.xml | Out-Null
    [xml]$doc=(& $adb -s RFGL343HCYX shell cat /sdcard/loki-ui.xml) -join "`n"
    $n=$doc.SelectNodes('//node') | Where-Object {$_.text -ceq $label} | Select-Object -First 1
    if (!$n) { return $false }
    $b=[regex]::Matches($n.bounds,'\d+') | ForEach-Object {[int]$_.Value}
    & $adb -s RFGL343HCYX shell input tap ([int](($b[0]+$b[2])/2)) ([int](($b[1]+$b[3])/2))
    Start-Sleep -Milliseconds 400
    return $true
}
# Match live UI labels; do not unlock the device or retain credentials.
$operations=[string][char]0x64cd+[char]0x4f5c
$pair='PC'+[char]0x63a5+[char]0x7d9a+[char]0xff08+'60'+[char]0x79d2+[char]0x9593+[char]0xff09
if (!(Tap-Text $pair)) {
    if (!(Tap-Text $operations) -or !(Tap-Text $pair)) { throw 'Unlock phone and open Loki Operations.' }
}
& $adb -s RFGL343HCYX forward tcp:18765 tcp:8765 | Out-Null
& "$PSScriptRoot/check-imported-live.ps1" -SendTest
