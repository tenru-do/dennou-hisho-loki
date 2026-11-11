package com.example.rokidkeyboardbridge;
public final class SeatedLegActionTest {
 static int checks;static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static void run(SeatedLegMotionPolicy p,long t){p.update(t,true,false,false,false,false);}
 public static void main(String[] args){
  String[] commands={"脚をほどいて","脚の組みを解いて","両足をそろえて","右足を上にして組んで","左脚を上にして組んで","反対側に組んで","脚を組んで"};
  int[] actions={2,2,2,0,1,3,4};for(int i=0;i<commands.length;i++)check(SeatedLegAction.parse(commands[i])==actions[i],commands[i]);
  for(String t:new String[]{"脚を組まないで","脚を組む方法は？","脚組み画像を追加して","彼女が脚を組んでいる","明日の予定は？"})check(SeatedLegAction.parse(t)==-1,"no false intent "+t);
  SeatedLegMotionPolicy p=new SeatedLegMotionPolicy();check(p.requestTarget(0,true,2),"uncross accepted");run(p,0);run(p,2600);check(!p.active()&&p.frame()==3&&p.settledSide()==2,"neutral held");run(p,500000);check(p.frame()==3,"neutral never silently re-crosses");
  check(!p.requestTarget(500000,true,2),"same endpoint no-op");check(p.requestTarget(500001,true,1),"neutral to left");run(p,500001);run(p,503651);check(!p.active()&&p.frame()==7,"left held");
  check(p.requestTarget(504000,true,2),"left uncross");run(p,504000);run(p,507650);check(p.frame()==3&&!p.active(),"left to neutral held");
  check(p.requestTarget(508000,true,0),"right cross");run(p,508000);run(p,510600);check(p.frame()==0&&!p.active(),"right held");
  System.out.println("SeatedLegActionTest "+checks+" checks passed");
 }
}
