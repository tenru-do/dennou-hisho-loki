package com.example.rokidkeyboardbridge;
public class ProactiveDeliveryPolicyTest {
 static int n;static void ok(boolean b,String label){if(!b)throw new AssertionError(label);n++;}
 static String gate(int flag){return ProactiveDeliveryPolicy.blocked(flag!=1,flag!=2,flag==3,flag==4,flag==5,flag==6,flag==7,flag==8,flag==9);}
 public static void main(String[]args){
  ok(gate(0).isEmpty(),"idle permitted");String[] reasons={"OFF","BACKGROUND","NAVIGATION","USER_PAUSE","VOICE","CONVERSATION","SPEAKING","PENDING","DRAFT"};for(int i=1;i<=9;i++)ok(gate(i).equals(reasons[i-1]),"independent gate "+i);
  ProactiveDeliveryPolicy p=new ProactiveDeliveryPolicy();String text="電車の窓から青空が見えます。遠くの建物までよく見えて、今日は見通しがいいですね。";ok(!p.duplicate(text,0),"unshown text not consumed");p.remember(text,0);ok(p.duplicate(text,1),"same text suppressed");ok(p.duplicate("【AMB取得】\n"+text+"[[THINK:1:DEFAULT]]",2),"display heading and reaction tags ignored");ok(p.duplicate("なるほど、"+text,3),"filler prefix ignored");ok(!p.duplicate(text,90000),"exact expiry eligible again");p.remember("電車はあと3駅です。",100000);ok(!p.duplicate("電車はあと2駅です。",100001),"number change preserved");p.remember("道路で事故が発生しています。",100000);ok(!p.duplicate("道路で事故が発生しています。",100001),"safety-related repeat not suppressed");ok(!p.duplicate("道路は通行できます。",100001),"different fact preserved");p.remember("雨は降っています。",100000);ok(!p.duplicate("雨は降っていません。",100001),"negated fact preserved");p.remember(text,100000);ok(!p.duplicate(text,99999),"backward clock cannot create stale duplicate");for(int i=0;i<9;i++)p.remember("別の説明"+i,200000+i);ok(!p.duplicate("別の説明0",200010),"bounded eight entries evict oldest");ok(p.duplicate("別の説明8",200010),"latest entry retained");
  System.out.println("PASS proactive final delivery "+n+" checks");
 }
}
