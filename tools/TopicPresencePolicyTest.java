package com.example.rokidkeyboardbridge;
public final class TopicPresencePolicyTest {
 static int n;static void ok(boolean b,String s){n++;if(!b)throw new AssertionError(s);}
 public static void main(String[] args){
 for(int bits=0;bits<16;bits++){boolean enabled=(bits&1)!=0,loop=(bits&2)!=0,record=(bits&4)!=0,recognizer=(bits&8)!=0;ok(TopicRunGuard.handoff(enabled,loop,record,recognizer)==(enabled&&(loop||record||recognizer)),"voice handoff truth table");}
 ok(TopicRunGuard.blocked(true,false,false,true,false,false).equals("CONVERSATION"),"dormant conversation prevents capture");ok(TopicRunGuard.blocked(true,false,false,false,false,false).isEmpty(),"idle unblocked");
 for(String phase:new String[]{"CAPTURING","GENERATING","QUEUED","ERROR"})ok(!TopicProgressLabel.label(phase,"NONE").isEmpty(),"phase visible "+phase);
 for(String reason:new String[]{"BRIDGE_STALE","COOLDOWN","MENU","NAVIGATION","CONVERSATION","CAPTURING","BUSY","QUEUE_BUSY"})ok(!TopicProgressLabel.label("WAITING",reason).isEmpty(),"reason visible "+reason);
 ok(HudOperationStatus.compact("認識中\n追加発話も受付",2500,true).equals("認識中 追加発話も受付（2秒）"),"single status with measured phase age");ok(HudOperationStatus.compact(null).isEmpty(),"empty status");ok(HudOperationStatus.compact(new String(new char[300]).replace('\0','長')).length()==64,"bounded status");
 ok(CharacterFrameCadence.delay(true,false,false)==33,"speech smooth");ok(CharacterFrameCadence.delay(false,true,false)==33,"gestures smooth");ok(CharacterFrameCadence.delay(false,false,true)==66,"recognition gets CPU");ok(CharacterFrameCadence.delay(false,false,false)==100,"idle avoids excess frames");
 System.out.println("PASS "+n+" TOPIC handoff, phase HUD and drawing cadence checks");
 }
}
