package com.example.rokidkeyboardbridge;
final class ResourceLoadPolicy {
 static int level(int battery,int temperatureDeciC,int thermal){return thermal>=3||temperatureDeciC>=450||battery>=0&&battery<=8?2:thermal>=2||temperatureDeciC>=410||battery>=0&&battery<=20?1:0;}
 static int withStorage(int level,long freeBytes){return freeBytes>=0&&freeBytes<100000000?2:freeBytes>=0&&freeBytes<300000000?Math.max(1,level):level;}
 static long interval(long requested,int level){return level>=2?Math.max(requested,60000):level==1?Math.max(requested,15000):requested;}
 static String label(int level){return level>=2?"発熱・低電池：録音優先、任意の撮影・画像判定は待機":level==1?"発熱・低電池：撮影間隔を延長し画像判定を抑制":"通常負荷";}
 private ResourceLoadPolicy(){}
}
