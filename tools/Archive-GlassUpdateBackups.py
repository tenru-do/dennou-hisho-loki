"""Local USB only. Default inventory; --apply copies, verifies, then removes exact duplicate ZIPs.
No photo/recording/settings files, arbitrary shell commands, extraction or recursive removal.
PC archives contain personal data: keep output outside source control.
"""
import argparse, hashlib, json, re, shutil, subprocess, zipfile
from pathlib import Path
ROOT="/sdcard/Android/data/com.example.rokidkeyboardbridge/files"
NAME=re.compile(r"loki-pre-[A-Za-z0-9._-]+-backup\.zip$")
def digest(path):
 h=hashlib.sha256()
 with path.open("rb") as stream:
  for b in iter(lambda:stream.read(1024*1024),b""):h.update(b)
 return h.hexdigest()
def eligible(remote):return remote.startswith(ROOT+"/") and "/" not in remote[len(ROOT)+1:] and NAME.fullmatch(remote[len(ROOT)+1:]) is not None
def require(value,message):
 if not value:raise RuntimeError(message)
def main():
 a=argparse.ArgumentParser();a.add_argument('--adb',required=True);a.add_argument('--serial',required=True);a.add_argument('--output',type=Path,required=True);a.add_argument('--apply',action='store_true');args=a.parse_args()
 require(re.fullmatch(r'[A-Za-z0-9_-]{4,80}',args.serial) is not None,'Supply a USB device serial')
 def run(*parts):
  p=subprocess.run([args.adb,'-s',args.serial,*parts],capture_output=True,text=True,encoding='utf-8');p.check_returncode();return p.stdout.strip()
 require(run('shell','getprop','ro.product.model')=='RG-glasses','Wrong device')
 output=args.output.resolve();output.mkdir(parents=True,exist_ok=True)
 # Never store these private backups under a Git work tree.
 for parent in (output,*output.parents):
  if (parent/'.git').exists():
   ignored=subprocess.run(['git','-c','safe.directory=*','-C',str(parent),'check-ignore','--quiet',str(output/'manifest.json')],capture_output=True)
   if ignored.returncode:raise RuntimeError('Choose an ignored private output directory or a directory outside Git')
   break
 rows=[]
 for remote in run('shell','find',ROOT,'-maxdepth','1','-type','f','-name','loki-pre-*-backup.zip').splitlines():
  if not eligible(remote):raise RuntimeError('Unexpected backup path')
  size=int(run('shell','stat','-c','%s',remote));row={'remote':remote,'bytes':size,'removed':False};rows.append(row)
  if args.apply:
   target=output/Path(remote).name;part=target.with_suffix('.zip.partial')
   remote_sha=run('shell','sha256sum',remote).split()[0]
   if not target.exists() or digest(target)!=remote_sha:
    if target.exists():raise RuntimeError('Existing PC backup differs; preserve it and choose another output')
    if shutil.disk_usage(output).free<size+256*1024*1024:raise RuntimeError('Insufficient PC space')
    run('pull',remote,str(part));require(digest(part)==remote_sha,'Copy hash mismatch')
    with zipfile.ZipFile(part) as archive:
     require(bool(archive.infolist()),'Empty archive');require(archive.testzip() is None,'CRC mismatch')
    part.replace(target)
   else:
    with zipfile.ZipFile(target) as archive:require(archive.testzip() is None,'CRC mismatch')
   require(digest(target)==run('shell','sha256sum',remote).split()[0],'Source changed; do not remove')
   require(eligible(remote),'Unexpected backup path')
   run('shell','rm','--',remote)
   row.update(pc=str(target),sha256=remote_sha,removed=True)
  (output/'manifest.json').write_text(json.dumps(rows,indent=2),encoding='utf-8')
  print(json.dumps({'checked':len(rows),'removedBytes':sum(x['bytes'] for x in rows if x['removed'])}),flush=True)
 print('Inventory only' if not args.apply else 'Verified PC copies retained; only exact glass duplicates removed')
if __name__=='__main__':main()
