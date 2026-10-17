from pathlib import Path
from PIL import Image,ImageDraw
import re,json
repo=Path(__file__).resolve().parents[1]
src=repo/'glass-app/src/com/example/rokidkeyboardbridge';out=Path('outputs/fullbody-face-crops');out.mkdir(parents=True,exist_ok=True)
ftext=(src/'FullBodyRealisticFeatures.java').read_text()
features={int(i):[float(x.rstrip('f')) for x in row.split(',')] for row,i in re.findall(r'\{([^{}]+)\},?\s*//\s*(\d+)',ftext)}
rtext=(src/'FullBodyRealisticAssets.java').read_text().split('static final int[][] REGIONS=')[1]
regions=[[int(x) for x in row.split(',')] for row in re.findall(r'\{(\d+,\d+,\d+,\d+)\}',rtext)]
remap={2:10,6:36,9:28,10:3,14:0,15:49}
C=192;H=240;atlas=Image.new('RGB',(C*8,H*5),'black');rows=[];geoms=[];closed=[];opened=[]
for cell in range(37):
 pose=remap.get(cell,cell);f=features[pose];reg=regions[pose]
 name=f'loki_fullbody_{pose//6*6:03d}_{pose//6*6+5:03d}.png'
 frame=Image.open(repo/'glass-app/assets'/name).convert('RGB').crop(reg)
 ex=(f[2]+f[8])/2;ey=(f[3]+f[9])/2
 w=max(122,abs(f[8]-f[2])*4.8);h=w*H/C
 left=round(ex-w/2);top=round(ey-h*.45);w=round(w);h=round(h)
 raw=frame.crop((left,top,left+w,top+h));raw.save(out/f'fullbody-cell-{cell:02d}-source.png')
 resized=raw.resize((C,H),Image.Resampling.LANCZOS);atlas.paste(resized,((cell%8)*C,(cell//8)*H))
 g=[(f[2]-left)/w,(f[3]-top)/h,(f[8]-left)/w,(f[9]-top)/h,(f[14]-left)/w,(f[15]-top)/h]
 assert all(.05<x<.95 for x in g),(cell,g)
 geoms.append(g);closed.append([f[7]<.5,f[13]<.5]);opened.append(f[18]>2)
 rows.append({'cell':cell,'fullbodyPose':pose,'asset':name,'sourceBodyRegion':reg,'faceCropWithinBody':[left,top,left+w,top+h],'landmarks':g,'closedEyes':closed[-1],'openMouth':opened[-1]})
atlas.save(repo/'glass-app/assets/loki_face_fullbody_v506.png');atlas.save(out/'fullbody-face-37-source.png')
java='package com.example.rokidkeyboardbridge;\n/** Landmarks transformed directly from reviewed full-body crops. */\nfinal class FullBodyPortraitGeometry {\n static final float[][] LANDMARKS={\n'+',\n'.join('{'+','.join(f'{x:.6f}f' for x in row)+'}' for row in geoms)+'\n };\n static final boolean[][] CLOSED={'+','.join('{'+','.join(str(x).lower() for x in row)+'}' for row in closed)+'};\n static final boolean[] OPEN={'+','.join(str(x).lower() for x in opened)+'};\n}\n'
(src/'FullBodyPortraitGeometry.java').write_text(java,encoding='utf-8')
(out/'fullbody-face-37-manifest.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf-8')
print('37 full-body face cells and transformed eye/mouth landmarks created')