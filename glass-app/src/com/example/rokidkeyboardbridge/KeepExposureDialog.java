package com.example.rokidkeyboardbridge;
import android.app.AlertDialog;import android.content.Context;import java.util.function.IntConsumer;
/** Keep the selection list separate from explanatory message content on native Android. */
final class KeepExposureDialog {
 static AlertDialog create(Context c,int current,IntConsumer changed){
  return new LokiAlertDialogBuilder(c).setTitle("KEEPの撮影の明るさ")
   .setSingleChoiceItems(new String[]{KeepExposurePolicy.label(-1),KeepExposurePolicy.label(0),KeepExposurePolicy.label(1)},KeepExposurePolicy.mode(current)+1,(dialog,index)->{changed.accept(index-1);dialog.dismiss();})
   .setNeutralButton("調整の注意点",(dialog,which)->new LokiAlertDialogBuilder(c).setTitle("撮影の明るさの調整").setMessage("自動は撮影直前の縮小画像で明るさを判定し、必要なときだけ露出を補正します。白飛びを抑え、暗所はブレ・ノイズを考慮して控えめに明るくします。手動設定も次のKEEP撮影から適用します。保存済み写真は変更せず、白飛びした部分の復元はできません。").setPositiveButton("閉じる",null).show())
   .setNegativeButton("戻る",null).create();
 }
 private KeepExposureDialog(){}
}
