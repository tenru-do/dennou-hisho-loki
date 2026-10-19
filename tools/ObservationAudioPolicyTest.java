package com.example.rokidkeyboardbridge;
import java.util.Arrays;
public class ObservationAudioPolicyTest {
 static int checks;
 static void ok(boolean b,String m){if(!b)throw new AssertionError(m);checks++;}
 static long simulation(boolean pause,boolean continuous){ObservationAudioPolicy p=new ObservationAudioPolicy();for(int ms=0;ms<=12000;ms+=20){boolean signal=continuous||(ms<2000||(pause&&ms>=3800&&ms<7000));p.observe(ms,signal?120:12);if(p.stop(ms,ms*32))return ms;}throw new AssertionError();}
 public static void main(String[] a){
 ok(simulation(true,false)>=9180,"1.8s hesitation retains following phrase and trailing pause");
 ok(simulation(false,false)>=4180,"stop after complete sentence plus 2.2s silence");
 ok(simulation(false,true)==12000,"continuous sound hard bound");
 ObservationAudioPolicy legacy=new ObservationAudioPolicy(4000);legacy.observe(3980,120);ok(legacy.stop(4000,128000),"legacy phone remains within foursecond receiver");
 ObservationAudioPolicy silent=new ObservationAudioPolicy();ok(!silent.stop(3999,1000)&&silent.stop(4000,128000),"ambient silence sample retains four seconds");
 ok(silent.stop(1,ObservationAudioPolicy.MAX_BYTES),"PCM memory bound independent of clock");
 MeetingPcmRing ring=new MeetingPcmRing();byte[] input=new byte[ObservationAudioPolicy.MAX_BYTES+64000];for(int i=0;i<input.length;i++)input[i]=(byte)(i%251);
 ring.put(input,input.length);byte[] copy=ring.snapshot();ok(copy.length==384000,"shared MTG window12s");ok(Arrays.equals(copy,Arrays.copyOfRange(input,64000,input.length)),"chronological latest samples");copy[0]=0;ok(ring.snapshot()[0]==input[64000],"snapshot does not mutate recorder");
 System.out.println("PASS observation audio "+checks+" checks; hesitation, long sentence, silence, size bound, MTG ordering");
 }
}
