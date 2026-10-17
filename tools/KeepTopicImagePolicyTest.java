package com.example.rokidkeyboardbridge;
public final class KeepTopicImagePolicyTest {
 static int checks;
 static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static boolean allow(long now,long at,long last,long size,long heap,boolean low,boolean waiting){return KeepTopicImagePolicy.canReuse(now,at,last,size,heap,low,waiting);}
 public static void main(String[] args){
  long mb=1024L*1024,now=100000;
  check(allow(now,now-5000,0,mb,64*mb,false,false),"recent saved photo can feed TOPIC");
  check(!allow(now,now-5000,0,mb,64*mb,false,true),"KEEP waiting takes priority");
  check(!allow(now,now-5000,0,mb,64*mb,true,false),"system memory pressure skips optional vision");
  check(!allow(now,now-5000,0,mb,35*mb,false,false),"retain heap reserve and decrypt/decode headroom");
  check(allow(now,now-15000,now-30000,3*mb,44*mb,false,false),"bounded edge values");
  check(!allow(now,now-15001,0,mb,64*mb,false,false),"stale photo never treated as current");
  check(!allow(now,now+1,0,mb,64*mb,false,false),"future capture rejected");
  check(!allow(now,0,0,mb,64*mb,false,false),"no capture yet");
  check(!allow(now,now-5000,now-29999,mb,64*mb,false,false),"vision rate limited to 30 seconds");
  check(!allow(now,now-5000,now+1,mb,64*mb,false,false),"monotonic rollback does not bypass throttle");
  check(!allow(now,now-5000,0,3*mb+1,64*mb,false,false),"large source skipped before decrypt");
  check(!allow(now,now-5000,0,0,64*mb,false,false),"missing file skipped");
  System.out.println("KeepTopicImagePolicy PASS checks="+checks);
 }
}
