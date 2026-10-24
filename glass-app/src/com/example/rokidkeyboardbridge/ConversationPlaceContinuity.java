package com.example.rokidkeyboardbridge;
import org.json.*;import java.util.*;import java.util.regex.*;
/** Bounded intent resolution from conversation only; never uses GPS or adds place facts. */
final class ConversationPlaceContinuity {
 static final Pattern POI=Pattern.compile("[一-龥々ヶヵァ-ヶA-Za-z0-9・]{1,30}(?:美術館|博物館|水族館|動物園|遊園地|神社|神宮|大社|公園|空港|温泉|寺|駅|城|湖|沼)");
 static final Pattern REGION=Pattern.compile("[一-龥々ヶ]{1,24}(?:県|都|府|道|市|区|町|村)");
 static boolean spatial(String text){return text!=null&&text.matches("(?s).*(近く|周辺|付近|近隣).*")&&!text.matches("(?s).*(入力欄|ボタン|ファイル|コード|変数|設定画面|写真|画像|スクリーン).*");}
 static boolean physical(String text){return text!=null&&text.matches("(?s).*(現在地|今いる|今居る|ここ|この辺|自宅|自分の近く|私の近く).*");}
 static boolean actionLike(String text){return text!=null&&text.matches("(?s).*(予約|登録|保存|削除|送信|送って|購入|注文|録音|撮影|転送|電話して|電話を|ナビ.*開始|案内.*開始).*");}
 static boolean regionalQuestion(String text){return text!=null&&text.matches("(?s).*(?:(?:北海道|青森|岩手|宮城|秋田|山形|福島|茨城|栃木|群馬|埼玉|千葉|東京|神奈川|新潟|富山|石川|福井|山梨|長野|岐阜|静岡|愛知|三重|滋賀|京都|大阪|兵庫|奈良|和歌山|鳥取|島根|岡山|広島|山口|徳島|香川|愛媛|高知|福岡|佐賀|長崎|熊本|大分|宮崎|鹿児島|沖縄)(?:県|都|府)?|[一-龥々]{2,12}(?:市|区|県|都|府))(?:じゃ|では)なかった(?:っけ|ですか|かな).*");}
 static boolean correction(String text){return text!=null&&text.matches("(?s).*(違う|ちがう|ではなく|じゃなく|の話|のこと).*")&&!ConversationTopicBoundary.reset(text);}
 static String unique(Pattern pattern,String text){LinkedHashSet<String> found=new LinkedHashSet<>();Matcher m=pattern.matcher(text);while(m.find()){String s=m.group();if(!s.equals("お寺")&&!s.equals("場所")&&!s.equals("近くの公園"))found.add(s);}return found.size()==1?found.iterator().next():"";}
 static boolean multiple(Pattern pattern,String text){java.util.HashSet<String> set=new java.util.HashSet<>();Matcher m=pattern.matcher(text);while(m.find())if(set.add(m.group())&&set.size()>1)return true;return false;}
 static String clean(String text){if(text==null)return "";int source=text.indexOf("情報元は");if(source>=0)text=text.substring(0,source);return text.replaceAll("https?://[^\\s]+", "");}
 static String userPlace(String text){String t=clean(text).trim();String poi=unique(POI,t);if(!poi.isEmpty())return poi;
  Matcher m=Pattern.compile("^(.{2,48}?)(?:は|って|の所在地|の住所)(?:どこ|何処|は|を教えて|を知りたい).*").matcher(t);if(m.matches()){String name=m.group(1).replaceFirst("^(?:では|じゃあ|ところで)[、\\s]*", "");if(!name.matches(".*(?:これ|それ|あれ|ここ|そこ|原因|問題|エラー|設定|ファイル|写真|画像).*"))return name;}
  return "";
 }
 static String discussed(JSONArray history,long now){for(int i=history.length()-1;i>=Math.max(0,history.length()-8);i--){JSONObject row=history.optJSONObject(i);if(row==null||!ConversationFollowupPolicy.fresh(row.optLong("time"),now))continue;String user=row.optString("user");if(ConversationTopicBoundary.reset(user))break;if(physical(user))return "";String explicit=userPlace(user);if(!explicit.isEmpty())return explicit;String answer=clean(row.optString("assistant"));String poi=unique(POI,answer);if(!poi.isEmpty())return poi;if(multiple(POI,answer))return "";String region=unique(REGION,answer);if(!region.isEmpty())return region;}return "";}
 static String pendingSpatial(JSONArray history,long now){for(int i=history.length()-1;i>=Math.max(0,history.length()-8);i--){JSONObject row=history.optJSONObject(i);if(row==null||!ConversationFollowupPolicy.fresh(row.optLong("time"),now))continue;String user=row.optString("user");if(ConversationTopicBoundary.reset(user)||physical(user))break;if(spatial(user)&&!actionLike(user))return user;}return "";}
 static String qualify(String text,String target){Matcher m=Pattern.compile("近く|周辺|付近|近隣").matcher(text);return m.find()?target+"の"+text.substring(m.start()):text;}
 static String resolve(String current,JSONArray history,long lastAt,long now){
  if(current==null||history==null||!ConversationFollowupPolicy.fresh(lastAt,now)||ConversationTopicBoundary.reset(current)||physical(current)||actionLike(current))return current;
  if(correction(current)){String target=userPlace(current),pending=pendingSpatial(history,now);if(!target.isEmpty()&&!pending.isEmpty()&&!spatial(current))return qualify(pending,target);}
  if(spatial(current)){if(POI.matcher(clean(current)).find()||REGION.matcher(clean(current)).find()||!userPlace(current).isEmpty())return current;String target=discussed(history,now);return target.isEmpty()?current:qualify(current,target);}
  // A tentative correction is a request to check the same subject, not a new topic.
  if(regionalQuestion(current)){String target=discussed(history,now);if(!target.isEmpty())return target+"について、「"+current+"」という確認です。公式情報を検索して所在地を再確認し、確認結果から簡潔に答えてください。『以前お伝えした通り』『ご確認くださいませ』と突き放す表現は使わないでください。";}
  return current;
 }
 private ConversationPlaceContinuity(){}
}
