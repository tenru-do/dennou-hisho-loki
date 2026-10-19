package com.example.rokidgeminisecretary;
import org.json.*;
public class TrackedWebRequestTest {
 static int n;static void ok(boolean b,String s){if(!b)throw new AssertionError(s);n++;}
 static JSONObject record(String id)throws Exception{return new JSONObject().put("key",id).put("tracked",true).put("message","同じ依頼文").put("requestState","QUEUED");}
 public static void main(String[]args)throws Exception{
  String a="a".repeat(64),b="b".repeat(64),text="同じ依頼文\n条件A:B\"C\"";
  String wire=TrackedWebRequest.wire(a,text);JSONObject parsed=TrackedWebRequest.parse(wire);
  ok(parsed.getString("id").equals(a)&&parsed.getString("text").equals(text),"body and multiline punctuation roundtrip");
  ok(!TrackedWebRequest.wire(a,text).equals(TrackedWebRequest.wire(b,text)),"identical bodies with different IDs independent");
  ok(TrackedWebRequest.wire(a,"x".repeat(2800)).length()<3000,"wire fits original queue limit without truncation");
  boolean bad=false;try{TrackedWebRequest.wire("short",text);}catch(Exception e){bad=true;}ok(bad,"malformed ID rejected");
  bad=false;try{TrackedWebRequest.wire(a,"__LOKI_PHOTO_SYNC__");}catch(Exception e){bad=true;}ok(bad,"browser cannot inject system command");
  bad=false;try{TrackedWebRequest.wire(a,"x".repeat(2801));}catch(Exception e){bad=true;}ok(bad,"overlong body rejected not split");
  bad=false;try{TrackedWebRequest.parse(TrackedWebRequest.PREFIX+a+";text");}catch(Exception e){bad=true;}ok(bad,"bad delimiter rejected");
  JSONObject q=record(a),received=TrackedWebRequest.receipt(q,"RECEIVED","",10);
  ok(received.getString("requestState").equals("RECEIVED")&&q.getString("requestState").equals("QUEUED"),"receipt copies record without mutating input");
  JSONObject running=TrackedWebRequest.receipt(received,"RUNNING","",11);
  ok(TrackedWebRequest.receipt(running,"RECEIVED","",12).toString().equals(running.toString()),"late received cannot regress running");
  JSONObject answered=TrackedWebRequest.receipt(running,"ANSWERED","",13);
  ok(TrackedWebRequest.receipt(answered,"FAILED","MODEL_ERROR",14).toString().equals(answered.toString()),"late failure cannot overwrite final result");
  JSONObject interrupted=TrackedWebRequest.receipt(received,"UNCONFIRMED","RESTART",15);
  ok(TrackedWebRequest.receipt(interrupted,"RUNNING","",16).toString().equals(interrupted.toString()),"restart stays unconfirmed never reexecutes");
  ok(TrackedWebRequest.receipt(q,"FAILED","SAFETY_STOP",17).getString("receiptReason").equals("SAFETY_STOP"),"safety reason contains no unsafe content");
  bad=false;try{TrackedWebRequest.receipt(q,"RUNNING","SECRET_KEY_OR_TEXT",18);}catch(Exception e){bad=true;}ok(bad,"arbitrary private reason rejected");
  bad=false;try{TrackedWebRequest.receipt(new JSONObject().put("key",a),"RECEIVED","",19);}catch(Exception e){bad=true;}ok(bad,"legacy untracked row not misidentified");
  bad=false;try{TrackedWebRequest.receipt(q,"CANCEL_ALL","",20);}catch(Exception e){bad=true;}ok(bad,"receipt cannot introduce operation");
  ok(TrackedWebRequest.receipt(q,"ANSWERED","",21).getString("requestState").equals("ANSWERED"),"terminal may arrive before intermediate receipt");
  ok(TrackedWebRequest.executionGrant("RECEIVED","RUNNING","RUNNING"),"first start granted");
  ok(!TrackedWebRequest.executionGrant("RUNNING","RUNNING","RUNNING"),"lost start response not granted again");
  ok(!TrackedWebRequest.executionGrant("ANSWERED","RECEIVED","ANSWERED"),"replayed completed envelope not executed");
  ok(!TrackedWebRequest.executionGrant("QUEUED","RUNNING","RUNNING"),"start requires durable receipt");
  String key="c".repeat(64);JSONObject bound=TrackedWebRequest.parse(TrackedWebRequest.wire(a,text,key));ok(bound.getString("text").equals(text),"bound text unchanged");ok(bound.getString("referenceKey").equals(key),"bound key survives queue");ok(TrackedWebRequest.wire(a,"x".repeat(2800),key).length()<3000,"bound maximum fits queue");ok(TrackedWebRequest.parse(wire).optString("referenceKey").isEmpty(),"legacy has no binding");bad=false;try{TrackedWebRequest.wire(a,text,"bad");}catch(Exception e){bad=true;}ok(bad,"bad reference key rejected");bad=false;try{TrackedWebRequest.parse(TrackedWebRequest.PREFIX+a+":__LOKI_REF_KEY__:bad:x");}catch(Exception e){bad=true;}ok(bad,"malformed reference envelope rejected");ok(TrackedWebRequest.receipt(q,"FAILED","REFERENCE_CHANGED",30).getString("receiptReason").equals("REFERENCE_CHANGED"),"reference failure retained");
  System.out.println("PASS tracked web requests "+n+" checks");
 }
}
