package com.example.rokidkeyboardbridge;
final class FullBodySeatedPolicy {
 static final int[][] REGIONS={{46,9,308,744},{381,9,643,744},{730,9,992,744},{46,752,308,1487},{380,752,642,1487},{722,752,984,1487}};
 static int frame(long elapsed){long t=Math.max(0,elapsed)%180000;if(t<60000)return 0;if(t<60150)return 1;if(t<90000)return 2;if(t<90150)return 1;if(t<135000)return 3;if(t<135150)return 4;if(t<165000)return 5;if(t<165150)return 4;return 3;}
 static int waitingPose(long elapsed){long t=Math.max(0,elapsed)%300000;return t<180000?168:t<240000?167:168;}
}
