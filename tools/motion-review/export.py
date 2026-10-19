"""Export Android-rendered frames for local inspection. Requires a matching development build."""
from pathlib import Path
import argparse,subprocess,os,json,zipfile,tarfile,shutil,hashlib,re,time,uuid,xml.etree.ElementTree as ET

def main():
 p=argparse.ArgumentParser();p.add_argument('--serial',required=True);p.add_argument('--output',type=Path,required=True);p.add_argument('--poses',default='all');args=p.parse_args()
 if args.poses not in ('all','routes') and not re.fullmatch(r'\d+(,\d+)*',args.poses):p.error('--poses must be all, routes or comma-separated IDs')
 root=Path(__file__).resolve().parents[2];src=Path(__file__).parent.resolve();dest=args.output.resolve()
 if dest.exists() and any(dest.iterdir()):p.error('Output folder must be new or empty')
 sdk=Path(os.environ['LOCALAPPDATA'])/'Android/Sdk';bt=sdk/'build-tools/37.0.0';android=sdk/'platforms/android-35/android.jar';j=Path('C:/Program Files/Android/Android Studio/jbr/bin');adb=sdk/'platform-tools/adb.exe';pkg='com.example.rokidkeyboardbridge'
 build=root/'.build-motion-review';build.mkdir(exist_ok=True);dest.mkdir(parents=True,exist_ok=True)
 def command(*a,binary=False):
  q=subprocess.run(list(map(str,a)),capture_output=True)
  if q.returncode:raise RuntimeError('Tool failed: '+Path(str(a[0])).name+' '+q.stderr.decode('utf-8','replace')[:1200])
  return q.stdout if binary else q.stdout.decode('utf-8','replace').strip()
 def device(*a,binary=False):return command(adb,'-s',args.serial,*a,binary=binary)
 if device('shell','getprop','ro.product.model')!='RG-glasses':raise RuntimeError('Selected device is not RG-glasses')
 manifest=ET.parse(root/'glass-app/AndroidManifest.xml').getroot();expected=int(manifest.attrib['{http://schemas.android.com/apk/res/android}versionCode'])
 info=device('shell','dumpsys','package',pkg)
 if not re.search(r'versionCode='+str(expected)+r'\s',info):raise RuntimeError('Installed and local source versions differ; build/install matching source first')
 apkpath=device('shell','pm','path',pkg).split('package:',1)[1].strip();sha=device('shell','sha256sum',apkpath).split()[0]
 local=root/'.build-integration/glass/loki-glass-integration.apk'
 if not local.exists() or hashlib.sha256(local.read_bytes()).hexdigest()!=sha:raise RuntimeError('Installed APK differs from local development build')
 for name in ['classes','dex','audit-classes','audit-dex']:(build/name).mkdir(exist_ok=True)
 command(j/'javac.exe','-encoding','UTF-8','-cp',str(android)+';'+str(root/'.build-integration/glass/classes'),'-d',build/'classes',src/'RenderExport.java')
 command(j/'java.exe','-cp',bt/'lib/d8.jar','com.android.tools.r8.D8','--lib',android,'--output',build/'dex',*list((build/'classes').rglob('*.class')))
 command(j/'javac.exe','-encoding','UTF-8','-cp',android,'-d',build/'audit-classes',src/'Audit.java')
 command(j/'java.exe','-cp',bt/'lib/d8.jar','com.android.tools.r8.D8','--lib',android,'--output',build/'audit-dex',*list((build/'audit-classes').rglob('*.class')))
 command(bt/'aapt2.exe','link','--manifest',src/'AndroidManifest.xml','-I',android,'-o',build/'unsigned.apk')
 with zipfile.ZipFile(build/'unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:z.write(build/'audit-dex/classes.dex','classes.dex')
 command(bt/'zipalign.exe','-f','4',build/'unsigned.apk',build/'aligned.apk')
 command(j/'java.exe','-jar',bt/'lib/apksigner.jar','sign','--ks',Path.home()/'.android/debug.keystore','--ks-key-alias','androiddebugkey','--ks-pass','pass:android','--key-pass','pass:android','--out',build/'test.apk',build/'aligned.apk')
 remote='/data/local/tmp/loki-motion-review.dex';run=uuid.uuid4().hex[:12];folder='cache/motion-review-'+run
 try:
  device('push',build/'dex/classes.dex',remote);device('shell','chmod','444',remote);device('install','-r',build/'test.apk')
  print('Rendering native frames; no microphone, camera, API calls or conversation data.',flush=True)
  with subprocess.Popen([str(adb),'-s',args.serial,'shell','am','instrument','-w','-e','poses',args.poses,'-e','run',run,'com.example.lokimotionreview/.Audit'],stdout=subprocess.PIPE,stderr=subprocess.PIPE) as task:
   last=time.monotonic()
   while task.poll() is None:
    time.sleep(2)
    if time.monotonic()-last>25:
     print('Native rendering continues...',flush=True);last=time.monotonic()
   stdout,stderr=task.communicate();result=stdout.decode('utf-8','replace');(dest/'native-result.txt').write_text(result,encoding='utf-8')
   if task.returncode or 'failure=' in result or 'INSTRUMENTATION_CODE: -1' not in result:raise RuntimeError('Native export failed: '+result[:1600])
  archive=dest/'native-frames.tar'
  with archive.open('wb') as out:
   q=subprocess.run([str(adb),'-s',args.serial,'exec-out','run-as',pkg,'tar','-cf','-','-C',folder,'.'],stdout=out,stderr=subprocess.PIPE)
   if q.returncode:raise RuntimeError('Frame transfer failed')
  with tarfile.open(archive) as tar:
   total=0
   for member in tar:
    target=(dest/member.name).resolve();total+=member.size
    if not target.is_relative_to(dest) or member.issym() or member.islnk() or member.size>8_000_000 or total>1_000_000_000:raise RuntimeError('Unsafe frame archive')
    if member.isdir():target.mkdir(parents=True,exist_ok=True)
    elif member.isfile():
     target.parent.mkdir(parents=True,exist_ok=True)
     with tar.extractfile(member) as inp,target.open('wb') as out:shutil.copyfileobj(inp,out)
  archive.unlink()
  data=json.loads((dest/'manifest.json').read_text(encoding='utf-8'));data['apkSha256']=sha
  count=0
  for entry in data['entries']:
   for frames in entry['stages'].values():
    for f in frames:
     if not re.fullmatch(r'frames/[a-z0-9-]+\.png',f['file']):raise RuntimeError('Unexpected frame path')
     if not (dest/f['file']).read_bytes().startswith(b'\x89PNG'):raise RuntimeError('Invalid frame PNG')
     count+=1
  (dest/'manifest.json').write_text(json.dumps(data,ensure_ascii=False),encoding='utf-8');(dest/'manifest.js').write_text('window.MOTION_REVIEW='+json.dumps(data,ensure_ascii=False)+';',encoding='utf-8')
  for f in ['index.html','review.js','review.css']:shutil.copy2(src/f,dest/f)
  # Remove only the UUID-named directory created by this invocation, within this application's cache.
  resolved=device('shell','run-as',pkg,'readlink','-f',folder)
  if resolved in ['/data/user/0/'+pkg+'/'+folder,'/data/data/'+pkg+'/'+folder]:device('shell','run-as',pkg,'rm','-rf',folder)
  print(f'PASS {len(data["entries"])} images, {count} native frames. Open {dest/"index.html"}',flush=True)
 finally:
  try:device('uninstall','com.example.lokimotionreview');device('shell','rm','-f',remote);device('shell','am','start','-n',pkg+'/.MainActivity')
  except Exception:print('Diagnostic cleanup/normal launch needs checking.',flush=True)
if __name__=='__main__':main()
