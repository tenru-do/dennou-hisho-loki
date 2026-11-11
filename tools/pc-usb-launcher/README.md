# Windows USB launcher

Windows PowerShell 5.1 is the actual runtime. Save every `.ps1` as UTF-8 with BOM. Build with `powershell.exe -NoProfile -ExecutionPolicy Bypass -File Build-LokiUsbLauncher.ps1`; it checks script encoding, the 5.1 parser and malformed-encoding fixtures before replacing the executable. Use `-VerifyLaunch` after local configuration to test the actual executable. Pin the generated EXE or its shortcut using the Windows context menu.

Copy `launcher.example.json` to `launcher.local.json` and configure your own smartphone serial. Configure `adb`/`node` only if their defaults are unavailable. The example contains no real device identifier. Obtain the phone public certificate through the existing trusted pairing procedure, verify it independently, and save it as `phone-public-certificate.local.der`. Do not download an arbitrary certificate or disable global browser certificate validation. The connection uses only the verified phone public key in a separate browser profile. Issue a Web invitation and approve the connecting PC on the phone if needed.

Local configuration, the device certificate and browser profile are excluded from Git. The repository does not include private user configuration, keys, phone certificates or compiled launchers. The portable template is source/build checked; the installed user-specific launcher has separately passed actual launch and Web response checks. This template does not configure ADB or approve Android permissions on your behalf.

ADB discovery captures native stderr under Windows PowerShell 5.1. A missing configured device triggers bounded start-server / reconnect-offline attempts, followed by at most one server reset. An unauthorized device requests approval without resetting ADB. A healthy connection is never reset; no other device is selected automatically. Physical disconnection still requires a cable/device check. Usb-AdbRecovery.ps1 must be distributed alongside Start-Loki-USB.ps1; both use UTF-8 BOM.


## 2026-11-07 recovery update

Recovery uses a 30-second monotonic overall discovery deadline, bounded process and stream reads, explicit UTF-8, exit-code-aware device matching and one shared server-reset allowance. A missing device gets startup/reconnect grace periods before one reset and polling. A healthy connection returns immediately; unauthorized devices require the existing Android consent. Late forwarding/start transport errors share the same reset allowance. Optional UI discovery timeouts do not reset a healthy server. The script itself suppresses overlapping launches and validates the recovery helper encoding.

The actual runtime suite covers delayed post-reset enumeration, offline/unauthorized/missing or wrong devices, nonzero results with misleading stdout, native argument/encoding/pipe handling and shared restart limits. Script concurrency fixtures use only the existing Windows PowerShell host. `verify-usb.cjs --check-web` verifies the saved certificate before each request and checks actual HTTP body bytes and markers for both public Web resources.

For an environment that permits the existing executable but blocks a new native build, script-only updates can use `Build-LokiUsbLauncher.ps1 -VerifyLaunch -KeepExistingExecutable`. This preserves the existing EXE, source, icon and pinned path and still runs the complete script and real-launch verification. The normal compile path keeps an old binary backup and restores it if verification fails. No Windows security policy is changed.
