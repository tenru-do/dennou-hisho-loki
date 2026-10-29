package com.example.rokidkeyboardbridge;
import android.content.Context;import android.util.Base64;import org.json.*;import java.nio.charset.StandardCharsets;
/** One owner for voice/text/button scenes. Persist only encrypted enum state, not dialogue. */
final class VirtualContactSession {
 private final Context context;private final VirtualContactPolicy state=new VirtualContactPolicy();private VirtualContactPolicy.Frame cached=VirtualContactPolicy.Frame.EMPTY;
 VirtualContactSession(Context c){context=c;try{String raw=c.getSharedPreferences("virtual_contact",0).getString("state","");if(!raw.isEmpty()){JSONObject j=new JSONObject(new String(DeviceSecrets.photos().decrypt(Base64.decode(raw,2)),StandardCharsets.UTF_8));state.restore(j.getInt("kind"),j.getInt("part"),j.getInt("actor"),j.getInt("event"),j.getInt("direction"),j.getInt("pending"),j.getLong("at"),j.optLong("activity",j.getLong("at")),j.optBoolean("suspended"),j.optBoolean("offerOnly"),System.currentTimeMillis());}}catch(Exception invalid){state.discard();}}
 synchronized VirtualContactPolicy.Frame frame(){VirtualContactPolicy.Frame f=state.frame(System.currentTimeMillis());if(cached.revision!=f.revision)cached=f;return cached;}
 synchronized void user(String text){long before=state.revision();state.user(text,System.currentTimeMillis());if(before!=state.revision())save();}
 synchronized void answer(String prompt,String reply){long before=state.revision();state.answer(prompt,reply,System.currentTimeMillis());if(before!=state.revision())save();}
 synchronized boolean gesture(int direction){boolean changed=state.gesture(direction,System.currentTimeMillis());if(changed)save();return changed;}
 synchronized void clear(){if(state.frame(System.currentTimeMillis()).kind==VirtualContactPolicy.NONE)return;state.clear(System.currentTimeMillis());save();}
 private void save(){try{VirtualContactPolicy.Frame f=state.frame(System.currentTimeMillis());JSONObject j=new JSONObject().put("kind",f.kind).put("part",f.part).put("actor",f.actor).put("event",f.event).put("direction",f.direction).put("pending",f.pending).put("at",f.at).put("activity",f.activity).put("suspended",f.suspended).put("offerOnly",f.offerOnly);context.getSharedPreferences("virtual_contact",0).edit().putString("state",Base64.encodeToString(DeviceSecrets.photos().encrypt(j.toString().getBytes(StandardCharsets.UTF_8)),2)).apply();cached=f;}catch(Exception unavailable){android.util.Log.w("LokiVirtualContact","Scene persistence unavailable; no dialogue logged");}}
 synchronized String block(String current){
  String posture=AvatarPostureContext.block(current);if(!posture.isEmpty())return posture;
  long now=System.currentTimeMillis();String text=VirtualContactPolicy.clean(current);if(VirtualContactPolicy.excluded(text))return "";if(!state.context(now)&&!VirtualContactPolicy.contactWords(text))return "";VirtualContactPolicy.Frame f=frame();
  if(f.suspended&&!VirtualContactPolicy.contactWords(VirtualContactPolicy.clean(current)))return "";
  try{return "\n<virtual_contact_state>\n"+VirtualContactPolicy.INSTRUCTION+"\n"+new JSONObject().put("scope","virtual_scene_only").put("state",new String[]{"NONE","OFFER","HAND","HUG","PAT"}[f.kind]).put("part",new String[]{"","HAND","HEAD","SHOULDER","CHEEK"}[f.part]).put("actor",new String[]{"","LOKI","USER","BOTH"}[f.actor]).put("event",f.event).put("lastEventAgeSeconds",Math.max(0,(now-f.at)/1000)).put("direction",f.direction).put("offerOnly",f.offerOnly).put("suspended",f.suspended).toString()+"\n</virtual_contact_state>\n";}catch(Exception invalid){return "";}
 }
}
