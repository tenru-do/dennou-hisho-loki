package com.example.rokidkeyboardbridge;
import java.util.*;
public final class ContextVisualPlanTest {
 static int n;static void ok(boolean b,String s){n++;if(!b)throw new AssertionError(s);}
 static VisualConversationPlan plan(String s,int posture,String last){return VisualConversationPlan.resolve(s,posture,last);}
 public static void main(String[] args){
  String[] seats={"腰掛けて","腰かけて","そこに腰を下ろして","座ってくれる？","ロキ、すわって","座ってほしい","座ってみて"};
  for(String t:seats){VisualConversationPlan p=plan(t,0,"");ok(p.kind==2&&Arrays.asList(p.steps).contains("座って"),"seat alias "+t);}
  for(String t:new String[]{"ねそべって","寝そべってくれる？","横になってくれる？"})ok(Arrays.asList(plan(t,1,"").steps).contains("寝そべって"),"lie alias");
  for(String t:new String[]{"ありがとう","いいね","うん","その姿勢は楽？","昨日は寝そべっていたね","彼女は座っていたね","今寝そべっているの？","座らないで"})ok(plan(t,2,"").kind==1,"hold/report "+t);
  ok(plan("足の痛みについて相談して",0,"").kind==1||plan("足の痛みについて相談して",0,"").kind==0,"ordinary help must not become unsupported pose");
  ok(plan("寝そべらないで",2,"").steps[0].equals("座って"),"explicit lying prohibition exits to seat");
  ok(ComfortPoseIntent.adjacent(plan("隣に座って",0,"").steps[0]),"adjacent retains original relaxation behavior");
  ok(plan("座れる？",0,"").kind==4,"capability is not execution");ok(plan("座ってくれる？",0,"").kind==2,"polite request executes");
  ok(plan("まだ座ってないよ",0,"座って").steps[0].equals("座って"),"complaint repairs");
  ok(plan("立ったままだよ",0,"座って").steps[0].equals("座って"),"context repairs target");
  ok(plan("座って、やっぱり立って",1,"").steps[0].equals("立って"),"self correction newest clause");
  ok(plan("手を振ってから座って",0,"").sequence,"sequence vs simultaneous");
  ok(plan("座りながら手を振って",0,"").steps.length==2&&!plan("座りながら手を振って",0,"").sequence,"combined pose and gesture");
  ok(plan("話し終わったら座って",0,"").kind==3,"conditional is deferred");
  ok(plan("少し楽にして",2,"").steps[0].contains("寝たまま"),"ambiguous rest retains lie");
  ok(plan("もっとゆっくり",1,"").speed<1,"speed separate from body scale");
  ok(plan("反対の手で",1,"手を振って").hand==2,"opposite prior hand");
  ok(plan("逆",0,"").kind==4,"ambiguous relative command asks");
  ok(plan("そのままこっち向いて",2,"").gaze==1,"gaze without posture jump");
  ok(plan("左手を振って",1,"").hand==-1,"character left");ok(plan("画面の右へ手を振って",1,"").hand==-1,"viewer and character side");
  ok(plan("この座り方が好き",1,"").kind==6,"explicit preference");
  for(int w:new int[]{100,240,360})for(int h:new int[]{300,600,900}){float s=CharacterViewport.scale(700,w,h);ok(s>0&&Float.isFinite(s),"finite portrait scale");ok(s==CharacterViewport.stature(w,h)/700,"no per-pose width fit");}
  VisualContinuityLedger ledger=new VisualContinuityLedger();ledger.begin(1,1,1000);ledger.rendered(0,1200);ok(ledger.summary().contains("MISMATCH"),"trace sees actual mismatch");for(int i=0;i<1000;i++)ledger.event("DRAW");ok(ledger.summary().length()<8000,"bounded metadata trace");
  System.out.println("PASS "+n+" contextual action, sequencing, privacy and crop checks");
 }
}
