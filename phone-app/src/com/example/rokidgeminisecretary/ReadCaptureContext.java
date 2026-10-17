package com.example.rokidgeminisecretary;
import org.json.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
/** Capture provenance is separate from dates/places printed in the photographed document. */
final class ReadCaptureContext {
 static JSONObject safe(JSONObject location,long capturedAt)throws JSONException {
  JSONObject out=new JSONObject().put("capturedAt",capturedAt).put("available",false).put("readOverlay",true);
  if(location==null||!location.optBoolean("available")||!"phone".equals(location.optString("source")))return out;
  double lat=location.optDouble("latitude",Double.NaN),lng=location.optDouble("longitude",Double.NaN),accuracy=location.optDouble("accuracyMeters",Double.NaN);
  long at=location.optLong("locationAt",0),age=capturedAt-at;
  if(capturedAt<=0||at<=0||age<0||age>90000||!Double.isFinite(lat)||!Double.isFinite(lng)||Math.abs(lat)>90||Math.abs(lng)>180||!Double.isFinite(accuracy)||accuracy<0||accuracy>500)return out;
  return out.put("available",true).put("latitude",lat).put("longitude",lng).put("accuracyMeters",accuracy).put("locationAt",at).put("source","phone");
 }
 static String[] lines(JSONObject value)throws JSONException {
  long captured=value.getLong("capturedAt");JSONObject meta=safe(value,captured);
  String time=DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss 'JST'",Locale.JAPAN).withZone(ZoneId.of("Asia/Tokyo")).format(Instant.ofEpochMilli(captured));
  if(!meta.optBoolean("available"))return new String[]{"READ 撮影日時: "+time,"スマホ位置: 未取得（撮影場所は未確認）"};
  String point=String.format(Locale.US,"スマホ位置: %.5f, %.5f",meta.getDouble("latitude"),meta.getDouble("longitude"));
  return new String[]{"READ 撮影日時: "+time,point,"測位精度 ±"+(long)Math.ceil(meta.getDouble("accuracyMeters"))+"m / 撮影時から"+((captured-meta.getLong("locationAt"))/1000)+"秒前の測位"};
 }
 static String prompt(JSONObject meta)throws JSONException {
  return String.join("\n",lines(meta))+"\n画像の下端にアプリが追加した撮影情報です。撮影日時とスマホの測位位置であり、書類の予約日時・開催日・記載場所ではありません。スマホとグラスが同じ場所とは保証されません。画像内に年の記載がなければ撮影年を予約年として補わず確認してください。位置未取得なら撮影場所を断定しないでください。";
 }
}
