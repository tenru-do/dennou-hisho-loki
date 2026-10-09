param([string]$JavaDirectory='C:/Program Files/Android/Android Studio/jbr/bin')
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$out=Join-Path $root '.build-security-tests'
New-Item -ItemType Directory -Force $out | Out-Null
$fixture=Join-Path $out 'fixture.p12'
if((Test-Path -LiteralPath $fixture) -and (Get-Item -LiteralPath $fixture).LastWriteTimeUtc -lt [DateTime]::UtcNow.AddDays(-1)){Remove-Item -LiteralPath $fixture}
if(!(Test-Path -LiteralPath $fixture)){
    & "$JavaDirectory/keytool.exe" '-J-Duser.language=en' -genkeypair -alias fixture -keystore $fixture -storetype PKCS12 -keyalg EC -groupname secp256r1 -dname 'CN=localhost' -validity 2 -storepass fixture-local-test -keypass fixture-local-test -noprompt
    if($LASTEXITCODE){throw 'Fixture certificate creation failed'}
}
$src=Join-Path $root 'glass-app/src/com/example/rokidkeyboardbridge'
& "$JavaDirectory/javac.exe" '-J-Duser.language=en' -encoding UTF8 -d "$out/classes" "$src/BridgeCertificatePolicy.java" "$src/PhotoVaultCipher.java" "$src/PhotoArchive.java" "$src/PhotoRecorderCommands.java" "$root/tools/BridgeSecurityTest.java" "$root/tools/PhotoArchiveTest.java" "$root/phone-app/src/com/example/rokidgeminisecretary/PairingWindow.java" "$root/tools/PairingWindowTest.java"
if($LASTEXITCODE){throw 'Security tests compilation failed'}
& "$JavaDirectory/java.exe" "-Djava.io.tmpdir=$out" -cp "$out/classes" com.example.rokidkeyboardbridge.BridgeSecurityTest $fixture fixture-local-test
if($LASTEXITCODE){throw 'TLS and vault tests failed'}
& "$JavaDirectory/java.exe" "-Djava.io.tmpdir=$out" -cp "$out/classes" com.example.rokidkeyboardbridge.PhotoArchiveTest
if($LASTEXITCODE){throw 'Archive regression failed'}
& "$JavaDirectory/java.exe" -cp "$out/classes" com.example.rokidgeminisecretary.PairingWindowTest
if($LASTEXITCODE){throw 'Pairing window tests failed'}
. "$PSScriptRoot/secure-bridge.ps1"
$tokens=$null;$errors=$null
Get-ChildItem $PSScriptRoot -Filter '*.ps1' | ForEach-Object {[System.Management.Automation.Language.Parser]::ParseFile($_.FullName,[ref]$tokens,[ref]$errors)|Out-Null;if($errors.Count){throw "Script syntax failed: $($_.Name)"}}
$failed=$false
try{& "$PSScriptRoot/build-integration.ps1" -App glass -Release}catch{$failed=$_.Exception.Message -match 'Release requires an explicit keystore'}
if(!$failed){throw 'Release credential guard failed'}
$failed=$false
try{& "$PSScriptRoot/build-integration.ps1" -App glass -Release -Development}catch{$failed=$_.Exception.Message -match 'Release cannot be combined with Development'}
if(!$failed){throw 'Release/development conflict guard failed'}
Write-Output 'PASS script syntax, missing release credentials and conflicting release/development rejected'
# Local interoperability fixture, no external network or installed applications.
$server=$null
try{
    $stdout=Join-Path $out 'tls-server.txt';$stderr=Join-Path $out 'tls-server-errors.txt'
    $launch=@('-cp',('"'+$out+'/classes"'),'com.example.rokidkeyboardbridge.BridgeSecurityTest',('"'+$fixture+'"'),'fixture-local-test','serve')
    $server=Start-Process -FilePath "$JavaDirectory/java.exe" -ArgumentList $launch -WindowStyle Hidden -PassThru -RedirectStandardOutput $stdout -RedirectStandardError $stderr
    $until=[DateTime]::UtcNow.AddSeconds(15)
    while([DateTime]::UtcNow -lt $until){if($server.HasExited){throw 'Local TLS fixture failed to start'};if((Get-Content $stdout -Raw -ErrorAction SilentlyContinue) -match 'READY'){break};Start-Sleep -Milliseconds 100}
    if(!((Get-Content $stdout -Raw) -match 'READY')){throw 'Local TLS fixture not ready'}
    $headers=@{'X-Roki-Token'='fixture-credential-not-a-real-secret'}
    $result=Invoke-LokiSecureRest -Uri 'http://127.0.0.1:8765/test' -Headers $headers
    if(!$result.ok){throw 'Encrypted PC request failed'}
    $failed=$false;try{New-LokiSecureRequest 'http://127.0.0.1:8765/test' 'wrong-fixture-token' | Out-Null}catch{$failed=$true}
    if(!$failed){throw 'False certificate proof accepted'}
    $pin=(@($script:LokiTlsPins.Values))[0]
    $wrong=if($pin.StartsWith('0')){'1'+$pin.Substring(1)}else{'0'+$pin.Substring(1)}
    $bad=New-LokiSecureRequest 'http://127.0.0.1:8765/test' '' $wrong;$bad.Headers.Add('X-Roki-Token',$headers['X-Roki-Token']);$bad.Timeout=4000
    $failed=$false;try{$bad.GetResponse().Dispose()}catch{$failed=$true}
    if(!$failed){throw 'False certificate accepted'}
    if(!(Invoke-LokiSecureRest -Uri 'http://127.0.0.1:8765/test' -Headers $headers).ok){throw 'Second encrypted request failed'}
    if(!$server.WaitForExit(5000)){throw 'Local TLS fixture did not finish'};$server.Refresh()
    if(($null -ne $server.ExitCode -and $server.ExitCode -ne 0) -or (Get-Content $stdout -Raw) -notmatch 'PASS local PC TLS fixture finished' -or (Get-Content $stderr -Raw)){throw 'Local TLS fixture failed'}
    Write-Output 'PASS PC real TLS interoperability, token-free discovery, false proof and false certificate rejected'
}finally{if($server -and !$server.HasExited){$server.Kill();$server.WaitForExit()}}
