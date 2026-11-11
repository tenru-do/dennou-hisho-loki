"""Separate permission-free Android UID; synthetic detached TextView/Canvas checks only."""
from pathlib import Path
import argparse, hashlib, json, os, re, subprocess, zipfile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--serial', required=True)
parser.add_argument('--output', type=Path, required=True)
args = parser.parse_args()
repo = Path(__file__).resolve().parents[2]
sdk = Path(os.environ.get('ANDROID_SDK_ROOT', str(Path(os.environ.get('LOCALAPPDATA', '')) / 'Android/Sdk')))
bt = sdk / 'build-tools/37.0.0'
java = Path(os.environ.get('JAVA_HOME', 'C:/Program Files/Android/Android Studio/jbr')) / 'bin'
adb = sdk / 'platform-tools/adb.exe'
folder = args.output.resolve()
folder.mkdir(exist_ok=False, parents=True)
pkg = 'com.example.lokinavmovementtest'

def call(*command, binary=False):
    process = subprocess.run([str(value) for value in command], capture_output=True, timeout=60)
    if process.returncode:
        raise RuntimeError('Tool failed: ' + Path(str(command[0])).name + '\n' + process.stderr.decode('utf-8', 'replace'))
    return process.stdout if binary else process.stdout.decode('utf-8', 'replace')

def dev(*command, binary=False):
    return call(adb, '-s', args.serial, *command, binary=binary)

if ('package:' + pkg) in dev('shell', 'pm', 'list', 'packages', pkg).splitlines():
    raise RuntimeError('Existing package is not owned by this run; refusing replacement')
classes, dex = folder / 'classes', folder / 'dex'
classes.mkdir(); dex.mkdir()
android = sdk / 'platforms/android-35/android.jar'
production = repo / 'glass-app/src/com/example/rokidkeyboardbridge/NavigationMovementLine.java'
checks = repo / 'tools/NavigationMovementLineChecks.java'
call(java / 'javac.exe', '-encoding', 'UTF-8', '-cp', android, '-d', classes, production, checks, Path(__file__).with_name('MovementAudit.java'))
call(java / 'java.exe', '-cp', bt / 'lib/d8.jar', 'com.android.tools.r8.D8', '--lib', android, '--output', dex, *classes.rglob('*.class'))
manifest = folder / 'AndroidManifest.xml'
manifest.write_text('<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="' + pkg + '"><uses-sdk android:minSdkVersion="26" android:targetSdkVersion="35"/><application android:debuggable="true" android:label="Loki isolated movement check"/><instrumentation android:name="com.example.rokidkeyboardbridge.MovementAudit" android:targetPackage="' + pkg + '"/></manifest>', encoding='utf-8')
call(bt / 'aapt2.exe', 'link', '--manifest', manifest, '-I', android, '-o', folder / 'unsigned.apk')
with zipfile.ZipFile(folder / 'unsigned.apk', 'a', zipfile.ZIP_DEFLATED) as archive:
    archive.write(dex / 'classes.dex', 'classes.dex')
call(bt / 'zipalign.exe', '-f', '4', folder / 'unsigned.apk', folder / 'aligned.apk')
call(java / 'java.exe', '-jar', bt / 'lib/apksigner.jar', 'sign', '--ks', Path.home() / '.android/debug.keystore', '--ks-key-alias', 'androiddebugkey', '--ks-pass', 'pass:android', '--key-pass', 'pass:android', '--out', folder / 'test.apk', folder / 'aligned.apk')
installed = False
try:
    if 'Success' not in dev('install', folder / 'test.apk'):
        raise RuntimeError('Isolated test package was not installed')
    installed = True
    raw = dev('shell', 'am', 'instrument', '-w', pkg + '/com.example.rokidkeyboardbridge.MovementAudit')
    (folder / 'instrumentation.txt').write_text(raw, encoding='utf-8')
    match = re.search(r'INSTRUMENTATION_RESULT: safeReport=(.+)', raw)
    if not match:
        raise RuntimeError('Movement checks failed; inspect instrumentation.txt')
    report = json.loads(match[1])
    report['productionSourceSha256'] = hashlib.sha256(production.read_bytes()).hexdigest()
    report['scope'] = 'production movement TextView in separate permission-free UID; synthetic text, real Android shaping/Canvas; no user app data or provider calls'
    for width in (132, 240):
        (folder / ('movement-' + str(width) + '.png')).write_bytes(dev('exec-out', 'run-as', pkg, 'cat', 'files/movement-' + str(width) + '.png', binary=True))
    (folder / 'result.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
    print(json.dumps(report))
finally:
    if installed and 'Success' not in dev('uninstall', pkg):
        raise RuntimeError('Owned isolated test package cleanup failed')
