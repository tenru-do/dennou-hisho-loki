package com.example.rokidgeminisecretary;
import android.content.*;import org.json.*;import java.io.*;
final class ProfilePresets {
 static JSONObject edits(String mode)throws Exception{int normal,navigation,meeting;if(mode.equals("outside")){normal=50;navigation=65;meeting=15;}else if(mode.equals("meeting")){normal=25;navigation=40;meeting=10;}else if(mode.equals("home")){normal=40;navigation=50;meeting=15;}else throw new IOException("設定セットを選んでください");return new JSONObject().put("enabled",true).put("normal",normal).put("navigation",navigation).put("meeting",meeting);}
 static JSONObject before(Context c)throws Exception{JSONObject out=new JSONObject();SharedPreferences p=c.getSharedPreferences("device_tuning",0);for(String key:new String[]{"enabled","normal","navigation","meeting"})out.put(key,p.getAll().containsKey(key)?p.getAll().get(key):JSONObject.NULL);return out;}
 static JSONObject preview(Context c,String mode)throws Exception{JSONObject old=before(c);return new JSONObject().put("mode",mode).put("before",old).put("after",edits(mode)).put("revision",ReviewStore.hash(old)).put("message","このスマホのメディア音量プロファイルだけを変更します。同じBluetooth出力の音量にも影響する場合があります。録音・撮影・再生開始・外部送信は行いません。手動変更後は取り消しを拒否する場合があります。");}
 static synchronized void apply(Context c,String mode,String revision)throws Exception{if(!ReviewStore.hash(before(c)).equals(revision))throw new IOException("設定が更新されています。差分を開き直してください");SettingsJournal.change(c,"device_tuning",edits(mode));DeviceTuning.restore(c);}
 private ProfilePresets(){}
}
