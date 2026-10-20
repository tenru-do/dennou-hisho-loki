package com.example.rokidgeminisecretary;
import org.json.*;
/** Display observed hops only; never infers delivery or retries a turn. */
final class ConnectionCheckpoint {
 static String describe(JSONObject state,long now){long received=state.optLong("receivedAt");String link=state.optBoolean("connected")?"スマホ↔グラス：最近の状態受信あり":"スマホ↔グラス：接続状態を確認できません";if(received>0&&now>=received)link+="（"+Math.min(7200,(now-received)/1000)+"秒前）";JSONObject c=state.optJSONObject("conversation");if(c==null)return link+"\nGemini処理：結果情報なし。通信復帰だけでは再送しません。";String phase=c.optString("phase"),reason=c.optString("reason");String stage=phase.equals("ANSWERED")?"回答完了の記録あり":phase.equals("FAILED")?"回答未完了の記録あり":phase.equals("RUNNING")?"グラスで処理中の記録あり":"結果未確認";String cause=reason.equals("CONNECTION")?"Geminiへの接続処理でエラー":reason.equals("TIMEOUT")?"応答待ち時間を超過":reason.equals("AUTHORIZATION")?"認証を確認してください":reason.equals("RATE_LIMIT")?"利用制限":reason.equals("SERVICE_UNAVAILABLE")?"サービス側の利用不能":reason.equals("INTERRUPTED")||reason.equals("RESTART")?"処理の中断・再起動":"";return link+"\nGemini処理："+stage+(state.optBoolean("connected")?"":"（現在は未確認）")+(cause.isEmpty()?"":" / "+cause)+"\nスマホ↔グラスの接続とGeminiの応答は別です。結果未確認の依頼を自動再送しません。";}
 private ConnectionCheckpoint(){}
}
