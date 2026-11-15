from pathlib import Path
import zipfile,hashlib,io,json,subprocess,re,concurrent.futures,datetime
from PIL import Image
import numpy as np
import argparse
parser=argparse.ArgumentParser(description="Optimize distributed glass artwork while keeping source PNGs untouched.")
parser.add_argument("--apk",type=Path,required=True);parser.add_argument("--report-dir",type=Path,required=True)
args=parser.parse_args();r=Path(__file__).resolve().parent.parent;out=args.report_dir.resolve();out.mkdir(parents=True,exist_ok=True);apk=args.apk.resolve();b=apk.parent;original=out/"unoptimized.apk"

if not original.exists():__import__('shutil').copy2(apk,original)
rawZip=zipfile.ZipFile(original);names=rawZip.namelist();active=set()
for p in (r/'glass-app/src').rglob('*.java'):active.update(re.findall(r'loki_transition_flow_v\d+\.bin',p.read_text('utf-8')))
assert active,'No active transition flow references found'
drop=[n for n in names if n.startswith('assets/loki_transition_flow_v') and n.rsplit('/',1)[-1] not in active]
assert all(n.rsplit('/',1)[-1] not in active for n in drop)
imageNames=[n for n in names if n.startswith('assets/') and n.endswith('.png')]
def encode(name):
 raw=rawZip.read(name);im=Image.open(io.BytesIO(raw)).convert('RGBA');buf=io.BytesIO();im.save(buf,format='WEBP',quality=95,method=6,exact=True);data=buf.getvalue();decoded=Image.open(io.BytesIO(data)).convert('RGBA');assert decoded.size==im.size
 # Alpha remains lossless. Native-view-size comparison tests appearance, not just file size.
 assert np.array_equal(np.asarray(im.getchannel('A')),np.asarray(decoded.getchannel('A'))),name
 size=(min(480,im.width),max(1,round(im.height*min(480,im.width)/im.width)));a=Image.new('RGBA',im.size,(0,0,0,255));a.alpha_composite(im);c=Image.new('RGBA',decoded.size,(0,0,0,255));c.alpha_composite(decoded)
 a=np.asarray(a.convert('RGB').resize(size,Image.Resampling.LANCZOS),dtype=float);c=np.asarray(c.convert('RGB').resize(size,Image.Resampling.LANCZOS),dtype=float);mae=float(np.abs(a-c).mean());rmse=float(np.sqrt(((a-c)**2).mean()));
 if mae>=1.8 or rmse>=3.8:
  buf=io.BytesIO();im.save(buf,format="WEBP",lossless=True,method=6,exact=True);data=buf.getvalue();assert np.array_equal(np.asarray(im),np.asarray(Image.open(io.BytesIO(data)).convert("RGBA")));mae=0.0;rmse=0.0
 proof={'asset':name,'sourceSha256':hashlib.sha256(raw).hexdigest(),'encodedSha256':hashlib.sha256(data).hexdigest(),'sourceBytes':len(raw),'encodedBytes':len(data),'width':im.width,'height':im.height,'alphaUnchanged':True,'nativeSizeMeanAbsoluteError':mae,'nativeSizeRmse':rmse}
 return name,data,proof
encoded={};proofs=[]
with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
 for n,(name,data,proof) in enumerate(pool.map(encode,imageNames),1):
  encoded[name]=data;proofs.append(proof)
  if n%50==0 or n==len(imageNames):print('Encoded',n,'/',len(imageNames),flush=True)
compact=b/'compact-unsigned.apk'
with zipfile.ZipFile(compact,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as z:
 for info in rawZip.infolist():
  if info.filename in drop or info.filename.startswith('META-INF/'):continue
  if info.filename in encoded:
   z.writestr(info.filename,encoded[info.filename],compress_type=zipfile.ZIP_STORED)
  else:
   # Executable dex, resources and active transition data remain byte-identical.
   z.writestr(info.filename,rawZip.read(info.filename),compress_type=info.compress_type,compresslevel=9)
rawZip.close();sdk=Path('C:/Users/user/AppData/Local/Android/Sdk');bt=sdk/'build-tools/37.0.0';j=Path('C:/Program Files/Android/Android Studio/jbr/bin/java.exe')
for args in [[str(bt/'zipalign.exe'),'-f','4',str(compact),str(b/'compact-aligned.apk')],[str(j),'-jar',str(bt/'lib/apksigner.jar'),'sign','--ks','C:/Users/user/.android/debug.keystore','--ks-key-alias','androiddebugkey','--ks-pass','pass:android','--key-pass','pass:android','--out',str(apk),str(b/'compact-aligned.apk')],[str(j),'-jar',str(bt/'lib/apksigner.jar'),'verify',str(apk)]]:
 p=subprocess.run(args,capture_output=True,timeout=120);assert p.returncode==0,p.stderr.decode('utf-8','replace')[-350:]
with zipfile.ZipFile(original) as before,zipfile.ZipFile(apk) as after:
 for n in before.namelist():
  if n in drop or n.startswith('META-INF/') or n in imageNames:continue
  assert before.read(n)==after.read(n),n
result={'at':datetime.datetime.now(datetime.timezone.utc).isoformat(),'originalApkBytes':original.stat().st_size,'optimizedApkBytes':apk.stat().st_size,'originalSourcesUnmodified':True,'quality':95,'codec':'WebP; existing logical asset keys retained, Android decodes by file signature','dexAndResourcesByteIdentical':True,'removedUnreferencedFlows':drop,'removedCompressedFlowBytes':sum(zipfile.ZipFile(original).getinfo(n).compress_size for n in drop),'images':proofs,'maxNativeSizeMeanAbsoluteError':max(a['nativeSizeMeanAbsoluteError'] for a in proofs),'maxNativeSizeRmse':max(a['nativeSizeRmse'] for a in proofs),'apkSha256':hashlib.sha256(apk.read_bytes()).hexdigest()}
(out/'compact-artwork-verification.json').write_text(json.dumps(result,indent=2),encoding='utf-8');print(json.dumps({k:v for k,v in result.items() if k!='images'}),flush=True)
