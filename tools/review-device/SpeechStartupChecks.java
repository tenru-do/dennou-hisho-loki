package com.example.rokidgeminisecretary;
public final class SpeechStartupChecks {
 public static int run(){if(!SpeechRecognizerWarmup.fresh(100,30099)||SpeechRecognizerWarmup.fresh(100,30100)||SpeechRecognizerWarmup.fresh(100,99)||SpeechRecognizerWarmup.fresh(-1,100))throw new AssertionError("Speech preparation expiry");return 4;}
}
