"""Synthetic regressions for audit rules; no devices, capture, API or source artwork changes."""
import importlib.util,json,tempfile,unittest
from pathlib import Path
from PIL import Image,ImageDraw
spec=importlib.util.spec_from_file_location('animation_audit',Path(__file__).with_name('audit.py'));audit=importlib.util.module_from_spec(spec);spec.loader.exec_module(audit)
class AuditTest(unittest.TestCase):
 def setUp(self):
  self.temp=tempfile.TemporaryDirectory();self.root=Path(self.temp.name);(self.root/'frames').mkdir();self.cfg=audit.settings();self.entry={'key':'body-0','kind':'body','id':0,'label':'fixture','width':160,'height':240,'anchors':{'leftEye':[65,55],'rightEye':[95,55],'mouth':[80,80],'visibleMouth':True,'visibleEyes':[True,True]},'stages':{}}
 def tearDown(self):self.temp.cleanup()
 def frame(self,name,dx=0,scale=1,mouth=0,blank=False):
  im=Image.new('RGB',(160,240));d=ImageDraw.Draw(im)
  if not blank:
   d.ellipse((50+dx,20,110+dx,105),outline='white',width=2);d.line((60+dx,55,70+dx,55),fill='white',width=2);d.line((90+dx,55,100+dx,55),fill='white',width=2);d.ellipse((73+dx,78-mouth,87+dx,82+mouth),outline='white',width=2);d.rectangle((50,115,110,115+100*scale),outline='white',width=3)
  name='frames/'+name+'.png';im.save(self.root/name);return {'file':name,'time':0}
 def inspect(self):return audit.inspect(self.root,self.entry,self.cfg)[0]
 def codes(self,row):return [f['code'] for f in row['findings']]
 def test_frozen(self):
  f=self.frame('same');self.entry['stages']={'speech':[f,f],'blink':[f,f],'idle':[f,f]};self.assertIn('frozen',self.codes(self.inspect()));self.assertIn('mouth_static',self.codes(self.inspect()))
 def test_mouth_motion(self):
  self.entry['stages']={'speech':[self.frame('one'),self.frame('two',mouth=5)]};self.assertNotIn('mouth_static',self.codes(self.inspect()));self.assertGreater(self.inspect()['stages']['speech']['mouthChangedFraction'],.002)
 def test_head_drift(self):
  self.entry['stages']={'idle':[self.frame('one'),self.frame('two',dx=16)]};self.assertIn('head_body_drift',self.codes(self.inspect()))
 def test_scale(self):
  self.entry['stages']={'idle':[self.frame('one',scale=.3),self.frame('two')]};self.assertIn('scale_jump',self.codes(self.inspect()))
 def test_missing(self):
  self.entry['stages']={'speech':[self.frame('one'),{'file':'frames/absent.png','time':100}]};self.assertEqual(self.inspect()['status'],'error');self.assertIn('missing_frame',self.codes(self.inspect()))
 def test_blank(self):
  f=self.frame('blank',blank=True);self.entry['stages']={'still':[f]};self.assertIn('empty_frame',self.codes(self.inspect()))
 def test_dimensions(self):
  f=self.frame('one');self.entry['width']=159;self.entry['stages']={'still':[f]};self.assertIn('dimension',self.codes(self.inspect()))
 def test_traversal(self):
  for path in ['../frames/a.png','frames/../../a.png','https://example.com/a.png','frames/a.png?x=1','C:/tmp/a.png']:
   with self.assertRaises(ValueError):audit.frame_path(self.root,path)
 def test_invalid_settings(self):
  for value in [{'unknown':1},{'ink_threshold':float('nan')},{'maximum_scale_ratio':0},{'minimum_changed_fraction':2}]:
   p=self.root/'thresholds.json';p.write_text(json.dumps(value))
   with self.assertRaises(ValueError):audit.settings(p)
 def test_registered_mouth_above_eyes(self):
  self.entry['anchors']['mouth']=[80,20];self.entry['stages']={'still':[self.frame('one')]};self.assertIn('anchor_geometry',self.codes(self.inspect()))
 def test_no_anchors_is_unknown(self):
  self.entry.pop('anchors');self.entry['stages']={'still':[self.frame('one')]};self.assertIn('coverage',self.codes(self.inspect()));self.assertNotEqual(self.inspect()['status'],'no_flags')
 def test_one_frame_not_motion_pass(self):
  self.entry['stages']={'idle':[self.frame('one')]};self.assertIn('coverage',self.codes(self.inspect()))
 def test_invisible_mouth(self):
  self.entry['anchors']['visibleMouth']=False;f=self.frame('one');self.entry['stages']={'speech':[f,f]};self.assertNotIn('mouth_static',self.codes(self.inspect()))
 def test_repeatable_metrics(self):
  self.entry['stages']={'speech':[self.frame('one'),self.frame('two',mouth=5)]};self.assertEqual(self.inspect(),self.inspect())
 def test_hash_changes(self):
  f=self.frame('one');self.entry['stages']={'still':[f]};a=audit.inspect(self.root,self.entry,self.cfg)[1];self.frame('one',mouth=5);b=audit.inspect(self.root,self.entry,self.cfg)[1];self.assertNotEqual(a,b)
 def test_closed_eyes_are_not_missing_blinks(self):
  self.entry['anchors']['closedEyes']=[True,True];f=self.frame('one');self.entry['stages']={'blink':[f,f]};self.assertNotIn('eyes_static',self.codes(self.inspect()));self.assertTrue(self.inspect()['stages']['blink']['eyesExpectedClosed'])
 def test_one_open_eye_still_checked(self):
  self.entry['anchors']['closedEyes']=[True,False];f=self.frame('one');self.entry['stages']={'blink':[f,f]};self.assertIn('eyes_static',self.codes(self.inspect()))
 def test_eye_state_hash_guard(self):
  data={'apkSha256':'a','entries':[self.entry]};path=self.root/'states.json';path.write_text(json.dumps({'apkSha256':'b','states':{'0':[True,True]}}))
  with self.assertRaises(ValueError):audit.apply_eye_state(data,path)
 def test_eye_state_legacy_export(self):
  data={'apkSha256':'a','entries':[self.entry]};path=self.root/'states.json';path.write_text(json.dumps({'apkSha256':'a','states':{'0':[True,True]}}));digest=audit.apply_eye_state(data,path);self.assertEqual(len(digest),64);self.assertEqual(self.entry['anchors']['closedEyes'],[True,True])
