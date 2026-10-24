from pathlib import Path
import importlib.util,unittest
spec=importlib.util.spec_from_file_location('soak',Path(__file__).with_name('Run-StabilitySoak.py'));soak=importlib.util.module_from_spec(spec);spec.loader.exec_module(soak)
class Checks(unittest.TestCase):
 def test_pss(self):
  self.assertEqual(soak.pss('TOTAL PSS: 123'),123);self.assertEqual(soak.pss(' TOTAL 456 0'),456);self.assertIsNone(soak.pss('unavailable'))
 def test_metrics(self):
  self.assertIsNone(soak.counters('')['networkAttempts']);self.assertIsNone(soak.counters('<invalid')['sampleElapsedMs']);self.assertEqual(soak.counters('<map><long name="networkAttempts" value="17"/></map>')['networkAttempts'],17);self.assertIsNone(soak.counters('<map><string name="session">PRIVATE</string></map>')['session'])
 def test_battery(self):
  self.assertEqual(soak.battery(' level: 91\n temperature: 351\n USB powered: true')['temperature'],351);self.assertIsNone(soak.battery('')['level']);self.assertTrue(soak.battery(' USB powered: true')['USB powered'])
 def test_offline_is_not_stopped(self):
  original=soak.read
  try:
   soak.read=lambda *a:None;self.assertEqual(soak.observe('adb',{'serial':'synthetic','package':'p'})['state'],'unreachable')
   replies=iter(['device',None,'device',None]);soak.read=lambda *a:next(replies);self.assertEqual(soak.observe('adb',{'serial':'synthetic','package':'p'})['state'],'process_unconfirmed')
   replies=iter(['device',None,'device',' PID NAME\n 123 other']);soak.read=lambda *a:next(replies);self.assertEqual(soak.observe('adb',{'serial':'synthetic','package':'p'})['state'],'not_running')
  finally:soak.read=original
 def test_observation_gaps(self):
  rows=[{'elapsedSeconds':0,'phone':{'state':'running','pid':1,'pssKiB':10},'glass':{'state':'unreachable'}},{'elapsedSeconds':300,'phone':{'state':'running','pid':2,'pssKiB':20},'glass':{'state':'not_running'}}];s=soak.summary(rows,60);self.assertEqual(s['hostSampleGaps'],1);self.assertEqual(s['phone']['observedPidChanges'],1);self.assertEqual(s['glass']['unreachableSamples'],1);self.assertIsNone(s['glass']['maxPssKiB'])
if __name__=='__main__':unittest.main()
