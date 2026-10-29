package com.example.rokidkeyboardbridge;
import java.time.*;
public final class EnergyBalanceNudgeTest {
 static int n;static void ok(boolean b){if(!b)throw new AssertionError("energy nudge "+n);n++;}
 public static int run(){n=0;ZoneId z=ZoneId.of("Asia/Tokyo");long t=ZonedDateTime.of(2026,10,29,16,0,0,0,z).toInstant().toEpochMilli();
  StepsExerciseAdvice.Sample s=new StepsExerciseAdvice.Sample("歩数 2500",t,"Health Connect");
  ok(!EnergyBalanceNudge.advice(500,t,"2026-10-29","HEALBE_SCREEN",s,t,z,"",false).isEmpty());
  for(int balance:new int[]{Integer.MIN_VALUE,-500,0,299,15001})ok(EnergyBalanceNudge.advice(balance,t,"2026-10-29","HEALBE_SCREEN",s,t,z,"",false).isEmpty());
  for(long at:new long[]{0,t+1,t-900001,t-86400000})ok(EnergyBalanceNudge.advice(500,at,"2026-10-29","HEALBE_SCREEN",s,t,z,"",false).isEmpty());
  for(String source:new String[]{"", "MANUAL","STALE_HEALBE","ActiveCalories","Step Counter / bridge"})ok(EnergyBalanceNudge.advice(500,t,"2026-10-29",source,s,t,z,"",false).isEmpty());
  ok(EnergyBalanceNudge.advice(500,t,"2026-10-28","HEALBE_SCREEN",s,t,z,"",false).isEmpty());
  ok(EnergyBalanceNudge.advice(500,t,"2026-10-29","HEALBE_SCREEN",s,t,z,"",true).isEmpty());
  ok(EnergyBalanceNudge.advice(500,t,"2026-10-29","HEALBE_SCREEN",s,t,z,"2026-10-29",false).isEmpty());
  for(StepsExerciseAdvice.Sample invalid:new StepsExerciseAdvice.Sample[]{new StepsExerciseAdvice.Sample("歩数 5000",t,"Health Connect"),new StepsExerciseAdvice.Sample("歩数 1000",t,"sensor"),new StepsExerciseAdvice.Sample("歩数 1000",t-300001,"Health Connect"),new StepsExerciseAdvice.Sample("歩数 1000",t+1,"Health Connect"),new StepsExerciseAdvice.Sample("",t,"Health Connect")})ok(EnergyBalanceNudge.advice(500,t,"2026-10-29","HEALBE_SCREEN",invalid,t,z,"",false).isEmpty());
  for(int h=0;h<24;h++){long now=ZonedDateTime.of(2026,10,29,h,0,0,0,z).toInstant().toEpochMilli();ok(!EnergyBalanceNudge.advice(500,now,"2026-10-29","HEALBE_NOTIFICATION",new StepsExerciseAdvice.Sample("歩数 2000",now,"Health Connect"),now,z,"",false).isEmpty()==(h>=15&&h<20));}
  return n;
 }
 public static void main(String[] args){System.out.println("EnergyBalanceNudge PASS "+run());}
}
