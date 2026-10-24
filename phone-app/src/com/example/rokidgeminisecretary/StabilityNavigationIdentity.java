package com.example.rokidgeminisecretary;
import org.json.*;import java.util.UUID;
/** Private context is held only in memory; bridge identity is a random token. */
final class StabilityNavigationIdentity {
 private String context="",id="";private boolean active;
 synchronized JSONObject observe(String context,boolean active)throws Exception{if(id.isEmpty()||!this.context.equals(context)||this.active!=active){this.context=context;this.active=active;id=UUID.randomUUID().toString();}return snapshot();}
 synchronized JSONObject snapshot()throws Exception{return new JSONObject().put("id",id).put("active",active);}
}
