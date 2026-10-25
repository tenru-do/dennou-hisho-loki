from pathlib import Path
import json,subprocess,re,zipfile,hashlib
import argparse,os
parser=argparse.ArgumentParser(description="Isolated Android TextView/Canvas header checks; no user app data or network permissions")
parser.add_argument('--serial',required=True);parser.add_argument('--output',type=Path,required=True);args=parser.parse_args()
repo=Path(__file__).resolve().parents[2];d={'serial':args.serial};sdk=Path(os.environ.get('ANDROID_SDK_ROOT',str(Path(os.environ.get('LOCALAPPDATA',''))/'Android/Sdk')));bt=sdk/'build-tools/37.0.0';java=Path(os.environ.get('JAVA_HOME','C:/Program Files/Android/Android Studio/jbr'))/'bin';adb=sdk/'platform-tools/adb.exe';folder=args.output.resolve();folder.mkdir(exist_ok=False);pkg='com.example.lokinavheadertest'

def call(*args):
 p=subprocess.run([str(x) for x in args],capture_output=True,timeout=180);assert p.returncode==0,'Tool failed: '+Path(str(args[0])).name;return p.stdout.decode('utf-8','replace')
def dev(*args):return call(adb,'-s',d['serial'],*args)
assert ('package:'+pkg) not in dev('shell','pm','list','packages',pkg).splitlines()
source=Path(__file__).with_name('HeaderAudit.java')
classes=folder/'classes';dex=folder/'dex';classes.mkdir();dex.mkdir();android=sdk/'platforms/android-35/android.jar';prod=repo/'glass-app/src/com/example/rokidkeyboardbridge';call(java/'javac.exe','-encoding','UTF-8','-cp',android,'-d',classes,source,prod/'NavigationHeaderStyle.java',prod/'NavigationHeaderLayout.java');call(java/'java.exe','-cp',bt/'lib/d8.jar','com.android.tools.r8.D8','--lib',android,'--output',dex,*classes.rglob('*.class'))
manifest=folder/'AndroidManifest.xml';manifest.write_text('<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="'+pkg+'"><uses-sdk android:minSdkVersion="26" android:targetSdkVersion="35"/><application android:debuggable="true" android:label="Loki isolated header check"/><instrumentation android:name="com.example.rokidkeyboardbridge.HeaderAudit" android:targetPackage="'+pkg+'"/></manifest>',encoding='utf-8');call(bt/'aapt2.exe','link','--manifest',manifest,'-I',android,'-o',folder/'unsigned.apk')
with zipfile.ZipFile(folder/'unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:z.write(dex/'classes.dex','classes.dex')
call(bt/'zipalign.exe','-f','4',folder/'unsigned.apk',folder/'aligned.apk');call(java/'java.exe','-jar',bt/'lib/apksigner.jar','sign','--ks',Path.home()/'.android/debug.keystore','--ks-key-alias','androiddebugkey','--ks-pass','pass:android','--key-pass','pass:android','--out',folder/'test.apk',folder/'aligned.apk');installed=False
try:
 assert 'Success' in dev('install',folder/'test.apk');installed=True;raw=dev('shell','am','instrument','-w',pkg+'/com.example.rokidkeyboardbridge.HeaderAudit');(folder/'instrumentation.txt').write_text(raw,encoding='utf-8');m=re.search(r'INSTRUMENTATION_RESULT: safeReport=(.+)',raw);assert m,'Header test failed; inspect private log';result=json.loads(m[1]);result['scope']='production header style/layout source in separate permission-free UID on glasses; real Android font measurement and canvas; no user app data, sensors or provider calls';(folder/'result.json').write_text(json.dumps(result,indent=2),encoding='utf-8');print(json.dumps(result))
finally:
 if installed:assert 'Success' in dev('uninstall',pkg)
