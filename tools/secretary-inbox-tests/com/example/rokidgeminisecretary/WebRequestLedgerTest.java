package com.example.rokidgeminisecretary;
import org.json.*;
public class WebRequestLedgerTest{
 static int n;static void ok(boolean value,String label){if(!value)throw new AssertionError(label);n++;}
 static JSONObject row(int number,String state)throws Exception{return new JSONObject().put("key",String.format("%064x",number)).put("tracked",true).put("requestState",state);}
 public static void main(String[]args)throws Exception{
  JSONArray old=new JSONArray();old.put(row(1,"RUNNING"));for(int i=2;i<=100;i++)old.put(row(i,"ANSWERED"));String before=old.toString();
  JSONArray next=WebRequestLedger.append(old,row(101,"CREATED"));ok(next.length()==100,"bounded length");ok(next.getJSONObject(0).getString("requestState").equals("RUNNING"),"old unresolved survives");ok(next.getJSONObject(1).getString("key").equals(String.format("%064x",3)),"oldest terminal evicted");ok(old.toString().equals(before),"input not mutated");
  JSONArray busy=new JSONArray();for(int i=1;i<=100;i++)busy.put(row(i,"QUEUED"));ok(WebRequestLedger.append(busy,row(101,"CREATED"))==null&&busy.length()==100,"full unresolved ledger rejects new request");
  old=new JSONArray().put(new JSONObject().put("message","legacy"));for(int i=2;i<=100;i++)old.put(row(i,"RECEIVED"));next=WebRequestLedger.append(old,row(101,"CREATED"));ok(next.getJSONObject(0).getString("requestState").equals("RECEIVED"),"legacy evicted before pending");
  boolean rejected=false;try{WebRequestLedger.append(next,row(101,"CREATED"));}catch(IllegalArgumentException e){rejected=true;}ok(rejected,"duplicate ID cannot be appended");
  JSONArray oversized=new JSONArray();for(int i=1;i<=110;i++)oversized.put(row(i,"FAILED"));ok(WebRequestLedger.append(oversized,row(111,"CREATED")).length()==100,"oversized terminal migration bounded");
  System.out.println("PASS request ledger retention "+n+" checks");
 }
}
