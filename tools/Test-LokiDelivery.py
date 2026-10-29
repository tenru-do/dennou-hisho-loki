from pathlib import Path
import tempfile,importlib.util,json
spec=importlib.util.spec_from_file_location('delivery',Path(__file__).with_name('Prepare-LokiDelivery.py'));m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
with tempfile.TemporaryDirectory() as t:
 p=Path(t);apk=p/'a.apk';apk.write_bytes(b'fixture');source=p/'Main.java';source.write_text('v1');out=p/'out'
 first=m.prepare(apk,out,'glass',663,[source]);assert not first['installed'];assert first['plan']['photoVerification']=='metadata'
 try:m.prepare(apk,out,'glass',663,[source],out/'loki-glass-v663.json');raise AssertionError('unverified manifest reused')
 except ValueError:pass
 log=p/'checks.log';log.write_text('fixture checks passed');evidence=dict(first,validation={'state':'passed','artifacts':[{'path':str(log),'sha256':m.sha(log)}]});ev=p/'evidence.json';ev.write_text(json.dumps(evidence))
 again=m.prepare(apk,out,'glass',663,[source],ev);assert again['evidenceReusable']
 source.write_text('v2')
 try:m.prepare(apk,out,'glass',663,[source],out/'loki-glass-v663.json');raise AssertionError('stale evidence accepted')
 except ValueError:pass
 apk.write_bytes(b'new')
 try:m.prepare(apk,out,'glass',663,[source]);raise AssertionError('immutable APK replaced')
 except ValueError:pass
 assert m.plan(['PhotoImportRequests.java'])['photoVerification']=='content-hash'
 assert m.plan(['CharacterPerformance.java'])['photoVerification']=='metadata'
 print('PASS immutable copy, source drift, APK mismatch, evidence binding, scoped photo verification')
