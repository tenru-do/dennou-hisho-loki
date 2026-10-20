"""Local heuristic audit of actual Android-rendered animation exports; no network/model calls."""
from __future__ import annotations
import argparse,csv,hashlib,json,math,os,shutil,sys
from datetime import datetime,timezone
from pathlib import Path,PurePosixPath
import numpy as np
from PIL import Image

DEFAULTS={"ink_threshold":24,"pixel_delta":10,"minimum_changed_fraction":0.002,"maximum_scale_ratio":1.16,"maximum_center_jump_fraction":0.07,"maximum_head_body_offset_px":5.0,"maximum_transition_changed_fraction":0.48,"mouth_min_eye_ratio":0.15,"mouth_max_eye_ratio":2.8}
NAMES={"missing_frame":"コマ不足・読込失敗","dimension":"画像寸法の不一致","empty_frame":"描画が空","frozen":"動作コマに変化なし","mouth_static":"口領域の変化が弱い","eyes_static":"目領域の変化が弱い","scale_jump":"同じ動作内で縮尺が跳ねる候補","position_jump":"同じ動作内で位置が跳ねる候補","head_body_drift":"顔と胴体の相対位置ずれ候補","transition_jump":"切替コマ間の変化が大きい候補","anchor_geometry":"目と口の登録位置を要確認","coverage":"検査データ不足","export_error":"描画書出しエラー"}

def settings(path=None):
    values=dict(DEFAULTS)
    if path:
        supplied=json.loads(Path(path).read_text(encoding='utf-8-sig'))
        if not isinstance(supplied,dict) or set(supplied)-set(DEFAULTS):raise ValueError('Unknown threshold configuration')
        for key,value in supplied.items():
            if isinstance(value,bool) or not isinstance(value,(int,float)) or not math.isfinite(value) or value<0 or value>1000:raise ValueError('Invalid threshold: '+key)
        values.update(supplied)
    if not 0<values['maximum_scale_ratio']<=5 or not 0<=values['ink_threshold']<255 or not 0<values['pixel_delta']<=255:raise ValueError('Invalid image/scale thresholds')
    for key in ['minimum_changed_fraction','maximum_center_jump_fraction','maximum_transition_changed_fraction']:
        if not 0<=values[key]<=1:raise ValueError('Invalid fractional threshold: '+key)
    return values

def frame_path(root,name):
    import re
    if not isinstance(name,str) or not re.fullmatch(r'frames/[a-z0-9-]+\.png',name):raise ValueError('Unsafe frame path')
    path=(root/name).resolve()
    if not path.is_relative_to(root.resolve()) or (root/name).is_symlink():raise ValueError('Frame escapes review folder')
    return path

def load(root,shot,width,height):
    path=frame_path(root,shot['file']);raw=path.read_bytes()
    if len(raw)>8_000_000:raise ValueError('Oversize frame')
    with Image.open(path) as im:
        if im.size!=(width,height):raise ValueError('dimension')
        if im.format!='PNG':raise ValueError('Not PNG')
        # max channel preserves cyan line art rather than treating it as dim grey.
        a=np.asarray(im.convert('RGB'),dtype=np.uint8).max(axis=2)
    return a,hashlib.sha256(raw).hexdigest()

def box_metrics(a,threshold):
    ys,xs=np.where(a>threshold)
    if len(xs)<8:return None
    x0,x1=np.quantile(xs,[.005,.995]);y0,y1=np.quantile(ys,[.005,.995])
    return {'width':float(x1-x0+1),'height':float(y1-y0+1),'x':float((x0+x1)/2),'y':float((y0+y1)/2),'pixels':int(len(xs))}

def crop(a,rect):
    x,y,w,h=rect;left=max(0,int(math.floor(x)));top=max(0,int(math.floor(y)));right=min(a.shape[1],int(math.ceil(x+w)));bottom=min(a.shape[0],int(math.ceil(y+h)))
    return a[top:bottom,left:right] if right-left>=3 and bottom-top>=3 else None

def centroid(a,threshold):
    if a is None:return None
    ys,xs=np.where(a>threshold)
    if len(xs)<5:return None
    return np.array([xs.mean(),ys.mean()])

