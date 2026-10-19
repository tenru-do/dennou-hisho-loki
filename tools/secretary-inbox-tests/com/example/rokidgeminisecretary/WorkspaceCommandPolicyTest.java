package com.example.rokidgeminisecretary;
import org.json.*;
public class WorkspaceCommandPolicyTest{
 static int n;static void ok(boolean b){if(!b)throw new AssertionError(n);n++;}
 public static void main(String[] args)throws Exception{
  String id="11111111-1111-1111-1111-111111111111";
  ok(WorkspaceCommandPolicy.validId(id));ok(!WorkspaceCommandPolicy.validId("------------------------------------"));ok(!WorkspaceCommandPolicy.validId(null));
  JSONObject a=new JSONObject().put("requestId",id).put("action","reference").put("mode","none").put("queued",false).put("expiresAt",100);
  JSONObject b=new JSONObject().put("mode","none").put("action","reference").put("requestId",id).put("expiresAt",200).put("queued",true);
  String before=a.toString();ok(WorkspaceCommandPolicy.sameCommand(a,b));ok(a.toString().equals(before));b.put("mode","image");ok(!WorkspaceCommandPolicy.sameCommand(a,b));b.put("mode","none").put("unexpected",true);ok(!WorkspaceCommandPolicy.sameCommand(a,b));
  ok(WorkspaceCommandPolicy.pendingFresh(a,99));ok(!WorkspaceCommandPolicy.pendingFresh(a,100));ok(!WorkspaceCommandPolicy.pendingFresh(a,101));ok(!WorkspaceCommandPolicy.pendingFresh(new JSONObject(),100));ok(!WorkspaceCommandPolicy.pendingFresh(new JSONObject().put("expiresAt",700001),100));
  ok(WorkspaceCommandPolicy.same(new JSONObject("{\"x\":{\"a\":1,\"b\":2},\"y\":[1,2]}"),new JSONObject("{\"y\":[1,2],\"x\":{\"b\":2,\"a\":1}}")));
  ok(!WorkspaceCommandPolicy.same(new JSONArray("[1,2]"),new JSONArray("[2,1]")));ok(!WorkspaceCommandPolicy.same(1,"1"));ok(WorkspaceCommandPolicy.same(1,1L));
  System.out.println("PASS "+n+" workspace command identity/expiry checks");
 }
}
