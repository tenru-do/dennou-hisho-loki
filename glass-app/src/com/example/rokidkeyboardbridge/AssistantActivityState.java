package com.example.rokidkeyboardbridge;
import org.json.*;
/** Content-free activity stages. A waiting stage is not proof of provider success. */
final class AssistantActivityState {
 static String phase(boolean enabled,boolean recording,boolean recognizing,boolean analyzing,boolean speaking,boolean paused){
  if(speaking)return "SPEAKING";if(recording)return "RECORDING";if(recognizing)return "RECOGNIZING";if(analyzing)return "ANALYZING";if(!enabled)return "OFF";return paused?"PAUSED":"WAITING";
 }
 static JSONObject safe(JSONObject input)throws Exception{JSONObject out=new JSONObject().put("at",Math.max(0,input.optLong("at")));for(String key:new String[]{"voice","topic"}){String p=input.optString(key);out.put(key,p.matches("OFF|SPEAKING|RECORDING|RECOGNIZING|ANALYZING|PAUSED|WAITING")?p:"UNKNOWN");}String reason=input.optString("pauseReason");return out.put("pauseReason",reason.matches("NAVIGATION|BACKGROUND|USER_TURN|MENU|STOPPING")?reason:"");}
 static String name(String p){return p.equals("OFF")?"停止中":p.equals("SPEAKING")?"読み上げ中":p.equals("RECORDING")?"聞き取り中":p.equals("RECOGNIZING")?"音声認識中":p.equals("ANALYZING")?"解析・回答待ち":p.equals("PAUSED")?"一時待機":p.equals("WAITING")?"次の入力・話題待ち":"状態未確認";}
 static String describe(JSONObject j,boolean fresh){if(j==null)return "TOPIC・VOICE：状態未受信";String reason=j.optString("pauseReason"),label=reason.equals("NAVIGATION")?"ナビ優先":reason.equals("BACKGROUND")?"画面が非表示":reason.equals("USER_TURN")?"利用者の会話・入力を優先":reason.equals("MENU")?"メニュー操作中":reason.equals("STOPPING")?"停止処理中":"";return (fresh?"":"前回受信の状態（現在は未確認）\n")+"VOICE："+name(j.optString("voice"))+"\nTOPIC："+name(j.optString("topic"))+(label.isEmpty()?"":"\n待機理由："+label)+"\n解析待ちは成功を示しません。エラーや依頼結果は下の記録で確認してください。";}
 private AssistantActivityState(){}
}
