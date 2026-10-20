package com.example.rokidgeminisecretary;
import org.json.*;
/** Flags explicit uncertainty statements only, never estimates OCR confidence. */
final class ReadingUncertainty {
 static JSONArray lines(String text){JSONArray out=new JSONArray();if(text==null)return out;for(String line:text.split("[\\r\\n。]+")){String s=line.trim();if(s.matches("(?s).*(不鮮明|読み取れ|判読でき|判別でき|未確認|不明|記載がない|記載されていない|確認が必要|確認してください).*")){out.put(s.substring(0,Math.min(240,s.length())));if(out.length()==6)break;}}return out;}
 static String label(String text){JSONArray rows=lines(text);StringBuilder out=new StringBuilder(rows.length()==0?"不確かな箇所の明記は見つかりません。読み取りの正しさを保証する判定ではありません。":"読み取り結果にある確認事項（推測で補いません）");for(int i=0;i<rows.length();i++)out.append("\n・").append(rows.optString(i));return out.toString();}
 private ReadingUncertainty(){}
}
