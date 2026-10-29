package com.example.rokidkeyboardbridge;
public final class NotificationExpiryPolicyTest {
 static int n;static void ok(boolean b,String m){n++;if(!b)throw new AssertionError(m);}
 public static void main(String[]args){
  for(int mask=0;mask<16;mask++)for(int age=-1000;age<150000;age+=125){boolean latest=(mask&1)!=0,busy=(mask&2)!=0,same=(mask&4)!=0,empty=(mask&8)!=0;int d=NotificationExpiryPolicy.decide(latest,busy,same,empty,age);
   if(!latest||age<18000||(!same&&!empty))ok(d==0,"old notice/new answer preserved");else if(busy)ok(d==(age<120000?1:0),"active voice/request never interrupted; bounded retry");else ok(d==2,"expired notice or cleared text releases conversation");
  }
  ok(NotificationExpiryPolicy.decide(true,false,false,true,20000)==2,"cleared display cannot leave busy flag forever");ok(NotificationExpiryPolicy.decide(true,false,true,false,20000)==2,"unrelated request counter changes do not strand notification");ok(NotificationExpiryPolicy.decide(true,true,true,false,20000)==1,"speech ending late gets another cleanup attempt");ok(NotificationExpiryPolicy.decide(true,false,true,false,25000)==2,"cleanup succeeds once speech stops");
  System.out.println("PASS "+n+" transient notification expiry, owner, speech, empty-surface and new-answer checks");
 }
}
