# Stop only the confirmed excessive Wi-Fi diagnostic writer on connected Rokid glasses.
# No app data, photos, recordings, Bluetooth or Wi-Fi state is changed.
function Stop-LokiExcessiveGlassDiagnostics {
 param([string]$Adb)
 $devices=Invoke-LokiAdb -Adb $Adb -Command @('devices','-l') -TimeoutMs 1500
 if($devices.Code -ne 0){return 'unavailable'}
 $matchesFound=@([regex]::Matches($devices.Output,'(?m)^([A-Za-z0-9._:-]+)\s+device\s+[^\r\n]*model:RG_glasses(?:\s|$)'))
 if($matchesFound.Count -ne 1){return 'no_unique_glasses'}
 $glassSerial=$matchesFound[0].Groups[1].Value
 $state=Invoke-LokiAdb -Adb $Adb -Command @('-s',$glassSerial,'shell','getprop','init.svc.vendor.cnss_diag') -TimeoutMs 1500
 if($state.Code -ne 0){return 'unavailable'}
 if($state.Output.Trim() -eq 'stopped'){return 'already_stopped'}
 if($state.Output.Trim() -ne 'running'){return 'unsupported'}
 $stop=Invoke-LokiAdb -Adb $Adb -Command @('-s',$glassSerial,'shell','setprop','ctl.stop','vendor.cnss_diag') -TimeoutMs 1500
 if($stop.Code -ne 0){return 'stop_failed'}
 $check=Invoke-LokiAdb -Adb $Adb -Command @('-s',$glassSerial,'shell','getprop','init.svc.vendor.cnss_diag') -TimeoutMs 1500
 if($check.Code -eq 0 -and $check.Output.Trim() -eq 'stopped'){return 'stopped'}
 return 'stop_not_confirmed'
}
