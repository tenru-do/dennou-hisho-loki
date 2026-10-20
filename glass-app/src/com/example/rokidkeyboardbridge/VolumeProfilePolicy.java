package com.example.rokidkeyboardbridge;
final class VolumeProfilePolicy {
 static int level(int percent,int max){return Math.max(0,Math.min(max,Math.round(Math.max(0,Math.min(100,percent))*max/100f)));}
 static boolean mayRestore(int current,int applied){return current==applied;}
 static String mode(boolean meeting,boolean navigation){return meeting?"meeting":navigation?"navigation":"normal";}
 private VolumeProfilePolicy(){}
}
