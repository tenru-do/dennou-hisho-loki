package com.example.rokidgeminisecretary;
import org.json.*;
final class NavigationTripReview {
 static String text(JSONObject route)throws Exception{if(route==null)return "経路候補を選択・取得してから確認してください。まだ案内は開始しません。";JSONArray legs=route.optJSONArray("legs");if(legs==null||legs.length()==0)return "区間情報がありません。経路候補の内容を確認してください。";StringBuilder out=new StringBuilder("取得した経路の区間順です。運行・乗車済みを保証するものではありません。\n");int count=0;
  for(int i=0;i<legs.length();i++){JSONArray steps=legs.getJSONObject(i).optJSONArray("steps");if(steps==null)continue;for(int j=0;j<steps.length();j++){if(count>=40){out.append("以降の区間は表示上限です。詳細を地図で確認してください。");return out.toString();}JSONObject step=steps.getJSONObject(j);String mode=step.optString("travelMode");String label="TRANSIT".equals(mode)?"公共交通":"WALK".equals(mode)?"徒歩":"BICYCLE".equals(mode)?"自転車":"DRIVE".equals(mode)?"車":"移動（種別未確認）";out.append(++count).append(". ").append(label);JSONObject details=step.optJSONObject("transitDetails"),stops=details==null?null:details.optJSONObject("stopDetails");if(stops!=null){JSONObject from=stops.optJSONObject("departureStop"),to=stops.optJSONObject("arrivalStop");out.append("：").append(from==null?"未確認":from.optString("name","未確認")).append(" → ").append(to==null?"未確認":to.optString("name","未確認"));JSONObject line=details.optJSONObject("transitLine");if(line!=null)out.append(" ／ ").append(line.optString("name"));}else{JSONObject nav=step.optJSONObject("navigationInstruction");if(nav!=null)out.append("：").append(nav.optString("instructions"));}out.append('\n');}}
  return count==0?"表示できる区間情報がありません。":out.toString();}
 static boolean inputBound(String destination,String input){return destination!=null&&!destination.isEmpty()&&input!=null&&destination.equals(input.trim());}
 static boolean missedAllowed(String mode,String phase){return "transit".equals(mode)&&!"TRANSIT_LEG".equals(phase);}
 static String stairs(boolean requested,boolean navitime){return !requested?"階段回避の希望：未設定":navitime?"階段回避をNAVITIMEへ要求（契約・区間対応は未確認、階段がない保証ではありません）":"階段回避を希望：現在の経路提供元では対応未確認";}
 private NavigationTripReview(){}
}
