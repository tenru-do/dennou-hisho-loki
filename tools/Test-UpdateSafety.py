"""No device, network or user files. Destructive update entry must fail before mutation."""
import importlib.util,unittest,tempfile,hashlib,sys,subprocess
from pathlib import Path
from unittest.mock import patch
HERE=Path(__file__).parent
def module(name,file):
 spec=importlib.util.spec_from_file_location(name,HERE/file);value=importlib.util.module_from_spec(spec);spec.loader.exec_module(value);return value
installer=module('loki_verified_installer','Install-LokiVerified.py');cleanup=module('loki_backup_archive','Archive-GlassUpdateBackups.py')
class SafetyTest(unittest.TestCase):
 def test_only_exact_backup_names(self):
  root=cleanup.ROOT+'/';self.assertTrue(cleanup.eligible(root+'loki-pre-v521-20261014-backup.zip'))
  for name in ['../loki-pre-v521-backup.zip','nested/loki-pre-v521-backup.zip','loki-pre-v521-backup.zip;rm','loki-pre-$(command)-backup.zip','recording.zip','loki-pre-v521-backup.zip\n','loki-pre-v521-backup.zip/extra']:
   with self.subTest(name=name):self.assertFalse(cleanup.eligible(root+name))
  self.assertFalse(cleanup.eligible('/sdcard/Pictures/loki-pre-v521-backup.zip'))
 def test_bad_hash_never_contacts_device(self):
  with tempfile.TemporaryDirectory() as temp:
   apk=Path(temp)/'new.apk';apk.write_bytes(b'fixture')
   argv=['install','--app','glass','--adb','unused','--serial','FIXTURE','--sdk',temp,'--java','unused','--apk',str(apk),'--expected-sha256','0'*64,'--output',str(Path(temp)/'out')]
   with patch.object(sys,'argv',argv),patch.object(installer.subprocess,'run') as calls:
    with self.assertRaisesRegex(RuntimeError,'hash mismatch'):installer.main()
    calls.assert_not_called();self.assertFalse((Path(temp)/'out').exists())
 def test_wrong_device_or_active_capture_never_mutates(self):
  for model,service,reason in [('SM-S942Q','','Wrong device'),('RG-glasses','ServiceRecord MeetingRecorderService','Stop user recording'),('RG-glasses','ServiceRecord PhotoRecorderService','Stop user recording')]:
   with self.subTest(model=model,service=service),tempfile.TemporaryDirectory() as temp:
    apk=Path(temp)/'new.apk';apk.write_bytes(b'fixture');args=['install','--app','glass','--adb','unused','--serial','FIXTURE','--sdk',temp,'--java','unused','--apk',str(apk),'--expected-sha256',hashlib.sha256(b'fixture').hexdigest(),'--output',str(Path(temp)/'out')]
    def fake(command,**kw):return subprocess.CompletedProcess(command,0,(model if 'getprop' in command else service).encode(),b'')
    with patch.object(sys,'argv',args),patch.object(installer.subprocess,'run',side_effect=fake) as calls:
     with self.assertRaisesRegex(RuntimeError,reason):installer.main()
     self.assertFalse((Path(temp)/'out').exists());self.assertTrue(all('install' not in call.args[0] and 'force-stop' not in call.args[0] and 'rm' not in call.args[0] for call in calls.call_args_list))
 def test_installed_path_rejects_shell_characters(self):
  self.assertTrue(installer.APK_PATH.fullmatch('/data/app/~~abc==/com.example.app-abc==/base.apk'))
  for path in ['/sdcard/base.apk','/data/app/a/base.apk;cmd','/data/app/a/base.apk\n','/data/app/a/other.apk']:
   self.assertIsNone(installer.APK_PATH.fullmatch(path))
if __name__=='__main__':unittest.main()
