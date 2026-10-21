"""Permission-free isolated tests. Never starts or accesses the user's Loki application."""
from pathlib import Path
import argparse,hashlib,json,os,re,subprocess,uuid,zipfile
p=argparse.ArgumentParser();p.add_argument('--output',required=True,type=Path);args=p.parse_args()
r=Path(__file__).resolve().parents[2];src=Path(__file__).parent;sdk=Path('C:/Users/user/AppData/Local/Android/Sdk');bt=sdk/'build-tools/37.0.0';java=Path('C:/Program Files/Android/Android Studio/jbr/bin');adb=sdk/'platform-tools/adb.exe';pkg='com.example.lokireviewchecks';dest=args.output.resolve();dest.mkdir(parents=True,exist_ok=False);build=r/'.build-review-device';build.mkdir(exist_ok=True)
def command(*parts,binary=False):
 q=subprocess.run(list(map(str,parts)),capture_output=True)
 if q.returncode:raise RuntimeError(Path(str(parts[0])).name+' failed: '+q.stderr.decode('utf-8','replace')[:1000])
 return q.stdout if binary else q.stdout.decode('utf-8','replace').strip()
def sha(file):return hashlib.sha256(Path(file).read_bytes()).hexdigest()
inventory=command(adb,'devices').splitlines();serials=[x.split()[0] for x in inventory if re.fullmatch(r'[A-Za-z0-9_-]+\s+device',x.strip())];matches=[serial for serial in serials if command(adb,'-s',serial,'shell','getprop','ro.product.model')=='SM-S942Q']
if len(matches)!=1:raise RuntimeError('Expected exactly one authorized phone with the handoff model')
serial=matches[0]
def device(*parts,binary=False):return command(adb,'-s',serial,*parts,binary=binary)
if ('package:'+pkg) in device('shell','pm','list','packages',pkg).splitlines():raise RuntimeError('Diagnostic package already exists; preserve it and stop')
glass=r/'.build-integration/glass/loki-glass-integration.apk';glassHash=sha(glass);source=r/'.build-integration/phone/loki-phone-integration.apk';sourceHash=sha(source);android=sdk/'platforms/android-35/android.jar'
for name in ['classes','dex','host','host-dex']:(build/name).mkdir(exist_ok=True)
command(java/'javac.exe','-encoding','UTF-8','-cp',str(android)+';'+str(r/'.build-integration/phone/classes')+';'+str(r/'.build-integration/glass/classes'),'-d',build/'classes',src/'ReviewChecks.java',src/'ReliabilityChecks.java',src/'MaintenanceChecks.java',src/'ExperienceChecks.java',src/'GlassReservationChecks.java',src/'ConversationUpgradeChecks.java',src/'ConversationPlusChecks.java',src/'ConversationReliabilityChecks.java',src/'ConversationStreamChecks.java',src/'ConversationToolsChecks.java',src/'UsabilityChecks.java',src/'EverydayChecks.java')
command(java/'java.exe','-cp',bt/'lib/d8.jar','com.android.tools.r8.D8','--lib',android,'--output',build/'dex',*list((build/'classes').rglob('*.class')))
command(java/'javac.exe','-encoding','UTF-8','-cp',android,'-d',build/'host',src/'Audit.java')
command(java/'java.exe','-cp',bt/'lib/d8.jar','com.android.tools.r8.D8','--lib',android,'--output',build/'host-dex',*list((build/'host').rglob('*.class')))
manifest=build/'AndroidManifest.xml';manifest.write_text('<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="'+pkg+'"><uses-sdk android:minSdkVersion="30" android:targetSdkVersion="35"/><application android:debuggable="true" android:label="Loki isolated review checks"/><instrumentation android:name=".Audit" android:targetPackage="'+pkg+'"/></manifest>',encoding='utf-8')
command(bt/'aapt2.exe','link','--manifest',manifest,'-I',android,'-o',build/'unsigned.apk')
with zipfile.ZipFile(build/'unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:z.write(build/'host-dex/classes.dex','classes.dex')
command(bt/'zipalign.exe','-f','4',build/'unsigned.apk',build/'aligned.apk')
command(java/'java.exe','-jar',bt/'lib/apksigner.jar','sign','--ks',Path('C:/Users/user/.android/debug.keystore'),'--ks-key-alias','androiddebugkey','--ks-pass','pass:android','--key-pass','pass:android','--out',build/'test.apk',build/'aligned.apk')
command(java/'java.exe','-jar',bt/'lib/apksigner.jar','verify',build/'test.apk')
run=uuid.uuid4().hex[:12];folder='files/render-'+run;remote='/data/local/tmp/loki-review-'+run;installed=False
try:
 if 'Success' not in device('install',build/'test.apk'):raise RuntimeError('Diagnostic install failed')
 installed=True;path=device('shell','pm','path',pkg)
 if not re.fullmatch(r'package:/data/app/[A-Za-z0-9_~+=/.-]+/base\.apk',path):raise RuntimeError('Unexpected diagnostic APK path')
 if device('shell','sha256sum',path[8:]).split()[0]!=sha(build/'test.apk'):raise RuntimeError('Diagnostic installed APK mismatch')
 device('shell','run-as',pkg,'mkdir','-p',folder)
 for local,name in [(source,'source.apk'),(glass,'glass.apk'),(build/'dex/classes.dex','export.dex')]:
  device('push',local,remote+'-'+name);device('shell','chmod','444',remote+'-'+name);device('shell','run-as',pkg,'cp',remote+'-'+name,folder+'/'+name);device('shell','run-as',pkg,'chmod','444',folder+'/'+name)
  if device('shell','run-as',pkg,'sha256sum',folder+'/'+name).split()[0]!=sha(local):raise RuntimeError('Test input copy hash mismatch')
 print('Running candidate APK classes under separate UID; no network/capture/playback permissions',flush=True)
 result=device('shell','am','instrument','-w','-e','run',run,pkg+'/.Audit');(dest/'instrumentation.txt').write_text(result,encoding='utf-8')
 if 'failure=' in result or 'INSTRUMENTATION_CODE: -1' not in result:raise RuntimeError('Isolated checks failed: '+result[:1200])
 report=json.loads(device('shell','run-as',pkg,'cat',folder+'/review/result.json'));report.update(sourceApkSha256=sourceHash,glassApkSha256=glassHash,testApkSha256=sha(build/'test.apk'),model='SM-S942Q');(dest/'result.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps(report),flush=True)
 for style in ['short','detailed']:(dest/('provider-'+style+'.json')).write_bytes(device('exec-out','run-as',pkg,'cat',folder+'/review/stream/provider-'+style+'.json',binary=True))
finally:
 if installed:
  if 'Success' not in device('uninstall',pkg):raise RuntimeError('Reconnect phone to remove the diagnostic package')
 for name in ['source.apk','glass.apk','export.dex']:device('shell','rm','-f',remote+'-'+name)
 if ('package:'+pkg) in device('shell','pm','list','packages',pkg).splitlines():raise RuntimeError('Diagnostic cleanup unconfirmed')
 print('Isolated diagnostic package and its inputs removed',flush=True)
