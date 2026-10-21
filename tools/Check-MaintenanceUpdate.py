"""Read-only candidate/installed pair verification. Never installs, launches, stops, or erases an app."""
from pathlib import Path
import argparse,hashlib,json,os,re,subprocess
p=argparse.ArgumentParser();p.add_argument('--handoff',required=True,type=Path);p.add_argument('--rollback-dir',required=True,type=Path);p.add_argument('--output',required=True,type=Path);a=p.parse_args()
r=Path(__file__).resolve().parents[1];sdk=Path(os.environ.get('LOCALAPPDATA','C:/Users/user/AppData/Local'))/'Android/Sdk';bt=sdk/'build-tools/37.0.0';java=Path('C:/Program Files/Android/Android Studio/jbr/bin/java.exe');adb=sdk/'platform-tools/adb.exe'
def run(*args):
 q=subprocess.run(list(map(str,args)),capture_output=True)
 if q.returncode:raise RuntimeError('Read-only verification command failed; device identifiers omitted')
 return q.stdout.decode('utf-8','replace').strip()
def digest(f):return hashlib.sha256(f.read_bytes()).hexdigest()
def cert(f):
 text=run(java,'-jar',bt/'lib/apksigner.jar','verify','--print-certs',f);m=re.search(r'certificate SHA-256 digest: ([a-f0-9]{64})',text)
 if not m:raise RuntimeError('Candidate signer could not be verified')
 return m.group(1)
config=json.loads(a.handoff.read_text(encoding='utf-8-sig'));rows=[]
for target in config['devices']:
 kind=target['kind'];serial=target['serial'];package=target['package'];candidate=r/f'.build-integration/{kind}/loki-{kind}-integration.apk';old=a.rollback_dir/f'{kind}.apk'
 info=run(adb,'-s',serial,'shell','dumpsys','package',package);m=re.search(r'\bversionCode=(\d+)\b',info)
 if not m:raise RuntimeError('Installed version unavailable')
 installed=int(m.group(1));badging=run(bt/'aapt2.exe','dump','badging',candidate);v=re.search(r"versionCode='(\d+)'",badging);name=re.search(r"package: name='([^']+)'",badging)
 if not v or not name or name.group(1)!=package:raise RuntimeError('Candidate package mismatch')
 path=run(adb,'-s',serial,'shell','pm','path',package)
 if not re.fullmatch(r'package:/data/app/[A-Za-z0-9_~+=/.-]+/base\.apk',path):raise RuntimeError('Installed APK path unavailable')
 actual=run(adb,'-s',serial,'shell','sha256sum',path[8:]).split()[0];original=digest(old);signer=cert(candidate);same=signer==cert(old);model=run(adb,'-s',serial,'shell','getprop','ro.product.model');battery=run(adb,'-s',serial,'shell','dumpsys','battery');level=re.search(r'\blevel:\s*(\d+)',battery);df=run(adb,'-s',serial,'shell','df','-Pk','/data');parts=df.splitlines()[-1].split();free=int(parts[3])*1024 if len(parts)>=6 and parts[3].isdigit() else -1
 row={'app':kind,'modelMatches':model.replace('_','-')==target['model'].replace('_','-'),'installedVersion':installed,'candidateVersion':int(v.group(1)),'candidateSha256':digest(candidate),'installedMatchesHandoff':actual==target['apkSha256'],'rollbackMatchesInstalled':original==actual,'signatureMatchesRollback':same,'candidateSignatureValid':True,'batteryPercent':int(level.group(1)) if level else None,'freeBytes':free,'adbConnected':True,'installedByCheck':False}
 row['binaryChecksPass']=all([row['modelMatches'],row['installedMatchesHandoff'],row['rollbackMatchesInstalled'],same,row['candidateVersion']>installed,free>=100000000,row['batteryPercent'] is not None and row['batteryPercent']>=30]);rows.append(row)
report={'checks':rows,'binaryChecksPass':all(x['binaryChecksPass'] for x in rows),'automaticInstall':False,'dataBackupVerified':False,'dataSchemaDowngradeVerified':False,'next':'Create and verify a data backup before installation. Keep both original APKs. Do not uninstall or clear data. A binary match alone does not guarantee a safe data downgrade.'};a.output.parent.mkdir(parents=True,exist_ok=True);a.output.write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps({'devicesChecked':len(rows),'binaryChecksPass':report['binaryChecksPass'],'installed':False}))
