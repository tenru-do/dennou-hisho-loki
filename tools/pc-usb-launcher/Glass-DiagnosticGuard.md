# Rokid Wi-Fi diagnostic storage guard

The verified device firmware starts `vendor.cnss_diag` with file logging enabled. Controlled testing showed about 0.6–0.7 MB/s of system UID writes during network use; stopping this diagnostic service eliminated that growth while Loki and Wi-Fi remained enabled. Existing private diagnostic files cannot be enumerated or removed with the stock shell.

`Start-Loki-USB.ps1` calls this bounded guard before the normal phone connection flow. Only a unique online `model:RG_glasses` device is eligible. A missing/ambiguous device or unsupported state is skipped. Only a running diagnostic service is stopped via the normal init control property, with an explicit stopped-state check. No Wi-Fi/Bluetooth state, photos, recordings, user settings or app data are changed. Failures do not prevent the normal phone connection flow. No scheduled task or background PC watcher is installed.

This guard is applied on PC launcher execution only. It does not permanently disable the manufacturer service across a standalone glasses reboot, and does not clear accumulated protected Wi-Fi logs. These remain firmware/vendor repair limitations. Do not lower Android storage thresholds, root the device or erase user data to hide them.

Run `Test-GlassDiagnosticGuard.ps1` in Windows PowerShell 5.1 for wrong-model, successful-stop, already-stopped, unsupported-state and ambiguous-device cases. Existing launcher runtime tests remain applicable.
