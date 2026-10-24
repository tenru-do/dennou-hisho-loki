"""Read-only 12-hour device observation. No workload, provider calls, capture or recovery actions."""
from pathlib import Path
import argparse,datetime,json,re,subprocess,time,xml.etree.ElementTree as ET

def read(adb,serial,*args):
 try:
  q=subprocess.run([str(adb),'-s',serial,*args],capture_output=True,text=True,encoding='utf-8',errors='replace',timeout=12)
  return q.stdout if q.returncode==0 else None
 except (OSError,subprocess.TimeoutExpired):return None

def pss(text):
 m=re.search(r'TOTAL PSS:\s*(\d+)',text or '') or re.search(r'^\s*TOTAL\s+(\d+)\s',text or '',re.M)
 return int(m[1]) if m else None

def battery(text):
 out={}
 for key in ['level','temperature','status']:
  m=re.search(r'^\s*'+key+r':\s*(-?\d+)',text or '',re.M);out[key]=int(m[1]) if m else None
 for key in ['AC powered','USB powered','Wireless powered']:
  m=re.search(r'^\s*'+key+r':\s*(true|false)',text or '',re.M);out[key]=m[1]=='true' if m else None
 return out

def counters(text):
 try:
  rows={x.attrib.get('name'):x for x in ET.fromstring(text or '')};out={}
  for key in ['networkAttempts','sampleElapsedMs']:
   e=rows.get(key);out[key]=max(0,int(e.attrib['value'])) if e is not None else None
  e=rows.get('session');v=e.text if e is not None else '';out['session']=v if re.fullmatch(r'[a-f0-9-]{36}',v or '') else None
  return out
 except (ET.ParseError,ValueError,KeyError):return {'networkAttempts':None,'sampleElapsedMs':None,'session':None}

def observe(adb,device):
 serial= device['serial'];package=device['package'];state=read(adb,serial,'get-state')
 if state is None or state.strip()!='device':return {'state':'unreachable'}
 raw=read(adb,serial,'shell','pidof',package);ids=(raw or '').strip().split()
 if raw is None or not ids:
  again=read(adb,serial,'get-state')
  if not again or again.strip()!='device':return {'state':'unreachable'}
  processes=read(adb,serial,'shell','ps','-A','-o','PID,NAME')
  if processes is None:return {'state':'process_unconfirmed'}
  found=re.search(r'^\s*(\d+)\s+'+re.escape(package)+r'\s*$',processes,re.M)
  if not found:return {'state':'not_running'}
  ids=[found[1]]
 if not all(x.isdigit() for x in ids):return {'state':'process_unconfirmed'}
 pid=ids[0];packageInfo=read(adb,serial,'shell','dumpsys','package',package);versionMatch=re.search(r'\bversionCode=(\d+)',packageInfo or '');version=int(versionMatch[1]) if versionMatch else None;memory=pss(read(adb,serial,'shell','dumpsys','meminfo',pid));b=battery(read(adb,serial,'shell','dumpsys','battery'));metrics=counters(read(adb,serial,'exec-out','run-as',package,'cat','shared_prefs/loki_stability_metrics.xml'));uptime=read(adb,serial,'shell','cat','/proc/uptime');age=None
 try:
  if metrics.get('sampleElapsedMs') is not None:age=max(0,int(float(uptime.split()[0])*1000)-metrics['sampleElapsedMs'])
 except (ValueError,AttributeError,IndexError):pass
 logs=read(adb,serial,'logcat','-d','--pid='+pid,'-t','300','-v','brief');cpu=read(adb,serial,'shell','dumpsys','cpuinfo');usage=None
 if cpu:
  m=re.search(r'^\s*([0-9.]+)%\s+'+pid+r'/',cpu,re.M);usage=float(m[1]) if m else None
 return {'state':'running','versionCode':version,'pid':int(pid),'pssKiB':memory,'battery':b,'instrumentedCounters':metrics,'activityMetricsAgeMs':age,'cpuPercentInDumpsysWindow':usage,'fatalInCurrentLogWindow':None if logs is None else 'FATAL EXCEPTION' in logs}

def summary(rows,interval):
 out={}
 for kind in ['phone','glass']:
  records=[r[kind] for r in rows];values=[r['pssKiB'] for r in records if r.get('pssKiB') is not None];pids=[r['pid'] for r in records if r.get('pid') is not None];temperatures=[r['battery']['temperature']/10 for r in records if r.get('battery',{}).get('temperature') is not None]
  out[kind]={'observations':len(records),'runningSamples':sum(r['state']=='running' for r in records),'unreachableSamples':sum(r['state']=='unreachable' for r in records),'notRunningSamples':sum(r['state']=='not_running' for r in records),'observedPidChanges':sum(a is not None and b is not None and a!=b for a,b in zip(pids,pids[1:])),'firstPssKiB':values[0] if values else None,'lastPssKiB':values[-1] if values else None,'maxPssKiB':max(values) if values else None,'maxBatteryTemperatureC':max(temperatures) if temperatures else None,'fatalWindows':sum(r.get('fatalInCurrentLogWindow') is True for r in records)}
 out['hostSampleGaps']=sum(b['elapsedSeconds']-a['elapsedSeconds']>interval*2.5 for a,b in zip(rows,rows[1:]));return out

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--handoff',required=True,type=Path);p.add_argument('--output',required=True,type=Path);p.add_argument('--hours',type=float,default=12);p.add_argument('--interval',type=int,default=60);p.add_argument('--stop-file',type=Path);a=p.parse_args()
 if not 0<a.hours<=24 or not 30<=a.interval<=300:p.error('hours (0,24], interval [30,300] required')
 if a.output.exists():raise SystemExit('Existing report retained; use a fresh output path')
 cfg=json.loads(a.handoff.read_text(encoding='utf-8-sig'));devices={d['kind']:d for d in cfg['devices'] if d['kind'] in ['phone','glass']};assert set(devices)=={'phone','glass'};adb=Path('C:/Users/user/AppData/Local/Android/Sdk/platform-tools/adb.exe');start=time.monotonic();deadline=start+a.hours*3600;rows=[];started=datetime.datetime.now(datetime.timezone.utc).isoformat();a.output.parent.mkdir(parents=True,exist_ok=True)
 while True:
  row={'elapsedSeconds':round(time.monotonic()-start,1)}
  for kind in ['phone','glass']:row[kind]=observe(adb,devices[kind])
  rows.append(row);elapsed=time.monotonic()-start;stopped=a.stop_file is not None and a.stop_file.exists();done=elapsed>=a.hours*3600
  report={'schema':1,'startedAtUtc':started,'requestedHours':a.hours,'intervalSeconds':a.interval,'elapsedSeconds':round(elapsed,1),'completed':done,'stoppedByUser':stopped,'status':'completed' if done else 'stopped' if stopped else 'running','scope':'read-only observation during normal use, no injected workload; not a crash-free or real-route guarantee; charging and USB conditions retained','providerCalls':0,'captures':0,'summary':summary(rows,a.interval),'samples':rows};temp=a.output.with_suffix('.tmp');temp.write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8');temp.replace(a.output)
  if stopped or done:break
  time.sleep(min(a.interval,max(0,deadline-time.monotonic())))
 print(json.dumps({'status':report['status'],'elapsedSeconds':report['elapsedSeconds'],'observations':len(rows)}))
if __name__=='__main__':main()