def changed(a,b,cfg):
    return float(np.mean(np.abs(a.astype(np.int16)-b.astype(np.int16))>=cfg['pixel_delta']))

def aligned_changed(a,b,cfg):
    if a is None or b is None:return None
    ca,cb=centroid(a,cfg['ink_threshold']),centroid(b,cfg['ink_threshold'])
    if ca is None or cb is None:return None
    dx,dy=np.clip(np.rint(cb-ca),-6,6).astype(int)
    h,w=a.shape;y0=max(0,-dy);y1=min(h,h-dy);x0=max(0,-dx);x1=min(w,w-dx)
    if x1-x0<3 or y1-y0<3:return None
    return changed(a[y0:y1,x0:x1],b[y0+dy:y1+dy,x0+dx:x1+dx],cfg)

def regions(entry):
    anchors=entry.get('anchors')
    if not anchors:return None
    points=[np.array(anchors[k],dtype=float) for k in ['leftEye','rightEye','mouth']]
    if any(p.shape!=(2,) or not np.isfinite(p).all() for p in points):raise ValueError('Invalid anchors')
    l,r,m=points;eye=(l+r)/2;span=float(np.linalg.norm(r-l))
    if span<2:return None
    return {'span':span,'eyeCenter':eye,'mouthCenter':m,'head':[min(l[0],r[0])-span, min(l[1],r[1])-span,span*3, max(span*2.3,m[1]-min(l[1],r[1])+span*1.5)],'eyes':[min(l[0],r[0])-span*.45,min(l[1],r[1])-span*.35,span*1.9,abs(r[1]-l[1])+span*.7],'mouth':[m[0]-span*.60,m[1]-span*.35,span*1.2,span*.7],'torso':[0,m[1]+span*1.5,entry['width'],max(4,entry['height']*.85-m[1]-span*1.5)],'mouthRatio':float(((r[0]-l[0])*(m[1]-eye[1])-(r[1]-l[1])*(m[0]-eye[0]))/(span*span))}

