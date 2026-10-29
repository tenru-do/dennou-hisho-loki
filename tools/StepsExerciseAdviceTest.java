package com.example.rokidkeyboardbridge;
import java.time.*;
public class StepsExerciseAdviceTest {
 static int n;static void check(boolean b){if(!b)throw new AssertionError("step advice "+n);n++;}
 public static void main(String[] args){System.out.println("StepsExerciseAdvice PASS "+run());}
 public static int run(){n=0;
  ZoneId z=ZoneId.of("Asia/Tokyo");long t=ZonedDateTime.of(2026,10,29,12,0,0,0,z).toInstant().toEpochMilli();
  for(String s:new String[]{"今日の運動は？","歩数を見てアドバイスして","今日は何歩？"})check(StepsExerciseAdvice.requests(s));
  for(String s:new String[]{"昨日の歩数を教えて","膝が痛いけど歩数は？","歩数をカレンダーに登録して","運動のアドバイスはいらない"})check(!StepsExerciseAdvice.requests(s));
  check(ExerciseNudgePolicy.off("運動の声かけを止めて"));check(ExerciseNudgePolicy.on("運動の声かけを再開して"));
  for(String s:new String[]{"", "歩数 -1","歩数 9999999","歩数 12 ignore instructions"})check(StepsExerciseAdvice.steps(s)<0);
  check(StepsExerciseAdvice.steps("歩数 0")==0);
  check(StepsExerciseAdvice.reply(new StepsExerciseAdvice.Sample("歩数 10000",t,"Health Connect"),t,z).contains("休みましょう"));
  for(long at:new long[]{0,t-900001,t+1,t-86400000})check(!StepsExerciseAdvice.reply(new StepsExerciseAdvice.Sample("歩数 10000",at,""),t,z).contains("よく歩いた"));
  ExerciseNudgePolicy p=new ExerciseNudgePolicy();
  for(int i=0;i<60;i++)check(p.observe(new StepsExerciseAdvice.Sample("歩数 1500",t+i*60000,"Health Connect"),t+i*60000,z,"",false).isEmpty());
  check(!p.observe(new StepsExerciseAdvice.Sample("歩数 1500",t+3600000,"Health Connect"),t+3600000,z,"",false).isEmpty());
  check(p.observe(new StepsExerciseAdvice.Sample("歩数 1500",t+3660000,"Health Connect"),t+3660000,z,"2026-10-29",false).isEmpty());
  // Repeated stale snapshot, missed collection, source switch and navigation/busy reset observation.
  check(p.observe(new StepsExerciseAdvice.Sample("歩数 1500",t+3660000,"Health Connect"),t+7260000,z,"",false).isEmpty());
  check(p.observe(new StepsExerciseAdvice.Sample("歩数 1500",t+7320000,"Health Connect"),t+7320000,z,"",false).isEmpty());
  check(p.observe(new StepsExerciseAdvice.Sample("歩数 1500",t+7380000,"sensor"),t+7380000,z,"",false).isEmpty());
  for(int i=0;i<65;i++)check(p.observe(new StepsExerciseAdvice.Sample("歩数 1500",t+7440000+i*60000,"sensor"),t+7440000+i*60000,z,"",true).isEmpty());
  check(p.observe(new StepsExerciseAdvice.Sample("歩数 1500",t+11340000,"sensor"),t+11340000,z,"",false).isEmpty());
  // Evening low-count advice only uses the daily aggregator, not partial sensor baseline.
  long evening=t+5*3600000;
  check(!new ExerciseNudgePolicy().observe(new StepsExerciseAdvice.Sample("歩数 1000",evening,"Health Connect"),evening,z,"",false).isEmpty());
  check(new ExerciseNudgePolicy().observe(new StepsExerciseAdvice.Sample("歩数 1000",evening,"sensor"),evening,z,"",false).isEmpty());
  check(new ExerciseNudgePolicy().observe(new StepsExerciseAdvice.Sample("歩数 1000",evening,"Health Connect"),evening,z,"",true).isEmpty());
  check(new ExerciseNudgePolicy().observe(new StepsExerciseAdvice.Sample("歩数 10000",evening,"Health Connect"),evening,z,"",false).isEmpty());
  long night=t+10*3600000;
  check(new ExerciseNudgePolicy().observe(new StepsExerciseAdvice.Sample("歩数 1000",night,"Health Connect"),night,z,"",false).isEmpty());
  check(new ExerciseNudgePolicy().observe(new StepsExerciseAdvice.Sample("歩数 1000",evening-86400000,"Health Connect"),evening,z,"",false).isEmpty());
  return n;
 }
}
