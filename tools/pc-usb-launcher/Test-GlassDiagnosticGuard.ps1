$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'Glass-DiagnosticGuard.ps1')
$script:calls=@();$script:states=@()
function Invoke-LokiAdb {param($Adb,$Command,$TimeoutMs) $script:calls+=,($Command -join ' '); if($Command[0] -eq 'devices'){return @{Code=0;Output=$script:list}}; $answer=$script:states[0];if($script:states.Count -gt 1){$script:states=$script:states[1..($script:states.Count-1)]};return @{Code=0;Output=$answer}}
$script:list="phone device product:m1 model:SM_S942Q`n";if((Stop-LokiExcessiveGlassDiagnostics adb) -ne 'no_unique_glasses' -or $script:calls.Count -ne 1){throw 'Wrong device safety'}
$script:list="glass device product:glasses model:RG_glasses device:glasses`n";$script:states=@('running','','stopped');$script:calls=@();if((Stop-LokiExcessiveGlassDiagnostics adb) -ne 'stopped' -or $script:calls.Count -ne 4 -or $script:calls[2] -ne '-s glass shell setprop ctl.stop vendor.cnss_diag'){throw 'Stop sequence'}
$script:states=@('stopped');$script:calls=@();if((Stop-LokiExcessiveGlassDiagnostics adb) -ne 'already_stopped' -or $script:calls.Count -ne 2){throw 'Idempotency'}
$script:states=@('');$script:calls=@();if((Stop-LokiExcessiveGlassDiagnostics adb) -ne 'unsupported' -or $script:calls.Count -ne 2){throw 'Unsupported safety'}
$script:list+=$script:list;$script:calls=@();if((Stop-LokiExcessiveGlassDiagnostics adb) -ne 'no_unique_glasses' -or $script:calls.Count -ne 1){throw 'Ambiguity safety'}
Write-Output 'PASS 5 diagnostic guard cases'
