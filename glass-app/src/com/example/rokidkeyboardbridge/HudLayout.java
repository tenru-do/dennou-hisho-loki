package com.example.rokidkeyboardbridge;
import android.app.*;import android.content.*;import android.view.*;import android.widget.*;import java.util.*;
/** Stable identifiers independent from dynamic PLAY/TOPIC captions. SET is always reachable. */
final class HudLayout {
 private final Activity owner;private final LinearLayout panel;private final LinkedHashMap<String,View> buttons=new LinkedHashMap<>();private static final String KEY="hud-button-order-v1";
 HudLayout(Activity owner,LinearLayout panel){this.owner=owner;this.panel=panel;String[] ids={"CAM","READ","PLAY","BT30","NAV","TOPIC","KEEP","VOICE","SET"};
  for(int i=0;i<panel.getChildCount();i++){View child=panel.getChildAt(i);if(!(child instanceof Button))continue;String label=((Button)child).getText().toString();for(String id:ids)if(label.startsWith(id)||id.equals("PLAY")&&child instanceof MusicToggleButton){buttons.put(id,child);break;}}
  if(buttons.containsKey("BT30")&&!prefs().getBoolean("bt30-top-restored-v641",false)){
   android.content.SharedPreferences.Editor edit=prefs().edit().putBoolean("bt30-top-restored-v641",true);
   String saved=prefs().getString(KEY,"");
   if(!saved.isEmpty()){
    ArrayList<String> restored=new ArrayList<>();for(String id:saved.split(","))if(buttons.containsKey(id)&&!id.equals("SET")&&!restored.contains(id))restored.add(id);
    if(!restored.contains("BT30")){int play=restored.indexOf("PLAY");restored.add(play<0?restored.size():play+1,"BT30");}
    restored.add("SET");edit.putString(KEY,android.text.TextUtils.join(",",restored));
   }
   edit.commit();
  }
  apply();
 }
 private android.content.SharedPreferences prefs(){return owner.getSharedPreferences("hud-layout",0);}
 private ArrayList<String> order(){ArrayList<String> out=new ArrayList<>();String saved=prefs().getString(KEY,"");if(saved.isEmpty()){for(String id:buttons.keySet())if(!id.equals("SET"))out.add(id);}else for(String id:saved.split(","))if(buttons.containsKey(id)&&!id.equals("SET")&&!out.contains(id))out.add(id);out.add("SET");return out;}
 private void apply(){ArrayList<String> visible=order();for(View view:buttons.values())panel.removeView(view);for(String id:visible){View view=buttons.get(id);if(view!=null){view.setVisibility(View.VISIBLE);panel.addView(view);}}}
 void showPresets(){NavigationChoiceDialog.show(owner,"トップの配置プリセット",new String[]{"会話中心：VOICE / TOPIC / PLAY","撮影中心：READ / CAM / KEEP / VOICE","移動中：NAV / VOICE / READ"},-1,(d,which)->{String value=HudPreset.order(new String[]{"conversation","camera","travel"}[which]);NavigationChoiceDialog.show(owner,"配置だけを切り替えます。各機能は開始しません",new String[]{"この配置に切り替える","戻る"},-1,(a,b)->{if(b==0){if(prefs().edit().putString(KEY,value).commit()){apply();View first=panel.getChildAt(0);if(first!=null)first.requestFocus();}else android.widget.Toast.makeText(owner,"配置を保存できません",1).show();}});});}
 void show(){ArrayList<String> order=order();ArrayList<String> options=new ArrayList<>();options.add("標準に戻す");for(String id:buttons.keySet())if(!id.equals("SET"))options.add(id+ (order.contains(id)?"：上へ移動／SETへ移す":"：トップに追加／開く"));
  NavigationChoiceDialog.show(owner,"トップのボタンを編集",options.toArray(new String[0]),-1,(d,which)->{if(which==0){prefs().edit().remove(KEY).commit();apply();return;}String id=new ArrayList<>(buttons.keySet()).stream().filter(x->!x.equals("SET")).toArray(String[]::new)[which-1];boolean shown=order.contains(id);new LokiAlertDialogBuilder(owner).setTitle(id).setItems(shown?new String[]{"先頭へ移動","SETへ移す","開く"}:new String[]{"トップの末尾に追加","開く"},(a,b)->{if(b==(shown?2:1)){buttons.get(id).performClick();return;}order.remove(id);order.remove("SET");if(shown&&b==0)order.add(0,id);else if(!shown)order.add(id);prefs().edit().putString(KEY,order.isEmpty()?"-":android.text.TextUtils.join(",",order)).commit();apply();}).show();});
 }
}