class ExtractionTest(unittest.TestCase):
 def test_rejects_unsafe_members(self):
  import io,tarfile
  from export_transfer import extract_and_finish
  for name,kind in [('../outside','file'),('/tmp/a','file'),('frames/symlink.png','symlink'),('unexpected.txt','file')]:
   with tempfile.TemporaryDirectory() as tmp:
    root=Path(tmp);archive=root/'a.tar';out=root/'output';out.mkdir()
    with tarfile.open(archive,'w') as tar:
     item=tarfile.TarInfo(name);item.size=1
     if kind=='symlink':item.type=tarfile.SYMTYPE;item.linkname='../outside'
     tar.addfile(item,io.BytesIO(b'x'))
    with self.assertRaises(ValueError):extract_and_finish(archive,out,Path(__file__).parent,'fixture')
 def test_rejects_oversize(self):
  import io,tarfile
  from export_transfer import extract_and_finish
  with tempfile.TemporaryDirectory() as tmp:
   root=Path(tmp);archive=root/'a.tar';out=root/'output';out.mkdir()
   with tarfile.open(archive,'w') as tar:
    item=tarfile.TarInfo('frames/huge.png');item.size=8_000_001;tar.addfile(item,io.BytesIO(bytes(item.size)))
   with self.assertRaises(ValueError):extract_and_finish(archive,out,Path(__file__).parent,'fixture')
if __name__=='__main__':unittest.main()
