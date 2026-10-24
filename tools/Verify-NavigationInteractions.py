"""Run real-dialog checks before legacy regressions, retaining hash-matched evidence. No install of Loki."""
from pathlib import Path
import argparse,json,subprocess,sys,hashlib
p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);a=p.parse_args();r=Path(__file__).resolve().parents[1];dest=a.output.resolve();dest.mkdir(parents=True,exist_ok=False)
def sha(file):return hashlib.sha256(file.read_bytes()).hexdigest()
inputs={kind:sha(r/f'.build-integration/{kind}/loki-{kind}-integration.apk') for kind in ['phone','glass']}
for name,tool in [('interactions','navigation-flow-ui/run.py'),('regression','review-device/run.py')]:
 q=subprocess.run([sys.executable,str(r/'tools'/tool),'--output',str(dest/name)]);assert q.returncode==0,name+' failed; retain diagnostics'
ui=json.loads((dest/'interactions/result.json').read_text(encoding='utf-8'));checks=json.loads((dest/'regression/result.json').read_text(encoding='utf-8'));assert ui['failed']==0 and checks['navigationModeMatrix']['failed']==0
for kind,key in [('phone','sourceApkSha256'),('glass','glassApkSha256')]:assert ui[key]==checks[key]==inputs[kind]==sha(r/f'.build-integration/{kind}/loki-{kind}-integration.apk'),'APK changed during validation'
report={'interactionCasesPassed':ui['passed'],'regressionAssertionsPassed':checks['passed']+checks['navigationModeMatrix']['passed'],'apkSha256':inputs,'liveRouteQueries':0,'realMovementVerified':False};(dest/'validation.json').write_text(json.dumps(report,indent=2),encoding='utf-8');print(json.dumps(report))
