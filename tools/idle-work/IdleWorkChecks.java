package com.example.rokidkeyboardbridge;
/** Desktop JVM checks of the exact production status gate and notice pacing. No device/API access. */
public final class IdleWorkChecks {
 private static int passed;
 private static void check(boolean value,String label){if(!value)throw new AssertionError(label);passed++;}
 public static void main(String[] args){
  IdleStateSync gate=new IdleStateSync();
  check(gate.shouldSend("idle",0,6000),"first delivery");
  check(gate.shouldSend("idle",1000,6000),"unsent/failure remains retryable");
  gate.delivered("idle",0);
  for(long now=1;now<6000;now++)check(!gate.shouldSend("idle",now,6000),"unchanged before heartbeat");
  check(gate.shouldSend("idle",6000,6000),"bounded liveness heartbeat");
  check(gate.shouldSend("idle",7000,6000),"failed heartbeat retry");
  check(gate.shouldSend("changed",1,6000),"change immediate");
  gate.delivered("Aa",100);check(gate.shouldSend("BB",101,6000),"String.hashCode collision");
  check(gate.shouldSend("Aa",99,6000),"monotonic clock reset");
  for(int i=0;i<1000;i++){String state="state-"+i+"-\u65e5\u672c\u8a9e";check(gate.shouldSend(state,200+i,6000),"new state");gate.delivered(state,200+i);check(!gate.shouldSend(state,201+i,6000),"successful state coalesced");}
  IdleStateSync quiet=new IdleStateSync();int attempts=0,sent=0;long last=-1,maxGap=0;
  for(long now=0;now<60000;now+=4000){attempts++;if(quiet.shouldSend("unchanged",now,6000)){if(last>=0)maxGap=Math.max(maxGap,now-last);last=now;sent++;quiet.delivered("unchanged",now);}}
  check(sent<attempts,"idle requests reduced");check(maxGap<15000,"existing remote freshness threshold retained");
  check(CommandNoticePacing.delayMs(true,true,0)>=3000,"held queue bounded");
  check(CommandNoticePacing.delayMs(true,false,0)>=1000,"empty queue bounded");
  long lastDelay=0;for(int failure=1;failure<100;failure++){long delay=CommandNoticePacing.delayMs(false,false,failure);check(delay>=lastDelay&&delay>=1000&&delay<=30000,"reconnect backoff");lastDelay=delay;}
  System.out.println("{\"passed\":"+passed+",\"simulationSeconds\":60,\"oldIdleStatusSends\":"+attempts+",\"newIdleStatusSends\":"+sent+",\"maxHeartbeatGapMs\":"+maxGap+",\"deviceBatteryMeasurement\":false}");
 }
}
