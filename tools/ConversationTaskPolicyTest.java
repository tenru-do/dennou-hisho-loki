package com.example.rokidkeyboardbridge;
public final class ConversationTaskPolicyTest {
 static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);}
 public static void main(String[]args){
  String request="調べろ。グラスとスタイルの使い分けを提案しろ。使い分けの意味も含めて";
  check(QueryPolicy.search(request),"imperative research");
  for(String s:new String[]{"調べろ","検索しろ","調査して"})check(QueryPolicy.search(s),s);
  for(String s:new String[]{"ひどいな。文脈無視で。","は？繰り返せと？さっき伝えたはず","とっとと提案してよ。なんでいったんとめるの？"}){
   check(ConversationFollowupPolicy.referencesPrevious(s),"retain task "+s);
   check(QueryPolicy.search(s,request,120000),"research repair "+s);
  }
  check(ConversationTaskPolicy.emptyRepair("とっとと提案してよ。","申し訳ございません。もしよろしければ改めてご提案させていただけますでしょうか。",true),"permission loop");
  check(ConversationTaskPolicy.emptyRepair("ひどいな。文脈無視で。","ご指摘の通りです。今後は精進いたします。",true),"apology only");
  check(!ConversationTaskPolicy.emptyRepair("とっとと提案してよ。","失礼しました。結論から言うと、持ち歩くなら軽い方がおすすめは明確です。一方、表示機能には違いがあります。",true),"substantive answer");
  check(!ConversationTaskPolicy.emptyRepair("とっとと提案してよ。","申し訳ございません。お教えください。",false),"no invented context");
  check(!ConversationTaskPolicy.continuesTask("別の話で、グラスの形を調べろ"),"new topic");
  check(!QueryPolicy.search("眠気がくる",request,1000),"personal-state regression");
  System.out.println("PASS imperative search, context repair chain, apology loop, substantive/new-topic/personal-state exclusions");
 }
}
