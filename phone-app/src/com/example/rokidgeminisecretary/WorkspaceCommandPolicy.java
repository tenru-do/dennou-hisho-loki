package com.example.rokidgeminisecretary;
import org.json.*;
/** Idempotent command comparison; never executes or changes stored commands. */
final class WorkspaceCommandPolicy {
 static boolean validId(String id){return id!=null&&id.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}");}
 static boolean pendingFresh(JSONObject command,long now){long expires=command.optLong("expiresAt");return expires>now&&expires-now<=600000;}
 static boolean sameCommand(JSONObject a,JSONObject b)throws Exception{
  JSONObject left=new JSONObject(a.toString()),right=new JSONObject(b.toString());
  for(String field:new String[]{"queued","expiresAt"}){left.remove(field);right.remove(field);}
  return same(left,right);
 }
 static boolean same(Object a,Object b)throws Exception{
  if(a==JSONObject.NULL||a==null)return b==JSONObject.NULL||b==null;
  if(a instanceof JSONObject&&b instanceof JSONObject){JSONObject x=(JSONObject)a,y=(JSONObject)b;if(x.length()!=y.length())return false;java.util.Iterator<String> keys=x.keys();while(keys.hasNext()){String k=keys.next();if(!y.has(k)||!same(x.get(k),y.get(k)))return false;}return true;}
  if(a instanceof JSONArray&&b instanceof JSONArray){JSONArray x=(JSONArray)a,y=(JSONArray)b;if(x.length()!=y.length())return false;for(int i=0;i<x.length();i++)if(!same(x.get(i),y.get(i)))return false;return true;}
  if(a instanceof Number&&b instanceof Number)return new java.math.BigDecimal(a.toString()).compareTo(new java.math.BigDecimal(b.toString()))==0;
  return a.equals(b);
 }
 private WorkspaceCommandPolicy(){}
}
