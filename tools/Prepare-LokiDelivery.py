#!/usr/bin/env python3
"""Prepare immutable candidates and hash-bound test evidence. Never installs or skips backup."""
from pathlib import Path
import argparse,hashlib,json,shutil,os
PHOTO_MARKERS=('photo','keep','transfer','storage','imageimport','googlephoto','importrequest')
def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def plan(changed):
 photo=any(any(k in str(p).lower() for k in PHOTO_MARKERS) for p in changed)
 return {'photoVerification':'content-hash' if photo else 'metadata','checks':['package-version','source-digest','smoke','regression'],'requiredPerInstall':['installed-signature-compatibility','fresh-private-data-backup','device-data-retention'],'photoCodeChanged':photo,'reuseRequires':['same-apk-sha256','same-source-digest','same-check-scope'],'installTool':'Install-LokiVerified.py','automaticInstall':False}
def prepare(apk,out,app,version,sources,evidence=None,changed=None):
 out=Path(out);out.mkdir(parents=True,exist_ok=True);apk=Path(apk);digest=sha(apk)
 source_hash=hashlib.sha256(json.dumps(sorted((str(p),sha(p)) for p in sources),ensure_ascii=False).encode()).hexdigest()
 target=out/f'loki-{app}-v{version}.apk'
 if target.exists() and sha(target)!=digest:raise ValueError('Immutable version exists with another SHA256; choose a new candidate directory')
 if not target.exists():
  # Exclusive creation prevents silently replacing an already-reviewed candidate.
  with target.open('xb') as dst,apk.open('rb') as src:shutil.copyfileobj(src,dst)
 if sha(target)!=digest:raise ValueError('Candidate copy differs')
 result={'app':app,'version':version,'sha256':digest,'sourceDigest':source_hash,'apk':str(target.resolve()),'plan':plan(sources if changed is None else changed),'evidenceReusable':False,'installed':False}
 if evidence:
  old=json.loads(Path(evidence).read_text(encoding='utf-8-sig'))
  if old.get('sha256')!=digest or old.get('sourceDigest')!=source_hash or old.get('plan')!=result['plan']:raise ValueError('Evidence is stale: APK, sources or check scope differ')
  validation=old.get('validation',{})
  if validation.get('state')!='passed' or not validation.get('artifacts'):raise ValueError('No completed validation evidence')
  for artifact in validation['artifacts']:
   if not Path(artifact['path']).is_file() or sha(artifact['path'])!=artifact['sha256']:raise ValueError('Validation artifact changed or missing')
  result['evidenceReusable']=True;result['evidenceSha256']=sha(evidence)
 manifest=out/f'loki-{app}-v{version}.json'
 if manifest.exists():
  prior=json.loads(manifest.read_text(encoding='utf8'))
  if prior.get('sha256')!=digest or prior.get('sourceDigest')!=source_hash:raise ValueError('Immutable manifest differs')
 else:
  with manifest.open('x',encoding='utf8') as stream:json.dump(result,stream,ensure_ascii=False,indent=2)
 return result
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--apk',required=True);p.add_argument('--out',required=True);p.add_argument('--app',choices=['glass','phone'],required=True);p.add_argument('--version',type=int,required=True);p.add_argument('--source',action='append',default=[]);p.add_argument('--evidence');p.add_argument('--changed',action='append');a=p.parse_args()
 print(json.dumps(prepare(a.apk,a.out,a.app,a.version,a.source,a.evidence,a.changed),ensure_ascii=False,indent=2))
