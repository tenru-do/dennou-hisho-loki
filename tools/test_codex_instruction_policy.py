import unittest
from codex_instruction_policy import VisibleThread, Unavailable, bind_visible, queue_bound

class BindingTest(unittest.TestCase):
    def setUp(self):
        self.id='123e4567-e89b-42d3-a456-426614174000'
        self.now=10_000
        self.calls=[]
        self.snapshot=VisibleThread(self.id,'ロキの改修',self.now,'window-1')
    def rpc(self,method,params):
        self.calls.append((method,params))
        return {'thread':{'id':self.id,'name':'ロキの改修'}}
    def test_title_announced_and_destination_frozen(self):
        request=bind_visible(self.snapshot,'追加質問の文脈を直して',self.now,self.rpc)
        self.assertIn('ロキの改修',request.announcement)
        self.snapshot=VisibleThread('123e4567-e89b-42d3-a456-426614174001','別の会話',self.now,'window-1')
        queue_bound(request,self.now+2000,self.rpc)
        method,params=self.calls[-1]
        self.assertEqual(method,'thread/queue/add')
        self.assertEqual(params['threadId'],self.id)
        self.assertEqual(params['clientUserMessageId'],request.request_id)
        self.assertEqual(params['input'][0]['text'],request.text)
        self.assertNotIn('approvalPolicy',params)
    def test_no_selected_ui_no_rpc(self):
        with self.assertRaises(Unavailable):bind_visible(None,'指示',self.now,self.rpc)
        self.assertEqual(self.calls,[])
    def test_stale_or_future_ui(self):
        for at in [3999,10001]:
            with self.assertRaises(Unavailable):bind_visible(VisibleThread(self.id,'ロキの改修',at,'window-1'),'指示',self.now,self.rpc)
        self.assertEqual(self.calls,[])
    def test_identity_and_title_must_match(self):
        for thread in [{'id':self.id,'name':'別の会話'},{'id':'other','name':'ロキの改修'},{'id':self.id,'name':None}]:
            with self.assertRaises(Unavailable):bind_visible(self.snapshot,'指示',self.now,lambda *args:{'thread':thread})
    def test_unavailable_connection_not_success(self):
        def failed(*args):raise ConnectionError('unavailable')
        with self.assertRaises(ConnectionError):bind_visible(self.snapshot,'指示',self.now,failed)
    def test_expired_request_not_sent(self):
        request=bind_visible(self.snapshot,'指示',self.now,self.rpc)
        with self.assertRaises(Unavailable):queue_bound(request,self.now+60_001,self.rpc)
        self.assertEqual(len(self.calls),1)
    def test_bad_input_does_not_reach_rpc(self):
        for text in ['', 'x'*3001, 'bad\x00input']:
            with self.assertRaises(Unavailable):bind_visible(self.snapshot,text,self.now,self.rpc)
        self.assertEqual(self.calls,[])

if __name__=='__main__':unittest.main()
