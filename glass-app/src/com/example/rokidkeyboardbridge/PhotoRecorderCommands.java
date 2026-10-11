package com.example.rokidkeyboardbridge;
final class PhotoRecorderCommands {
 static int action(String text){String t=text.replaceAll("[\\s。！!？?、]","");
  if(t.matches("(今の場面|この場面|さっきの場面)(を)?(残して|保存して|保護して)(ください)?"))return 1;
  if(t.matches("(連続撮影|ドラレコ)(を)?(開始して|始めて)(ください)?"))return 2;
  if(t.matches("(連続撮影|ドラレコ)(を)?(停止して|止めて|終了して)(ください)?"))return 3;
  return 0;
 }
}
