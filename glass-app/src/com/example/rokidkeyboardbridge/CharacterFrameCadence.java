package com.example.rokidkeyboardbridge;
/** Reserve drawing work for speech/gestures; let recognition and input stay responsive. */
final class CharacterFrameCadence {
 static long delay(boolean speaking,boolean moving,boolean listening){long base=speaking||moving?33L:listening?66L:100L;if(CharacterChoreography.scene==1)base=Math.max(200,base);else if(CharacterChoreography.scene==2&&!speaking&&!listening)base=Math.max(83,base);return CharacterRenderBudget.delay(base);}
 private CharacterFrameCadence(){}
}
