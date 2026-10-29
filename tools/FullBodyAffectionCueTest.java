package com.example.rokidkeyboardbridge;
public final class FullBodyAffectionCueTest {
 static int n;static void ok(boolean b,String s){n++;if(!b)throw new AssertionError(s);}
 public static void main(String[]args){
  FullBodyAffectionCue cue=new FullBodyAffectionCue();
  cue.user("ロキ、甘えてきて",1000);ok(cue.clip(1100,true)==-1,"wait for response");cue.answer("少しそばにいてもいいですか",FullBodyPoseCatalog.Kind.SHY,1200);ok(cue.clip(1300,true)==5&&cue.clip(1300,false)==4,"affection keeps posture");
  ok(cue.clip(6000,true)==-1,"one clip settles");ok(cue.clip(19500,true)==5,"bounded conversational continuation");ok(cue.clip(91000,true)==-1,"ninety second expiry");
  for(String text:new String[]{"やめて","普通に戻って","普通にして","もういい","別の話","もっと天気を教えて","明日の予定","誘惑しないで"}){cue.user("もっと甘えて",1000);cue.answer("そばにいたいです",FullBodyPoseCatalog.Kind.SHY,1200);cue.user(text,1300);ok(cue.clip(1400,true)==-1,"stop/topic precedence "+text);}
  for(String text:new String[]{"誘惑とは何","彼女が甘えてくる話","友達を飲みに誘って","誘ってほしい文面を作る","小説の誘惑してくる登場人物"})ok(FullBodyAffectionCue.request(text)==-1,"definition/third-party exclusions");
  cue.user("ロキ、こちらを誘惑して",1000);cue.answer("こっちへいらっしゃい",FullBodyPoseCatalog.Kind.CONFIDENT,1200);ok(cue.clip(1300,true)==7&&cue.clip(1300,false)==6,"invitation distinct from affection");
  cue.answer("おやめください",FullBodyPoseCatalog.Kind.REFUSE,1400);ok(cue.clip(1500,true)==-1,"assistant refusal stops gesture");
  cue.user("甘えて",1000);cue.answer("こちらではできません",FullBodyPoseCatalog.Kind.JOY,1200);ok(cue.clip(1300,true)==-1,"wording refusal beats wrong positive tag");
  cue.user("誘って",1000);cue.answer("駅はこちらです",FullBodyPoseCatalog.Kind.NEUTRAL,1200);ok(cue.clip(1300,true)==-1,"plain unrelated answer not invitation");
  cue.user("ロキ大好き",1000);cue.answer("私も一緒にいたいです",FullBodyPoseCatalog.Kind.JOY,1200);ok(cue.clip(1300,true)==5,"warm conversation activation");cue.clear();ok(cue.clip(1400,true)==-1,"conversation end clear");
  cue.user("今日は会いたかった。一緒にいて",1000);cue.answer("私もそばにいたいです",FullBodyPoseCatalog.Kind.SHY,1200);ok(cue.clip(1300,true)==5,"natural affectionate exchange without manual action command");
  cue.user("今日の会議の準備を手伝って",1400);ok(cue.clip(1500,true)==-1,"work conversation returns to normal");
  System.out.println("PASS "+n+" affection/invitation/context/stop/refusal/expiry assertions");
 }
}
