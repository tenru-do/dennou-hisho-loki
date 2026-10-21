package com.example.rokidgeminisecretary;
import org.json.*;import java.io.*;
final class ReservationReceiptPolicy {
 static JSONArray checked(JSONArray rows)throws Exception{if(rows.length()>8)throw new IOException("予約結果が多すぎます");JSONArray out=new JSONArray();for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);String id=row.optString("id"),kind=row.optString("kind"),phase=row.optString("phase"),batch=row.optString("batchId");if(!id.matches("[a-f0-9-]{36}")||!kind.matches("photo|meeting")||!phase.matches("ACCEPTED|RUNNING|COMPLETED|PARTIAL|FAILED|REJECTED|INTERRUPTED|EXPIRED")||!batch.isEmpty()&&!batch.matches("[a-f0-9-]{36}"))continue;JSONObject safe=new JSONObject().put("id",id).put("kind",kind).put("phase",phase).put("reason",row.optString("reason").substring(0,Math.min(180,row.optString("reason").length())));if(!batch.isEmpty())safe.put("batchId",batch);for(String key:new String[]{"expected","confirmed","retained","failed","unitsExpected","unitsConfirmed"})safe.put(key,Math.max(0,Math.min(720000,row.optLong(key))));out.put(safe);}return out;}
 private ReservationReceiptPolicy(){}
}
