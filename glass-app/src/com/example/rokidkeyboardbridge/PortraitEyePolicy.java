package com.example.rokidkeyboardbridge;
/** Eye states in the approved 4x6 identity atlas, independent of the base expression. */
final class PortraitEyePolicy {
 static boolean closed(int cell,int eye){return cell==3||cell==10||cell==15||cell==17||cell==18||cell==22||cell==2&&eye==0;}
 private PortraitEyePolicy(){}
}
