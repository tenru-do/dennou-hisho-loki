package com.example.rokidkeyboardbridge;
public final class ContextReactionTest {
 public static int n;static void ok(boolean v,String msg){n++;if(!v)throw new AssertionError(msg);}
 public static void main(String[] args){
 for(String pose:new String[]{"寝そべって","膝を抱えて座って","寝たまま立膝にして"}){
  EmbodiedScenePolicy p=new EmbodiedScenePolicy();p.user(pose,1000);p.answer("お断りします。",FullBodyPoseCatalog.Kind.REFUSE,2000);ok(!p.correctedRest(),"old rest cannot override refusal");ok(p.clip(2100,false)==-1,"refusal stops previous body clip");ok(!p.canReplay()&&!p.canUndo(),"repair cannot restore dismissed scene");
 }
 VirtualContactPolicy c=new VirtualContactPolicy();c.user("手を握って",10000);c.answer("手を握って","手を握ります。",11000);ok(c.frame(11000).kind==VirtualContactPolicy.HAND,"setup hand");c.user("続けて",12000);String reply=FullBodySemanticReaction.decode("今は控えます。[[LOKI_REACTION:REFUSE:2:PROTECT]]");c.answer("続けて",reply,13000);if(ContactResponseBoundary.refused(reply))c.clear(13000);ok(c.frame(13000).kind==VirtualContactPolicy.NONE,"semantic refusal clears held contact");ok(!c.gesture(0,14000),"tap cannot revive dismissed contact");
 c=new VirtualContactPolicy();c.user("手を握って",10000);c.answer("手を握って","手を握ります。",11000);c.user("続けて",12000);c.answer("続けて","お断りします。",13000);if(ContactResponseBoundary.refused("お断りします。"))c.clear(13000);ok(c.frame(13000).kind==VirtualContactPolicy.NONE,"plain refusal also clears held contact");
 RestConversationReaction q=new RestConversationReaction();q.answer(FullBodyPoseCatalog.Kind.JOY,1000);q.answer(FullBodyPoseCatalog.Kind.WORRY,1100);ok(q.face(0,false,1200)==9,"worry replaces joy");q.answer(FullBodyPoseCatalog.Kind.SURPRISE,1300);ok(q.face(0,false,1400)==8,"surprise replaces worry");q.answer(FullBodyPoseCatalog.Kind.WORRY,1500);ok(q.moving(1600),"new worry not suppressed by old cooldown");ok(q.face(1,true,1600)==9,"current concern overrides old manual smile or kiss temporarily");q.touch(1600,1,true);ok(q.face(0,false,1650)==9,"touch feedback cannot erase current concern");q.answer(FullBodyPoseCatalog.Kind.RECOVERY,1700);ok(q.face(0,false,1800)==0,"recovery releases concern");
 EmbodiedScenePolicy p=new EmbodiedScenePolicy();p.user("寝そべって",1000);p.answer("落ち着きました。",FullBodyPoseCatalog.Kind.RECOVERY,2000);ok(p.correctedRest(),"ordinary recovery preserves base posture");
 for(FullBodyPoseCatalog.Kind k:FullBodyPoseCatalog.Kind.values()){int mapped=ConversationVisualCatalog.restReaction(k);ok(mapped>=1&&mapped<=6,"every emotion has an existing artwork family: "+k);}ok(!ContactResponseBoundary.refused("彼女がやめてと言いました。"),"quoted third-party refusal is not Loki refusal");
 System.out.println("PASS "+n+" context reaction priority checks");
 }
}
