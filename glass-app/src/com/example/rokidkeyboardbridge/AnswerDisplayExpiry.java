package com.example.rokidkeyboardbridge;
final class AnswerDisplayExpiry {
 static final long DELAY_MS=3000;
 static boolean clear(int expectedRequest,int currentRequest,int expectedSpeech,int currentSpeech,String expected,String current,boolean busy){return !busy&&expectedRequest==currentRequest&&expectedSpeech==currentSpeech&&expected!=null&&!expected.isEmpty()&&expected.equals(current);}
 private AnswerDisplayExpiry(){}
}
