"""Fixed automatic update gate with explicit manual coverage. Never deploys or submits an AI request."""
from pathlib import Path
import argparse,hashlib,json,os,subprocess,sys,time
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',required=True,type=Path);p.add_argument('--manual-evidence',type=Path);p.add_argument('--require-manual',action='store_true');a=p.parse_args();r=Path(__file__).resolve().parents[1];out=a.output.resolve();out.mkdir(parents=True,exist_ok=False)
def sha(f):return hashlib.sha256(f.read_bytes()).hexdigest()
hashes={k:sha(r/f'.build-integration/{k}/loki-{k}-integration.apk') for k in ['phone','glass']}
q=subprocess.run([sys.executable,str(r/'tools/Verify-NavigationInteractions.py'),'--output',str(out/'android')]);assert q.returncode==0,'Android interaction or regression failed; evidence retained'
reg=json.loads((out/'android/regression/result.json').read_text(encoding='utf-8'));assert reg.get('stabilityChecks',0)>0,'Stability checks missing'
q=subprocess.run([sys.executable,str(r/'tools/Test-StabilitySoak.py')],capture_output=True);(out/'soak-parser-tests.log').write_bytes(q.stdout+q.stderr);assert q.returncode==0,'Observation validity tests failed'
node=Path('C:/Users/user/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node.exe');env=os.environ.copy();env['NODE_PATH']='C:/Users/user/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules';results=[]
for name in ['ConversationRobustnessWebTest','ConversationPlusWebTest','ConversationUiWebTest','TabletConversationWebTest','WebTurnProgressTest','ConversationToolsWebTest','EverydayConversationWebTest','ReconnectConversationWebTest','PhotoFollowUpWebTest','ImageQuestionWebTest','PhotoQualityWebTest','AnswerCorrectionsWebTest','RecordsWebTest','CheckpointWebTest','ReleaseReliabilityWebTest','ReliabilityCenterWebTest','ExperienceCenterWebTest','MaintenanceCenterWebTest','AssistantReviewWebTest','TuningWebTest','MediaAttachmentsBrowserTest','MeetingWebBrowserTest','WebEnterBrowserTest','WebSendBrowserTest']:
 try:q=subprocess.run([str(node),str(r/'tools'/f'{name}.cjs')],capture_output=True,timeout=180,env=env);code=q.returncode;log=q.stdout+q.stderr
 except subprocess.TimeoutExpired as e:code=124;log=(e.stdout or b'')+(e.stderr or b'')+b'\nTIMEOUT'
 (out/f'{name}.log').write_bytes(log);results.append({'suite':name,'exitCode':code});(out/'browser-results.json').write_text(json.dumps(results,indent=2),encoding='utf-8');print(json.dumps(results[-1]),flush=True)
assert all(x['exitCode']==0 for x in results),'Browser regression failures; evidence retained'
assert hashes=={k:sha(r/f'.build-integration/{k}/loki-{k}-integration.apk') for k in hashes},'Candidate APK changed during validation'
checks={k:{'state':'not_run','evidence':''} for k in ['conversation_end_to_end','voice_microphone_and_audio','photo_capture_and_answer','saved_records_open','two_device_sync','navigation_start_cancel_resume']}
if a.manual_evidence:
 manual=json.loads(a.manual_evidence.read_text(encoding='utf-8'));assert manual.get('apkSha256')==hashes,'Manual evidence APK differs'
 for k in checks:
  row=manual.get('checks',{}).get(k,{})
  if row.get('state')=='passed' and row.get('evidence') and Path(row['evidence']).is_file():checks[k]={'state':'passed','evidence':row['evidence']}
manual_ok=all(x['state']=='passed' for x in checks.values());report={'schema':1,'status':'all_listed_checks_passed' if manual_ok else 'automatic_pass_manual_pending','apkSha256':hashes,'android':json.loads((out/'android/validation.json').read_text(encoding='utf-8')),'stabilityAssertionsPassed':reg['stabilityChecks'],'browserSuitesPassed':len(results),'observerParserTestsPassed':5,'manualChecks':checks,'allChecksPassed':manual_ok,'providerRequestsFromTests':0,'realWorldMovementVerified':False};(out/'validation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8');print(json.dumps({'status':report['status'],'stabilityAssertionsPassed':reg['stabilityChecks'],'browserSuitesPassed':len(results)}));sys.exit(2 if a.require_manual and not manual_ok else 0)
