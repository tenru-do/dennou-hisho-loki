"""Render a local glasses APK in an isolated, permission-free Android software Canvas host.
This checks the APK's renderer on the chosen Android host, not glasses hardware or wearing UX.
"""
from pathlib import Path
import argparse,hashlib,json,os,re,shutil,subprocess,tarfile,time,uuid,zipfile

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--serial',required=True);p.add_argument('--output',required=True,type=Path);p.add_argument('--poses',default='all');args=p.parse_args()
 if args.poses not in ('all','routes') and not re.fullmatch(r'\d+(,\d+)*',args.poses):p.error('Invalid pose selection')
 dest=args.output.resolve()
 if dest.exists() and any(dest.iterdir()):p.error('Output must be new or empty')
 r=Path(__file__).resolve().parents[2];src=Path(__file__).parent;build=r/'.build-motion-host';build.mkdir(exist_ok=True);dest.mkdir(parents=True,exist_ok=True);sdk=Path(os.environ['LOCALAPPDATA'])/'Android/Sdk';bt=sdk/'build-tools/37.0.0';j=Path('C:/Program Files/Android/Android Studio/jbr/bin');android=sdk/'platforms/android-35/android.jar';adb=sdk/'platform-tools/adb.exe';pkg='com.example.lokimotionhost';run=uuid.uuid4().hex[:12];folder='files/render-'+run
 def cmd(*args,binary=False):
  q=subprocess.run(list(map(str,args)),capture_output=True)
  if q.returncode:raise RuntimeError(Path(str(args[0])).name+': '+q.stderr.decode('utf-8','replace')[:900])
  return q.stdout if binary else q.stdout.decode('utf-8','replace').strip()
 def device(*a,binary=False):return cmd(adb,'-s',args.serial,*a,binary=binary)
 source=r/'.build-integration/glass/loki-glass-integration.apk';sha=hashlib.sha256(source.read_bytes()).hexdigest();model=device('shell','getprop','ro.product.model');api=int(device('shell','getprop','ro.build.version.sdk'))
 if api<30:raise RuntimeError('Host needs Android 11 or later')
 state={'run':run,'folder':folder,'serial':args.serial,'model':model,'sourceApkSha256':sha,'selection':args.poses,'rendererHost':'isolated software Canvas; not glasses hardware','phase':'building'};(dest/'export-state.json').write_text(json.dumps(state,indent=2))
 for d in ['classes','dex','host','host-dex']:(build/d).mkdir(exist_ok=True)
 cmd(j/'javac.exe','-encoding','UTF-8','-cp',str(android)+';'+str(r/'.build-integration/glass/classes'),'-d',build/'classes',src/'RenderExport.java');cmd(j/'java.exe','-cp',bt/'lib/d8.jar','com.android.tools.r8.D8','--lib',android,'--output',build/'dex',*list((build/'classes').rglob('*.class')))
 shutil.copy2(src/'HostAudit.java',build/'Audit.java');cmd(j/'javac.exe','-encoding','UTF-8','-cp',android,'-d',build/'host',build/'Audit.java');cmd(j/'java.exe','-cp',bt/'lib/d8.jar','com.android.tools.r8.D8','--lib',android,'--output',build/'host-dex',*list((build/'host').rglob('*.class')))
 (build/'AndroidManifest.xml').write_text('<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="'+pkg+'"><uses-sdk android:minSdkVersion="30" android:targetSdkVersion="35"/><application android:debuggable="true" android:label="Loki isolated render audit"/><instrumentation android:name=".Audit" android:targetPackage="'+pkg+'"/></manifest>')
 cmd(bt/'aapt2.exe','link','--manifest',build/'AndroidManifest.xml','-I',android,'-o',build/'unsigned.apk')
 with zipfile.ZipFile(build/'unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:z.write(build/'host-dex/classes.dex','classes.dex')
 cmd(bt/'zipalign.exe','-f','4',build/'unsigned.apk',build/'aligned.apk');cmd(j/'java.exe','-jar',bt/'lib/apksigner.jar','sign','--ks',Path.home()/'.android/debug.keystore','--ks-key-alias','androiddebugkey','--ks-pass','pass:android','--key-pass','pass:android','--out',build/'test.apk',build/'aligned.apk')
 remote='/data/local/tmp/loki-render-'+run
 try:
  device('install','-r',build/'test.apk');device('shell','run-as',pkg,'mkdir','-p',folder)
  for local,name in [(source,'source.apk'),(build/'dex/classes.dex','export.dex')]:
   device('push',local,remote+'-'+name);device('shell','chmod','444',remote+'-'+name);device('shell','run-as',pkg,'cp',remote+'-'+name,folder+'/'+name);device('shell','run-as',pkg,'chmod','444',folder+'/'+name)
  copied=device('shell','run-as',pkg,'sha256sum',folder+'/source.apk').split()[0]
  if copied!=sha:raise RuntimeError('Copied APK hash differs')
  state['phase']='rendering';(dest/'export-state.json').write_text(json.dumps(state,indent=2));print('Rendering on isolated host '+model+'; no network/capture/user app access',flush=True)
  with subprocess.Popen([str(adb),'-s',args.serial,'shell','am','instrument','-w','-e','run',run,'-e','poses',args.poses,pkg+'/.Audit'],stdout=subprocess.PIPE,stderr=subprocess.PIPE) as task:
   last=time.monotonic()
   while task.poll() is None:
    time.sleep(2)
    if time.monotonic()-last>=25:print('Isolated Canvas render continues...',flush=True);last=time.monotonic()
   stdout,stderr=task.communicate();result=stdout.decode('utf-8','replace');(dest/'native-result.txt').write_text(result)
   if task.returncode or 'failure=' in result or 'INSTRUMENTATION_CODE: -1' not in result:raise RuntimeError('Host render failed: '+result[:1600])
  archive=dest/'frames.tar'
  with archive.open('wb') as out:
   q=subprocess.run([str(adb),'-s',args.serial,'exec-out','run-as',pkg,'tar','-cf','-','-C',folder+'/review','.'],stdout=out,stderr=subprocess.PIPE)
   if q.returncode:raise RuntimeError('Frame transfer failed')
  from export_transfer import extract_and_finish
  extract_and_finish(archive,dest,src,sha,{'model':model,'serial':args.serial,'type':'isolated_android_canvas','glassesHardwareTest':False})
  archive.unlink();state['phase']='complete';(dest/'export-state.json').write_text(json.dumps(state,indent=2));print('PASS host export '+str(dest),flush=True)
 finally:
  # Uninstall only this permission-free diagnostic package, which owns all copied render inputs.
  try:device('uninstall',pkg)
  except Exception:print('Diagnostic host uninstall requires reconnection',flush=True)
  for name in ['source.apk','export.dex']:
   try:device('shell','rm','-f',remote+'-'+name)
   except Exception:print('Own diagnostic temporary input cleanup requires reconnection',flush=True)
if __name__=='__main__':main()
