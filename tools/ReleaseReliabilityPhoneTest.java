package com.example.rokidgeminisecretary;
import org.json.*;
public class ReleaseReliabilityPhoneTest {
 static int count;static void check(boolean yes){count++;if(!yes)throw new AssertionError("phone case "+count);}
 public static void main(String[] args)throws Exception{
 String id="a".repeat(64);JSONObject row=new JSONObject().put("key",id).put("tracked",true).put("requestState","RUNNING").put("message","合成会話").put("time",1);check(ComposerReceiptSummary.describe(new JSONArray().put(row)).contains("処理中"));row.put("tracked",false);check(ComposerReceiptSummary.describe(new JSONArray().put(row)).contains("結果未確認"));row.put("tracked",true).put("key","bad");check(ComposerReceiptSummary.describe(new JSONArray().put(row)).contains("結果未確認"));check(ComposerReceiptSummary.describe(new JSONArray()).contains("まだありません"));
 for(String state:new String[]{"CREATED","RECEIVED","ANSWERED","FAILED","UNCONFIRMED"}){row.put("key",id).put("requestState",state);check(!ComposerReceiptSummary.describe(new JSONArray().put(row)).contains("null"));}
 JSONObject event=new JSONObject().put("title","合成予定").put("start","2026-11-01").put("end","2026-11-03").put("all_day",true);JSONObject c=new JSONObject().put("calendar_name","合成").put("events",new JSONArray().put(event));check(CalendarReviewSummary.describe(c).contains("2026-11-02"));check(CalendarReviewSummary.diff(c,c).contains("変更はありません"));JSONObject next=new JSONObject(c.toString());next.getJSONArray("events").getJSONObject(0).put("title","訂正");check(CalendarReviewSummary.diff(c,next).contains("合成予定 → 訂正"));event.put("end","bad");check(CalendarReviewSummary.describe(c).contains("不正"));check(ReadingUncertainty.label("文字が不鮮明です。").contains("不鮮明"));check(ReadingUncertainty.label("読めました。").contains("保証"));System.out.println("PASS phone reliability "+count);
 }
}
