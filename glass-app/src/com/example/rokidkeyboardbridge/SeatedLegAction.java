package com.example.rokidkeyboardbridge;
/** Display-side interpretation only: never changes provider prompts or saved dialogue. */
final class SeatedLegAction {
 static final int NONE=-1,SWITCH=3,CROSS=4;
 static int parse(String text){
  if(text==null)return NONE;
  String t=text.replace(" ","").replace("　","").replace('足','脚').replace("組み換","組み替").replace("組みかえ","組み替え");
  if(t.matches("(?s).*(ないで|しない|方法|意味|について|と言った|と言って|彼女|彼が|友人|写真|画像|追加|充実|実装|過程).*"))return NONE;
  if(t.matches("(?s).*(脚を(ほど|解)|脚の(組み|組んだ).*?(ほど|解)|両脚を(そろ|揃)|脚を(そろ|揃)).*"))return SeatedLegMotionPolicy.NEUTRAL;
  if(!t.contains("組"))return NONE;
  if(t.matches("(?s).*右脚.*(上|組).*"))return SeatedLegMotionPolicy.RIGHT;
  if(t.matches("(?s).*左脚.*(上|組).*"))return SeatedLegMotionPolicy.LEFT;
  if(t.matches("(?s).*(組み替えて|左右逆に|反対.*組|逆.*組).*"))return SWITCH;
  if(t.matches("(?s).*脚を組んで.*"))return CROSS;
  return NONE;
 }
}