def inspect(root,entry,cfg):
    width,height=int(entry['width']),int(entry['height'])
    if not 8<=width<=2048 or not 8<=height<=2048:raise ValueError('Invalid canvas dimensions')
    result={k:entry[k] for k in ['key','kind','id','label','width','height']};result.update({'stages':{},'findings':[],'anchors':entry.get('anchors'),'focus':entry.get('focus'),'base_pose':entry.get('base_pose'),'transition_flow':entry.get('transition_flow')})
    def flag(code,stage,detail,frame=0,level='review'):
        result['findings'].append({'code':code,'name':NAMES[code],'stage':stage,'detail':detail,'frame':frame,'level':level})
    roi=regions(entry)
    if roi is None:flag('coverage','still','目・口の登録位置なし。領域ごとの検査は未実施。')
    elif entry.get('anchors',{}).get('visibleMouth',True) and not cfg['mouth_min_eye_ratio']<=roi['mouthRatio']<=cfg['mouth_max_eye_ratio']:
        flag('anchor_geometry','still',f"目の間隔に対する目→口の距離 {roi['mouthRatio']:.2f}。横顔・仰向けなどは正常でも候補になります。")
    hashes={}
    for stage,shots in entry['stages'].items():
        if stage not in ['still','idle','speech','blink','transition']:continue
        if not isinstance(shots,list) or len(shots)>500:raise ValueError('Invalid frame sequence')
        stats={'frames':len(shots),'loaded':0,'shots':shots,'maximumChangedFraction':0,'mouthChangedFraction':None,'eyesChangedFraction':None,'scaleRatio':1,'maximumCenterJump':0,'headBodyRelativeOffset':0};result['stages'][stage]=stats
        arrays=[];geometry=[]
        for i,shot in enumerate(shots):
            try:
                a,digest=load(root,shot,width,height);hashes[shot['file']]=digest;g=box_metrics(a,cfg['ink_threshold'])
                if g is None:flag('empty_frame',stage,'描画された画素がほとんどありません。',i,'error')
                arrays.append(a);geometry.append(g);stats['loaded']+=1
            except Exception as e:
                flag('dimension' if str(e)=='dimension' else 'missing_frame',stage,str(e)[:180],i,'error');arrays.append(None);geometry.append(None)
        if len(shots)<2:
            if stage!='still':flag('coverage',stage,'2コマ未満のため動作を比較できません。')
            continue
        if stats['loaded']!=len(shots):continue
        differences=[changed(a,b,cfg) for a,b in zip(arrays,arrays[1:])];stats['maximumChangedFraction']=round(max(differences,default=0),6)
        if stage in ['idle','speech','blink'] and max(differences,default=0)==0:flag('frozen',stage,'書き出した全コマが同じです。')
        good=[g for g in geometry if g]
        if len(good)==len(geometry):
            ratio=max(g['height'] for g in good)/min(g['height'] for g in good);stats['scaleRatio']=round(ratio,4)
            jumps=[math.hypot(b['x']-a['x'],b['y']-a['y']) for a,b in zip(good,good[1:])];stats['maximumCenterJump']=round(max(jumps,default=0),3)
            if stage in ['idle','speech','blink'] and ratio>cfg['maximum_scale_ratio']:flag('scale_jump',stage,f"輪郭高の最大/最小 {ratio:.3f}（しきい値 {cfg['maximum_scale_ratio']}）。")
            if stage in ['idle','speech','blink'] and stats['maximumCenterJump']>math.hypot(width,height)*cfg['maximum_center_jump_fraction']:flag('position_jump',stage,f"隣接コマの輪郭中心差 {stats['maximumCenterJump']:.1f}px。",jumps.index(max(jumps))+1)
        if roi and stage in ['idle','speech','blink']:
            for part,metric,code in [('mouth','mouthChangedFraction','mouth_static'),('eyes','eyesChangedFraction','eyes_static')]:
                values=[aligned_changed(crop(a,roi[part]),crop(b,roi[part]),cfg) for a,b in zip(arrays,arrays[1:])];valid=[v for v in values if v is not None];stats[metric]=round(max(valid),6) if valid else None
                eye_flags=entry.get('anchors',{}).get('visibleEyes',[True,True]);closed_flags=entry.get('anchors',{}).get('closedEyes',[False,False]);visible=entry.get('anchors',{}).get('visibleMouth',True) if part=='mouth' else any(v and not c for v,c in zip(eye_flags,closed_flags));stats['eyesExpectedClosed']=bool(all(closed_flags))
                if (stage=='speech' and part=='mouth' or stage=='blink' and part=='eyes') and visible:
                    if not valid:flag('coverage',stage,part+'領域の画素が不足し変化を評価できません。')
                    elif max(valid)<cfg['minimum_changed_fraction']:flag(code,stage,f"平行移動補正後の領域変化 {max(valid):.4f}。小さい動き・閉眼・遮蔽は正常でも候補になります。")
            if entry['kind']=='body':
                relative=[]
                for a in arrays:
                    h=centroid(crop(a,roi['head']),cfg['ink_threshold']);t=centroid(crop(a,roi['torso']),cfg['ink_threshold']);relative.append(h-t if h is not None and t is not None else None)
                valid=[v for v in relative if v is not None]
                if len(valid)==len(arrays):
                    offsets=[float(np.linalg.norm(v-valid[0])) for v in valid];stats['headBodyRelativeOffset']=round(max(offsets),3)
                    if max(offsets)>cfg['maximum_head_body_offset_px']*width/320:flag('head_body_drift',stage,f"頭領域と胴体領域の輪郭重心の相対変化 {max(offsets):.1f}px。関節の検出ではありません。",offsets.index(max(offsets)))
        if stage=='transition' and max(differences,default=0)>cfg['maximum_transition_changed_fraction']:
            flag('transition_jump',stage,f"隣接コマの変化画素率 {max(differences):.3f}。通常の全身IDは隣接IDへの人工的な切替です。",differences.index(max(differences))+1)
    for stage in ['still','idle','speech','blink']:
        if stage not in result['stages'] and entry['kind']!='route':flag('coverage',stage,'この動作の書き出しがありません。')
    result['status']='error' if any(f['level']=='error' for f in result['findings']) else 'review' if result['findings'] else 'no_flags'
    return result,hashes

