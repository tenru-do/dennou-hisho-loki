"""Recover a completed UUID-named native export after USB interruption."""
from pathlib import Path
import argparse,subprocess,re,json,hashlib,sys
from export_transfer import extract_and_finish

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--serial',required=True);p.add_argument('--run',required=True);p.add_argument('--output',required=True,type=Path);p.add_argument('--audit',action='store_true');a=p.parse_args()
 if not re.fullmatch('[a-f0-9]{12}',a.run):p.error('Invalid run ID')
 dest=a.output.resolve();src=Path(__file__).parent;r=src.parents[1];pkg='com.example.rokidkeyboardbridge';adb=Path.home()/'AppData/Local/Android/Sdk/platform-tools/adb.exe';folder='cache/motion-review-'+a.run
 if dest.exists() and any(f.name not in ['native-result.txt','export-state.json'] for f in dest.iterdir()):p.error('Use a new output or the interrupted output folder')
 dest.mkdir(parents=True,exist_ok=True)
 def d(*args):return subprocess.check_output([str(adb),'-s',a.serial,*args]).decode('utf-8','replace').strip()
 if d('shell','getprop','ro.product.model')!='RG-glasses':raise ValueError('Not glasses')
 apk=d('shell','pm','path',pkg).split('package:',1)[1].strip();sha=d('shell','sha256sum',apk).split()[0]
 if sha!=hashlib.sha256((r/'.build-integration/glass/loki-glass-integration.apk').read_bytes()).hexdigest():raise ValueError('APK/source mismatch')
 manifest=json.loads(d('exec-out','run-as',pkg,'cat',folder+'/manifest.json'))
 if manifest.get('schema')!=1 or manifest.get('errors'):raise ValueError('Export incomplete/errors; do not claim completion')
 archive=dest/'native-frames.tar'
 with archive.open('wb') as out:
  subprocess.run([str(adb),'-s',a.serial,'exec-out','run-as',pkg,'tar','-cf','-','-C',folder,'.'],stdout=out,check=True)
 data=extract_and_finish(archive,dest,src,sha,{'type':'glasses_android_canvas','model':'RG-glasses','glassesHardwareTest':True,'wearingTest':False});archive.unlink()
 (dest/'recovery.json').write_text(json.dumps({'run':a.run,'apkSha256':sha,'entries':len(data['entries']),'recoveredAfterUsbInterruption':True},indent=2))
 if a.audit:subprocess.run([sys.executable,str(src/'audit.py'),'--review',str(dest),'--output',str(dest/'auto-audit')],check=True)
 resolved=d('shell','run-as',pkg,'readlink','-f',folder)
 if resolved not in ['/data/user/0/'+pkg+'/'+folder,'/data/data/'+pkg+'/'+folder]:raise ValueError('Refusing unexpected cleanup path')
 d('shell','run-as',pkg,'rm','-rf',folder);d('uninstall','com.example.lokimotionreview');d('shell','rm','-f','/data/local/tmp/loki-motion-review.dex');d('shell','am','start','-n',pkg+'/.MainActivity');print('PASS recovered export and own diagnostic cleanup')
if __name__=='__main__':main()
