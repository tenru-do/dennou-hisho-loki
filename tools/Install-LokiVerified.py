"""Verified local USB update. No external backup ZIPs and no network calls.
--output must be a private directory outside Git (or ignored). Existing captures block update.
"""
import argparse,hashlib,io,json,re,subprocess,tarfile,uuid
from pathlib import Path
DEVICES={'glass':('RG-glasses','com.example.rokidkeyboardbridge'),'phone':('SM-S942Q','com.example.rokidgeminisecretary')}
def require(value,message):
 if not value:raise RuntimeError(message)
def digest(path):
 h=hashlib.sha256()
 with path.open('rb') as f:
  for block in iter(lambda:f.read(1048576),b''):h.update(block)
 return h.hexdigest()
def private_output(path):
 path=path.resolve()
 for parent in (path,*path.parents):
  if (parent/'.git').exists():
   result=subprocess.run(['git','-c','safe.directory=*','-C',str(parent),'check-ignore','--quiet',str(path/'verification.json')],capture_output=True)
   require(result.returncode==0,'Choose a private ignored output directory');break
 return path
APK_PATH=re.compile(r'/data/app/[A-Za-z0-9_~+=./-]+/base\.apk$')
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--app',choices=DEVICES,required=True);parser.add_argument('--adb',required=True);parser.add_argument('--serial',required=True);parser.add_argument('--sdk',type=Path,required=True);parser.add_argument('--java',type=Path,required=True);parser.add_argument('--apk',type=Path,required=True);parser.add_argument('--expected-sha256',required=True);parser.add_argument('--output',type=Path,required=True);parser.add_argument('--launch',action='store_true');a=parser.parse_args()
 serial=a.serial;model,package=DEVICES[a.app];require(re.fullmatch(r"[A-Za-z0-9_-]{4,80}",serial) is not None,"Supply a USB device serial")
 def run(*parts):
  p=subprocess.run([a.adb,'-s',serial,*map(str,parts)],capture_output=True);require(p.returncode==0,'ADB operation failed: '+str(parts[:3]));return p.stdout.decode('utf-8',errors='replace').strip()
 def shell(*parts):return run('shell',*parts)
 def installed_path():
  paths=shell('pm','path',package).splitlines();require(len(paths)==1 and paths[0].startswith('package:'),'Expected one base APK');path=paths[0][8:];require(APK_PATH.fullmatch(path) is not None,'Unexpected installed APK path');return path
 def cert(path):
  p=subprocess.run([str(a.java),'-jar',str(a.sdk/'build-tools/37.0.0/lib/apksigner.jar'),'verify','--print-certs',str(path)],capture_output=True,text=True)
  require(p.returncode==0,'APK signature invalid');m=re.search(r'certificate SHA-256 digest: ([0-9a-f]+)',p.stdout);require(m is not None,'Certificate missing');return m.group(1)
 require(re.fullmatch(r'[a-fA-F0-9]{64}',a.expected_sha256) is not None,'Supply explicit expected SHA-256')
 require(digest(a.apk)==a.expected_sha256.lower(),'New APK hash mismatch')
 require(shell('getprop','ro.product.model')==model,'Wrong device model')
 service=shell('dumpsys','activity','services',package)
 require(not any(n in service for n in ['MeetingRecorderService','PhotoRecorderService']),'Stop user recording/KEEP before updating')
 out=private_output(a.output);out.mkdir(parents=True,exist_ok=False)
 previous=installed_path();run('pull',previous,out/'previous.apk');oldhash=digest(out/'previous.apk');require(oldhash==shell('sha256sum',previous).split()[0],'Previous APK copy hash mismatch')
 require(cert(a.apk)==cert(out/'previous.apk'),'Signing certificate differs')
 badging=subprocess.run([str(a.sdk/'build-tools/37.0.0/aapt2.exe'),'dump','badging',str(a.apk)],capture_output=True,text=True,encoding='utf-8',errors='replace');require(badging.returncode==0,'Cannot inspect APK');m=re.search(r"package: name='([^']+)' versionCode='(\d+)'",badging.stdout);require(m is not None and m.group(1)==package,'Wrong APK package');version=int(m.group(2))
 shell('am','force-stop',package);remote='cache/loki-update-'+uuid.uuid4().hex+'.tar';shell('run-as',package,'mkdir','-p','cache');shell('run-as',package,'tar','-cf',remote,'files','shared_prefs');backuphash=shell('run-as',package,'sha256sum',remote).split()[0]
 with (out/'app-data-backup.tar').open('wb') as stream:
  copied=subprocess.run([a.adb,'-s',serial,'exec-out','run-as',package,'cat',remote],stdout=stream,stderr=subprocess.PIPE);require(copied.returncode==0,'Backup transfer failed')
 backup=out/'app-data-backup.tar';require(digest(backup)==backuphash,'Backup transfer hash mismatch')
 with tarfile.open(backup) as archive:
  members=archive.getmembers();settings='shared_prefs/'+('gemini_settings.xml' if a.app=='glass' else 'phone_secretary.xml');require(any(x.name==settings and x.isfile() for x in members),'Expected app settings missing')
  for entry in members:
   if entry.isfile():
    with archive.extractfile(entry) as stream:
     size=sum(len(block) for block in iter(lambda:stream.read(1048576),b''));require(size==entry.size,'Truncated archive member')
 record={'device':serial,'model':model,'package':package,'version':version,'previousApkSha256':oldhash,'backupSha256':backuphash,'backupReadVerified':True,'signatureMatch':True,'installStarted':False,'installedHashVerified':False}
 def save():(out/'verification.json').write_text(json.dumps(record,indent=2),encoding='utf-8')
 save()
 # PC backup remains valid even if installation fails. Remove only this exact verified duplicate.
 require(shell('run-as',package,'sha256sum',remote).split()[0]==backuphash,'Backup changed');shell('run-as',package,'rm','--',remote);record['temporaryBackupRemoved']=True;record['installStarted']=True;save()
 require('Success' in run('install','-r',a.apk),'Install failed');path=installed_path();installed=shell('sha256sum',path).split()[0];require(installed==a.expected_sha256.lower(),'Installed APK hash mismatch');require('versionCode='+str(version)+' ' in shell('dumpsys','package',package),'Installed version differs');record.update(installedSha256=installed,installedHashVerified=True);save()
 if a.launch:shell('am','start','-n',package+'/.MainActivity')
 print(json.dumps({'app':a.app,'version':version,'verified':True,'launched':a.launch,'pcBackupRetained':True,'temporaryDuplicateRemoved':True}))
if __name__=='__main__':main()
