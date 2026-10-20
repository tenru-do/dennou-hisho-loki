"""Bounded, path-checked extraction of only animation PNGs and its manifest."""
from pathlib import Path
import json,re,shutil,tarfile

def extract_and_finish(archive,dest,src,sha,host=None):
 dest=Path(dest).resolve();total=0;seen=set()
 with tarfile.open(archive) as tar:
  for member in tar:
   name=member.name.removeprefix('./');total+=member.size
   if member.isdir():
    if name not in ('','.', 'frames','frames/'):raise ValueError('Unexpected archive directory')
    continue
   if not member.isfile() or member.issym() or member.islnk() or member.size>8_000_000 or total>1_000_000_000:raise ValueError('Unsafe animation archive member')
   if name!='manifest.json' and not re.fullmatch(r'frames/[a-z0-9-]+\.png',name):raise ValueError('Unexpected animation file')
   target=(dest/name).resolve()
   if not target.is_relative_to(dest) or name in seen or target.exists():raise ValueError('Duplicate/existing or escaping animation file')
   seen.add(name);target.parent.mkdir(parents=True,exist_ok=True)
   with tar.extractfile(member) as inp,target.open('wb') as out:shutil.copyfileobj(inp,out)
 data=json.loads((dest/'manifest.json').read_text(encoding='utf-8'));data['apkSha256']=sha
 if host:data['host']=host
 for entry in data['entries']:
  for frames in entry['stages'].values():
   for frame in frames:
    name=frame['file']
    if not re.fullmatch(r'frames/[a-z0-9-]+\.png',name) or not (dest/name).read_bytes().startswith(b'\x89PNG'):raise ValueError('Missing/invalid animation frame')
 (dest/'manifest.json').write_text(json.dumps(data,ensure_ascii=False),encoding='utf-8');(dest/'manifest.js').write_text('window.MOTION_REVIEW='+json.dumps(data,ensure_ascii=False).replace('<','\\u003c')+';',encoding='utf-8')
 for name in ['index.html','review.js','review.css']:shutil.copy2(src/name,dest/name)
 return data
