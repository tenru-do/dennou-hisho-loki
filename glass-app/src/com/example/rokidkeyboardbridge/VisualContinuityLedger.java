package com.example.rokidkeyboardbridge;
import java.util.*;
/** Metadata only. Never stores commands, answers, locations or image pixels. */
final class VisualContinuityLedger {
 private final ArrayDeque<String> rows=new ArrayDeque<>();private final int[] unsupported=new int[4];private long started;private int expected=-1,selected=-1,drawn=-1;private long count,latency;
 void begin(int e,int s,long now){started=now;expected=e;selected=s;record("ACCEPTED",0);}
 void rendered(int actual,long now){if(drawn==actual&&expected<0)return;drawn=actual;if(started>0){long delta=Math.max(0,now-started);latency+=delta;count++;record(actual==expected||expected<0?"DRAWN":"MISMATCH",delta);started=0;} }
 void unsupported(int category){unsupported[Math.max(0,Math.min(3,category))]++;record("UNSUPPORTED",0);}
 void event(String label){record(label,0);}
 private void record(String phase,long ms){while(rows.size()>=64)rows.removeFirst();rows.addLast(phase+" expected="+expected+" selected="+selected+" drawn="+drawn+" elapsedMs="+ms);}
 String summary(){StringBuilder b=new StringBuilder("表示診断（本文・写真なし）\n平均描画応答 "+(count==0?"未計測":latency/count+"ms")+" / "+count+"件\n未対応 "+Arrays.toString(unsupported)+"\n");for(String s:rows)b.append(s).append('\n');return b.toString();}
 String snapshot(){return summary();}
}
