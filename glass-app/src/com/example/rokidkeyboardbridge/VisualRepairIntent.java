package com.example.rokidkeyboardbridge;
/** Only short, direct display controls. Navigation's generic 'back' stays untouched. */
final class VisualRepairIntent {
 static final int NONE=0,REPLAY=1,UNDO=2;
 static int kind(String text){String t=RestPoseIntent.normalize(text).replaceFirst("^(ロキ[、, :：]*)","");
 if(t.matches("(?:動いてない|動いていない|その姿勢じゃない|その姿勢ではない|動作をやり直して|さっきの動きをもう一度)[。！!\\s]*"))return REPLAY;
 if(t.matches("(?:戻って|前の姿勢に戻って|一つ前の姿勢に戻して|ひとつ前の姿勢に戻して)[。！!\\s]*"))return UNDO;return NONE;}
}
