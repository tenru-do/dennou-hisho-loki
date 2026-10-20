package com.example.rokidgeminisecretary;

import android.Manifest;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import org.json.*;
import java.time.*;
import java.util.ArrayList;

/** Writes only to one explicitly named, writable Google calendar. */
final class CalendarWriter {
    static boolean permitted(Context c) {
        return c.checkSelfPermission(Manifest.permission.READ_CALENDAR)==0
                && c.checkSelfPermission(Manifest.permission.WRITE_CALENDAR)==0;
    }
    static JSONArray calendars(Context c) throws Exception {
        JSONArray out=new JSONArray();
        if(!permitted(c)) return out;
        try(Cursor rows=c.getContentResolver().query(CalendarContract.Calendars.CONTENT_URI,
                new String[]{"_id","calendar_displayName"},
                "account_type=? AND calendar_access_level>=? AND sync_events=1",
                new String[]{"com.google",String.valueOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR)},null)) {
            if(rows!=null)while(rows.moveToNext())out.put(new JSONObject().put("id",rows.getLong(0)).put("name",rows.getString(1)));
        }
        return out;
    }
    /** Reads existing events only. No insert, update, delete or sync request. */
    static JSONObject preview(Context c,JSONObject request)throws Exception{
        JSONObject checked=CalendarDraftEdits.apply(request,request);String summary=CalendarReviewSummary.describe(checked);
        if(!permitted(c))return new JSONObject().put("ok",false).put("message",summary+"\n重複確認：カレンダー権限がありません。まだ登録していません。");
        String name=checked.getString("calendar_name");long calendar=-1;int matches=0;JSONArray choices=calendars(c);for(int i=0;i<choices.length();i++){JSONObject row=choices.getJSONObject(i);if(name.equals(row.getString("name"))){matches++;calendar=row.getLong("id");}}if(matches!=1)return new JSONObject().put("ok",false).put("message",summary+"\n登録先を一意に確認できません。同期・書き込み権限・同名カレンダーを確認してください。");
        int exact=0,overlap=0;JSONArray events=checked.getJSONArray("events");for(int i=0;i<events.length();i++){JSONObject e=events.getJSONObject(i);boolean day=e.getBoolean("all_day");long a=timestamp(e.getString("start"),day),b=timestamp(e.getString("end"),day);boolean duplicate=false;
            try(Cursor rows=c.getContentResolver().query(CalendarContract.Events.CONTENT_URI,new String[]{"title","dtstart","dtend","allDay"},"calendar_id=? AND dtstart<? AND dtend>? AND deleted=0",new String[]{""+calendar,""+b,""+a},null)){if(rows==null)throw new java.io.IOException("重複確認を取得できません。まだ登録していません");while(rows.moveToNext()){if(e.getString("title").equals(rows.getString(0))&&a==rows.getLong(1)&&b==rows.getLong(2)&&(day?1:0)==rows.getLong(3))duplicate=true;else overlap++;}}
            for(int j=0;j<i;j++){JSONObject prior=events.getJSONObject(j);if(prior.getString("title").equals(e.getString("title"))&&timestamp(prior.getString("start"),prior.getBoolean("all_day"))==a&&timestamp(prior.getString("end"),prior.getBoolean("all_day"))==b&&prior.getBoolean("all_day")==day)duplicate=true;}if(duplicate)exact++;
        }return new JSONObject().put("ok",true).put("exactDuplicates",exact).put("overlappingMatches",overlap).put("message",summary+"\n同一予定候補："+exact+"件 / 時間帯が重なる既存予定との組合せ："+overlap+"件\nまだ登録していません。重複は登録直前にも再確認します。繰り返し予定の各回や未同期の予定はこの確認に含まれません。");
    }
    static synchronized JSONObject save(Context c,JSONObject request) throws Exception {
        if(!permitted(c))return new JSONObject().put("ok",false).put("message","スマホのロキにカレンダーの読み書き権限を許可してください。まだ登録していません。");
        String name=request.getString("calendar_name").trim();
        JSONArray choices=calendars(c);long calendar=-1;int matches=0;
        for(int i=0;i<choices.length();i++){JSONObject row=choices.getJSONObject(i);
            if(name.equals(row.getString("name"))){calendar=row.getLong("id");matches++;}}
        if(matches!=1)return new JSONObject().put("ok",false).put("message",matches==0
                ? "指定したGoogleカレンダーが同期されていないか、書き込み権限がありません。登録先を確認してください。"
                : "同じ名前のカレンダーが複数あります。スマホで名前を区別してから指定してください。").put("calendars",choices);
        JSONArray events=request.getJSONArray("events");
        if(events.length()<1||events.length()>10)throw new IllegalArgumentException("予定は1〜10件で指定してください。");
        ArrayList<ContentProviderOperation> batch=new ArrayList<>();
        ArrayList<ContentValues> values=new ArrayList<>();
        int existing=0;
        for(int i=0;i<events.length();i++) {
            JSONObject e=events.getJSONObject(i);String title=e.getString("title").trim();
            if(title.isEmpty()||title.length()>200)throw new IllegalArgumentException("予定名を確認してください。");
            boolean allDay=e.getBoolean("all_day");
            long start=timestamp(e.getString("start"),allDay),end=timestamp(e.getString("end"),allDay);
            if(end<=start||end-start>366L*86400000)throw new IllegalArgumentException("開始・終了日時を確認してください。");
            ContentValues v=new ContentValues();v.put("calendar_id",calendar);v.put("title",title);
            v.put("dtstart",start);v.put("dtend",end);v.put("allDay",allDay?1:0);
            v.put("eventTimezone",allDay?"UTC":"Asia/Tokyo");
            v.put("eventLocation",e.optString("location",""));v.put("description",e.optString("description",""));
            if(v.getAsString("description").length()>10000||v.getAsString("eventLocation").length()>500)throw new IllegalArgumentException("予定の説明が長すぎます。");
            boolean duplicate=false;
            try(Cursor rows=c.getContentResolver().query(CalendarContract.Events.CONTENT_URI,new String[]{"_id"},
                    "calendar_id=? AND title=? AND dtstart=? AND dtend=? AND allDay=? AND deleted=0",
                    new String[]{""+calendar,title,""+start,""+end,allDay?"1":"0"},null)){if(rows==null)throw new java.io.IOException("既存予定を確認できないため登録していません。スマホのカレンダーを確認してから再操作してください。");duplicate=rows.moveToFirst();}
            for(ContentValues prior:values)if(prior.getAsString("title").equals(title)&&prior.getAsLong("dtstart")==start&&prior.getAsLong("dtend")==end&&prior.getAsLong("allDay")== (allDay?1:0))duplicate=true;
            if(duplicate){existing++;continue;}
            values.add(v);batch.add(ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI).withValues(v).build());
        }
        ContentProviderResult[] results=batch.isEmpty()?new ContentProviderResult[0]
                :c.getContentResolver().applyBatch(CalendarContract.AUTHORITY,batch);
        if(results.length!=batch.size())throw new IllegalStateException("保存結果を確認できません。再送する前にカレンダーを確認してください。");
        for(int i=0;i<results.length;i++) {
            Uri uri=results[i].uri;
            try(Cursor row=uri==null?null:c.getContentResolver().query(uri,new String[]{"title","dtstart","dtend","calendar_id","allDay"},null,null,null)) {
                ContentValues v=values.get(i);
                if(row==null||!row.moveToFirst()||!v.getAsString("title").equals(row.getString(0))
                        ||row.getLong(1)!=v.getAsLong("dtstart")||row.getLong(2)!=v.getAsLong("dtend")||row.getLong(3)!=calendar||row.getLong(4)!=v.getAsLong("allDay"))
                    throw new IllegalStateException("保存結果を確認できません。カレンダーを確認してください。");
            }
        }
        return new JSONObject().put("ok",true).put("created",results.length).put("existing",existing)
                .put("message","『"+name+"』に"+results.length+"件登録しました。"+(existing>0?existing+"件は同じ予定が既にあるため追加していません。":"")
                +"スマホ上の保存を確認しました。Googleへの同期はスマホの通信・同期設定に従います。");
    }
    static long timestamp(String value,boolean allDay) {
        return allDay?LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                :OffsetDateTime.parse(value).toInstant().toEpochMilli();
    }
}
