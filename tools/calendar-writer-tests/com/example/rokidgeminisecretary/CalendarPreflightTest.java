package com.example.rokidgeminisecretary;
import android.content.*;import android.database.Cursor;import android.net.Uri;import org.json.*;import java.util.*;
public class CalendarPreflightTest{
 static int checks;static void ok(boolean b){if(!b)throw new AssertionError(checks);checks++;}
 static class Rows implements Cursor{Object[][] rows;int i=-1;Rows(Object[]...v){rows=v;}public boolean moveToFirst(){i=0;return rows.length>0;}public boolean moveToNext(){return ++i<rows.length;}public long getLong(int n){return Long.parseLong(rows[i][n].toString());}public String getString(int n){return rows[i][n].toString();}public void close(){}}
 static class Provider extends ContentResolver{int reads,batches,eventReads;boolean nullRead,secondNull,duplicate,badDay;List<ContentValues> saved=new ArrayList<>();
 public Cursor query(Uri u,String[] projection,String selection,String[] args,String order){reads++;if(u.toString().equals("calendars"))return new Rows(new Object[]{7,"家族"});if(u.toString().equals("events")){eventReads++;ok(selection.contains("allDay=?")&&args.length==5);if(nullRead||(secondNull&&eventReads==2))return null;return duplicate?new Rows(new Object[]{9}):new Rows();}ContentValues v=saved.get(Integer.parseInt(u.toString().substring(4)));return new Rows(new Object[]{v.get("title"),v.get("dtstart"),v.get("dtend"),v.get("calendar_id"),badDay?9:v.get("allDay")});}
 public ContentProviderResult[] applyBatch(String a,ArrayList<ContentProviderOperation> ops){batches++;ContentProviderResult[] out=new ContentProviderResult[ops.size()];for(int i=0;i<ops.size();i++){saved.add(ops.get(i).values);out[i]=new ContentProviderResult(new Uri("row:"+i));}return out;}}
 static class Host extends Context{Provider p=new Provider();boolean allowed=true;public int checkSelfPermission(String permission){return allowed?0:-1;}public ContentResolver getContentResolver(){return p;}}
 static JSONObject event(String title,boolean day)throws Exception{return new JSONObject().put("title",title).put("all_day",day).put("start",day?"2026-11-01":"2026-11-01T00:00:00Z").put("end",day?"2026-11-02":"2026-11-02T00:00:00Z");}
 static JSONObject req(JSONObject...events)throws Exception{JSONArray a=new JSONArray();for(JSONObject e:events)a.put(e);return new JSONObject().put("calendar_name","家族").put("events",a);}
 static void stopped(Host h,JSONObject r)throws Exception{boolean failed=false;try{CalendarWriter.save(h,r);}catch(java.io.IOException e){failed=true;}ok(failed);ok(h.p.batches==0);}
 public static void main(String[] a)throws Exception{
 Host h=new Host();h.p.nullRead=true;stopped(h,req(event("A",true)));
 h=new Host();h.p.secondNull=true;stopped(h,req(event("A",true),event("B",true)));
 h=new Host();h.p.duplicate=true;JSONObject result=CalendarWriter.save(h,req(event("A",true)));ok(result.getInt("created")==0&&result.getInt("existing")==1);ok(h.p.batches==0);
 h=new Host();result=CalendarWriter.save(h,req(event("A",true),event("A",true)));ok(result.getInt("created")==1&&result.getInt("existing")==1);
 h=new Host();result=CalendarWriter.save(h,req(event("A",true),event("A",false)));ok(result.getInt("created")==2&&result.getInt("existing")==0);
 h=new Host();h.p.badDay=true;boolean failed=false;try{CalendarWriter.save(h,req(event("A",true)));}catch(IllegalStateException e){failed=true;}ok(failed&&h.p.batches==1);
 h=new Host();h.allowed=false;ok(!CalendarWriter.save(h,req(event("A",true))).getBoolean("ok")&&h.p.reads==0&&h.p.batches==0);
 System.out.println("PASS "+checks+" calendar preflight checks; Android provider doubles; real CalendarWriter; real calendar writes=0");
 }}