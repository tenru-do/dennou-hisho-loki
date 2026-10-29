package com.example.rokidkeyboardbridge;
public final class VirtualContactPolicyTest {
 static int n;static void ok(boolean b,String s){n++;if(!b)throw new AssertionError(s);}
 static VirtualContactPolicy.Frame f(VirtualContactPolicy p,long now){return p.frame(now);}
 public static void main(String[]args){
  long now=10000000;VirtualContactPolicy p=new VirtualContactPolicy();ok(f(p,now).kind==0,"initially none");
  p.user("手を差し出して",now);ok(f(p,now).kind==1&&f(p,now).offerOnly,"explicit offer waits");p.answer("手を差し出して","手を握ります。",now+1);ok(f(p,now+1).kind==1,"model cannot invent user acceptance");ok(p.gesture(0,now+2),"tap accepts");ok(f(p,now+2).kind==2,"held");
  p.user("あたたかいね",now+10000);p.answer("あたたかいね","私も嬉しいです。",now+10001);ok(f(p,now+10001).kind==2,"normal followup retains hold");
  p.user("手をつないで",now+11000);ok(f(p,now+11000).kind==2,"request does not restart held scene");ok(p.gesture(2,now+12000),"swipe");ok(f(p,now+12000).kind==2&&f(p,now+12000).direction==2&&f(p,now+12000).event==3,"direction retained without losing hand");
  p.user("手を離さないで",now+13000);ok(f(p,now+13000).kind==2,"negative release does not release");p.user("やめないで",now+14000);ok(f(p,now+14000).kind==2,"negative stop preserved");
  p.user("今日の天気を教えて",now+15000);ok(f(p,now+15000).kind==2&&f(p,now+15000).suspended,"task hides visual but retains hand");ok(!p.gesture(0,now+15001),"task never accepts touch");p.answer("今日の天気を教えて","APIの呼び出しはできません。",now+15002);ok(f(p,now+15002).kind==2,"unrelated failure not release");p.user("そのままで",now+16000);ok(f(p,now+16000).visible(),"resume scene");
  p.user("手を離して",now+17000);ok(f(p,now+17000).kind==0&&!p.gesture(0,now+17001),"release cannot revive by button");ok(p.context(now+17002),"recent release beats old transcript");
  p.user("今は手をつないでいる？",now+17500);ok(f(p,now+17500).kind==0,"status question cannot create new hold");
  for(String q:new String[]{"今も手をつないでる？","手つないでるの？","今、手を握ってる？","手をつないでいる","頭を撫でている？"}){p.user(q,now+17600);ok(f(p,now+17600).kind==0,"status variants cannot restart scene: "+q);}
  for(String action:new String[]{"手つないで","手をつなごう","手を取って"}){VirtualContactPolicy natural=new VirtualContactPolicy();natural.user(action,now);ok(f(natural,now).kind==1,"natural request: "+action);natural.answer(action,"手を握り返します。",now+1);ok(f(natural,now+1).kind==2,"natural request confirmed");}
  p.user("手を握って",now+18000);p.answer("古い質問","手を握ります。",now+18001);ok(f(p,now+18001).kind==1,"stale answer excluded");p.answer("手を握って","そっと手を握り返します。",now+18002);ok(f(p,now+18002).kind==2,"explicit reply confirms");
  p.user("抱きしめて",now+19000);p.answer("抱きしめて","抱きしめられません。",now+19001);ok(f(p,now+19001).kind==2,"declining new hug preserves previous hand");
  p.user("抱きしめて",now+20000);p.answer("抱きしめて","そっと抱きしめます。",now+20001);ok(f(p,now+20001).kind==3&&f(p,now+20001).actor==1,"Loki hugs user");
  p.user("やめて",now+21000);ok(f(p,now+21000).kind==0,"stop respected");p.user("頭を撫でる",now+22000);ok(f(p,now+22000).kind==4&&f(p,now+22000).actor==2&&f(p,now+22000).part==2,"user pats Loki head");
  p.user("頬を撫でて",now+23000);p.answer("頬を撫でて","頬をそっと撫でます。",now+23001);ok(f(p,now+23001).kind==4&&f(p,now+23001).actor==1&&f(p,now+23001).part==4,"Loki pats user cheek");
  for(String text:new String[]{"手をつなぐ意味は？","彼女が手を握る物語","手を握る方法を教えて","画像で手を差し出して","頭を撫でる仕様を実装"}){VirtualContactPolicy other=new VirtualContactPolicy();other.user(text,now);ok(f(other,now).kind==0,"not a contact command: "+text);}
  for(String stop:new String[]{"離して","触らないで","触るな","触れ合いを終了","新しい話題です"}){p.user("手を握る",now);p.user(stop,now+1);ok(f(p,now+1).kind==0,"release: "+stop);}
  p.user("手を差し出して",now);ok(f(p,now+VirtualContactPolicy.OFFER_TTL+1).kind==0,"offer timeout");p.user("手を握る",now);ok(f(p,now+VirtualContactPolicy.TTL+1).kind==0,"idle timeout");
  p.restore(2,1,3,2,0,2,now,false,false,now+1000);ok(f(p,now+1000).kind==2,"valid saved state");p.restore(2,1,3,2,0,2,now+1,false,false,now);ok(f(p,now).kind==0,"future state excluded");p.restore(99,1,3,2,0,2,now,false,false,now);ok(f(p,now).kind==0,"invalid enum excluded");
  for(int key:new int[]{4,19,20,21,22,23,109,111}){ok(VirtualContactPresentation.captureKey(true,true,false,false,key),"avatar-only capture");ok(!VirtualContactPresentation.captureKey(false,true,false,false,key),"ordinary command not stolen");ok(!VirtualContactPresentation.captureKey(true,true,true,false,key),"nav/record/dialog priority");ok(!VirtualContactPresentation.captureKey(true,true,false,true,key),"no repeats");}
  p.user("手を握る",now);for(int level=0;level<3;level++){CharacterMotionTuning.set(level);for(int t=0;t<2000;t+=5){float pulse=VirtualContactPresentation.pulse(f(p,now+t),now+t);ok(Float.isFinite(pulse)&&pulse>=0&&pulse<=1.25,"bounded reaction");}}CharacterMotionTuning.set(1);
  p.user("手を握る",now);long originalEvent=f(p,now).at;for(int hour=1;hour<=3;hour++)p.user("そのままで",now+hour*3600000L);VirtualContactPolicy.Frame carried=f(p,now+3*3600000L);ok(carried.kind==2&&carried.at==originalEvent,"conversation refresh is not a new touch");VirtualContactPolicy restored=new VirtualContactPolicy();restored.restore(carried.kind,carried.part,carried.actor,carried.event,carried.direction,carried.pending,carried.at,carried.activity,carried.suspended,carried.offerOnly,now+3*3600000L);ok(f(restored,now+3*3600000L).kind==2,"last activity survives restore");
  System.out.println("PASS "+n+" virtual scene/consent/actor/continuity/release/expiry/input/response assertions");
 }
}
