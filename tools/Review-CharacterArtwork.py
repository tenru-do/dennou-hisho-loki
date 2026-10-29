"""Offline review; never edits production art or auto-approves it. Requires Pillow.
Landmarks JSON: referenceKey, frames [{crop:[x,y,w,h],head:[x,y,w,h],
shoulders:[[x,y],[x,y]],hip:[x,y],ankle:[x,y],top:n,bottom:n}].
Coordinates are manually inspected, crop-local; no automatic anatomy detector is claimed.
"""
from pathlib import Path
import argparse,hashlib,html,json,math
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parent.parent
def metrics(f):
 height=f['bottom']-f['top'];assert height>0
 shoulder=math.dist(*f['shoulders']);assert shoulder>0
 return {'headWidthPerHeight':f['head'][2]/height,'headPerShoulder':f['head'][2]/shoulder,'shoulderPerHeight':shoulder/height,'legPerHeight':math.dist(f['hip'],f['ankle'])/height}
def compare(frame,reference):
 a,b=metrics(frame),metrics(reference);limits={'headWidthPerHeight':.15,'headPerShoulder':.15,'shoulderPerHeight':.18,'legPerHeight':.18}
 return {'metrics':a,'reference':b,'flags':[k for k in a if abs(a[k]/b[k]-1)>limits[k]]}
def run(candidate,landmarks,output):
 contract=json.loads((ROOT/'artwork/character-landmarks.json').read_text(encoding='utf-8'));ref=contract[landmarks['referenceKey']];refimg=Image.open(ROOT/ref['image']).convert('RGB');im=Image.open(candidate).convert('RGB');output.mkdir(parents=True,exist_ok=True)
 frames=landmarks['frames'];sheet=Image.new('RGB',(240*(len(frames)+1),560));draw=ImageDraw.Draw(sheet)
 def paste(image,crop,i,label,mark):
  x,y,w,h=crop;assert x>=0 and y>=0 and x+w<=image.width and y+h<=image.height
  cell=image.crop((x,y,x+w,y+h));overlay=ImageDraw.Draw(cell);hx,hy,hw,hh=mark['head'];assert hw>0 and hh>0 and hx>=0 and hy>=0 and hx+hw<=w and hy+hh<=h
  overlay.rectangle((hx,hy,hx+hw,hy+hh),outline='cyan',width=2);overlay.line([tuple(x) for x in mark['shoulders']],fill='yellow',width=2);overlay.line([tuple(mark['hip']),tuple(mark['ankle'])],fill='magenta',width=2);cell.thumbnail((230,520));sheet.paste(cell,(i*240+(240-cell.width)//2,30+(520-cell.height)//2));draw.text((i*240+8,8),label,fill='white')
 paste(refimg,ref['frame']['crop'],0,'APPROVED REFERENCE',ref['frame']);results=[]
 for i,f in enumerate(frames):paste(im,f['crop'],i+1,'FRAME '+str(i),f);results.append(compare(f,ref['frame']))
 sheet.save(output/'comparison.png');report={'candidateSha256':hashlib.sha256(Path(candidate).read_bytes()).hexdigest(),'referenceKey':landmarks['referenceKey'],'frames':results,'manualFaceIdentityReviewRequired':True,'approved':False};(output/'metrics.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
 (output/'review.html').write_text('<meta charset="utf-8"><title>Loki artwork review</title><style>body{background:#111;color:#eee;font-family:sans-serif}img{max-width:100%}pre{white-space:pre-wrap}</style><h1>ロキ：顔・肩幅・脚の比率確認</h1><p>水色枠は頭、黄線は肩幅、紫線は腰から足首。手入力の目印による比較です。顔立ち・手・服の丈・遠近感も原画と実表示サイズで確認してから登録してください。自動承認は行いません。</p><img src="comparison.png"><pre>'+html.escape(json.dumps(report,ensure_ascii=False,indent=2))+'</pre>',encoding='utf-8')
 return report
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--candidate',type=Path,required=True);p.add_argument('--landmarks',type=Path,required=True);p.add_argument('--output',type=Path,required=True);a=p.parse_args();report=run(a.candidate,json.loads(a.landmarks.read_text(encoding='utf-8')),a.output);print(json.dumps({'flaggedFrames':sum(bool(x['flags']) for x in report['frames']),'approved':False}));raise SystemExit(2 if any(x['flags'] for x in report['frames']) else 0)