def apply_eye_state(data,path):
    raw=Path(path).read_bytes();state=json.loads(raw.decode('utf-8-sig'))
    if not data.get('apkSha256') or state.get('apkSha256')!=data['apkSha256']:raise ValueError('Eye-state APK hash mismatch')
    for entry in data['entries']:
        if entry['kind']!='body' or not entry.get('anchors'):continue
        flags=state.get('states',{}).get(str(entry['id']))
        if not isinstance(flags,list) or len(flags)!=2 or any(type(f) is not bool for f in flags):raise ValueError('Missing/invalid eye state')
        entry['anchors']['closedEyes']=flags
    return hashlib.sha256(raw).hexdigest()

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--review',required=True,type=Path);parser.add_argument('--output',required=True,type=Path);parser.add_argument('--thresholds',type=Path);parser.add_argument('--eye-state',type=Path,help='Optional same-APK registered eye states for older exports');args=parser.parse_args();root=args.review.resolve();out=args.output.resolve();cfg=settings(args.thresholds)
    if out.exists() and any(out.iterdir()):parser.error('Output directory must be new or empty')
    manifest=root/'manifest.json';raw=manifest.read_bytes()
    if len(raw)>20_000_000:raise ValueError('Oversize manifest')
    data=json.loads(raw.decode('utf-8-sig'))
    if data.get('schema')!=1 or not isinstance(data.get('entries'),list) or not data['entries'] or len(data['entries'])>1000:raise ValueError('Unsupported manifest')
    if len({e['key'] for e in data['entries']})!=len(data['entries']):raise ValueError('Duplicate entry key')
    eye_state_hash=apply_eye_state(data,args.eye_state) if args.eye_state else None
    results=[];hashes={}
    for i,entry in enumerate(data['entries']):
        row,digests=inspect(root,entry,cfg);results.append(row);hashes.update(digests)
        if (i+1)%50==0:print(f'Audited {i+1}/{len(data["entries"])}',flush=True)
    counts={k:sum(r['status']==k for r in results) for k in ['error','review','no_flags']}
    result={'schema':1,'createdAt':datetime.now(timezone.utc).isoformat(),'version':data.get('version'),'apkSha256':data.get('apkSha256'),'manifestSha256':hashlib.sha256(raw).hexdigest(),'eyeStateSha256':eye_state_hash,'renderer':data.get('renderer'),'host':data.get('host'),'selection':data.get('selection'),'thresholds':cfg,'counts':counts,'exportErrors':data.get('errors',[]),'entries':results,'frameHashes':hashes,'frameBase':Path(os.path.relpath(root,out)).as_posix()+'/','limitations':'数値による要確認候補です。検出なしは自然さの合格判定ではありません。登録ROIと輪郭比較を使い、骨格・腕の本数・解剖学的な首・表情の意味は判定しません。静止位置基準の領域なので大きな動き・横顔・閉眼では誤検出や見逃しがあります。'}
    out.mkdir(parents=True,exist_ok=True);encoded=json.dumps(result,ensure_ascii=False,indent=2);(out/'audit.json').write_text(encoded,encoding='utf-8');(out/'audit-data.js').write_text('window.ANIMATION_AUDIT='+encoded.replace('<','\\u003c')+';',encoding='utf-8');(out/'thresholds.json').write_text(json.dumps(cfg,indent=2),encoding='utf-8')
    with (out/'findings.csv').open('w',encoding='utf-8-sig',newline='') as f:
        writer=csv.writer(f);writer.writerow(['key','kind','id','label','stage','code','level','frame','detail'])
        for row in results:
            for flag in row['findings']:writer.writerow([row['key'],row['kind'],row['id'],row['label'],flag['stage'],flag['code'],flag['level'],flag['frame'],flag['detail']])
    for name in ['audit.html','audit.js','audit.css']:shutil.copy2(Path(__file__).parent/name,out/name)
    print(json.dumps({'entries':len(results),'frames':len(hashes),'counts':counts,'exportErrors':len(result['exportErrors']),'report':str(out/'audit.html'),'externalRequests':0},ensure_ascii=False))
    return 2 if counts['error'] or result['exportErrors'] else 0
if __name__=='__main__':sys.exit(main())
