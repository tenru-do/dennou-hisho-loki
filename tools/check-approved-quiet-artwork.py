"""Verify approved originals and geometry; generate a side-by-side human review board.
No image changes, inference API, private app data, or automatic identity approval.
"""
from pathlib import Path
import argparse, hashlib, html, json, os, re, struct

def main():
    root = Path(__file__).resolve().parent.parent
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--out', type=Path, default=root/'artwork/reviews/quiet-poses-v703')
    args = parser.parse_args()
    manifest_path=root/'artwork/adopted/quiet-poses-runtime-v712.json'
    if not manifest_path.exists():manifest_path=root/'artwork/adopted/quiet-poses-runtime-v709.json'
    if not manifest_path.exists():manifest_path=root/'artwork/adopted/quiet-poses-runtime-v702.json'
    manifest = json.loads(manifest_path.read_text(encoding='utf-8'))
    text = (root/'glass-app/src/com/example/rokidkeyboardbridge/ApprovedQuietPosePolicy.java').read_text(encoding='utf-8')
    names = re.findall(r'"(loki_approved_[^"]+\.png)"', text)
    points = [[float(v.rstrip('f')) for v in row.split(',')] for row in re.findall(r'\{([0-9.f,\-]+)\}', text.split('FACE=')[1])]
    errors = []
    rows = []
    assert len(names) == len(points) == len(manifest['assets'])
    for index, item in enumerate(manifest['assets']):
        assert item['asset'] == names[index]
        source = (root/item['source']).resolve()
        asset = (root/'glass-app/assets'/item['asset']).resolve()
        assert source.is_relative_to(root) and asset.is_relative_to(root/'glass-app/assets')
        data = asset.read_bytes()
        assert data[:8] == b'\x89PNG\r\n\x1a\n'
        width, height = struct.unpack('>II', data[16:24])
        digest = hashlib.sha256(data).hexdigest()
        unchanged = digest == item['sha256'] == hashlib.sha256(source.read_bytes()).hexdigest()
        f = points[index]
        valid = f[:2] == [width, height] and all(0 < f[x] < width and 0 < f[y] < height for x,y in [(2,3),(8,9),(14,15)])
        valid = valid and all(f[x]-f[w] > 0 and f[x]+f[w] < width for x,w in [(2,4),(8,10),(14,16)])
        # A review metric, not a claim to estimate full skeletal proportions from pixels.
        span = (abs(f[8]-f[2])+f[4]+f[10])/height
        if not unchanged or not valid:
            errors.append(item['asset'])
        rows.append(dict(asset=item['asset'],source=item['source'],width=width,height=height,sha256=digest,originalUnchanged=unchanged,landmarksWithinImage=valid,eyeSpanToCanvasHeight=round(span,4),manualIdentityReviewRequired=True,closeupNotAutomatic=index==5))
    reference = root/'artwork/reference/loki-user-approved-base-20261027.png'
    args.out.mkdir(parents=True, exist_ok=True)
    report = dict(schema=1,checks='provenance, PNG dimensions, landmark bounds; human comparison required',passed=not errors,errors=errors,reference='artwork/reference/'+reference.name,assets=rows)
    (args.out/'report.json').write_text(json.dumps(report, ensure_ascii=False, indent=2),encoding='utf-8')
    def src(p):return html.escape(os.path.relpath(p,args.out).replace('\\','/'),quote=True)
    cards=''.join('<article><h2>'+html.escape(x['asset'])+'</h2><div class="pair"><img src="'+src(reference)+'" alt="User approved standing master"><img src="'+src(root/'glass-app/assets'/x['asset'])+'" alt="Candidate original"></div><p>'+str(x['width'])+' × '+str(x['height'])+' · 原本一致 '+str(x['originalUnchanged'])+' · 目の幅／画像高 '+str(x['eyeSpanToCanvasHeight'])+'</p><p>顔・頭・肩・手・脚を比較。'+('寄り構図：自動巡回の対象外。' if x['closeupNotAutomatic'] else '')+'人の確認後に採用判断する。</p><label><input type="checkbox">顔・体格・画角を目視確認（このチェックは保存されません）</label></article>' for x in rows)
    page='<!doctype html><html lang="ja"><meta charset="utf-8"><title>ロキ画像の比較確認</title><style>body{margin:24px;background:#10151d;color:#eef2f6;font-family:system-ui}main{display:grid;grid-template-columns:repeat(auto-fit,minmax(340px,1fr));gap:20px}article{background:#1c2532;padding:16px;border-radius:12px}h2{font-size:15px;overflow-wrap:anywhere}.pair{display:flex;background:#000;height:420px}.pair img{width:50%;height:100%;object-fit:contain}p,label{font-size:13px;line-height:1.6}</style><h1>ロキ：基準原本との比較</h1><p>左＝指定基準、右＝追加画像。比率の数値は画角を含む参考値です。同一人物かどうかは自動判定していません。頭を切る、手・上半身が大きくなる、顔が幼くなる等を目視で確認します。</p><main>'+cards+'</main></html>'
    (args.out/'review.html').write_text(page,encoding='utf-8')
    print(json.dumps(dict(passed=not errors,images=len(rows),errors=errors,output=str(args.out))))
    if errors:raise SystemExit(1)

if __name__=='__main__':main()
