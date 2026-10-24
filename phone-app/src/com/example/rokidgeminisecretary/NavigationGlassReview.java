package com.example.rokidgeminisecretary;
import org.json.*;import java.util.*;
/** Phone-owned ephemeral place/route review. No destination changes before claim. */
public final class NavigationGlassReview {
 String session="",kind="",original="",mode="",selected="",originalPoint="";long routeAt,at;boolean claimed;private volatile boolean invalid;
 public void invalidate(){invalid=true;}
 double latitude,longitude;private final Map<String,JSONObject> places=new LinkedHashMap<>();
 public synchronized JSONObject begin(String kind,String original,String mode,String originalPoint,long routeAt,JSONArray rows,long now)throws Exception{
  if(!kind.matches("destination|stopover|alternative")||original.isEmpty()||!mode.matches("walking|driving|bicycling|transit"))throw new Exception("invalid review");
  session=UUID.randomUUID().toString();this.kind=kind;this.original=original;this.mode=mode;this.originalPoint=originalPoint;this.routeAt=routeAt;at=now;selected="";claimed=false;invalid=false;places.clear();JSONArray choices=new JSONArray();
  for(int i=0;i<Math.min(5,rows.length());i++){JSONObject row=new JSONObject(rows.getJSONObject(i).toString());double lat=row.getDouble("latitude"),lon=row.getDouble("longitude");if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)continue;String id=UUID.randomUUID().toString();places.put(id,row);choices.put(new JSONObject().put("id",id).put("label",row.getString("name")+"\n"+lat+", "+lon));}
  if(choices.length()==0)throw new Exception("no places");return new JSONObject().put("ok",true).put("session",session).put("choices",choices);
 }
 public synchronized void check(String id,String original,String mode,long routeAt,long now)throws Exception{if(invalid||!id.equals(session)||claimed||!this.original.equals(original)||!this.mode.equals(mode)||this.routeAt!=routeAt||now<at||now-at>180000)throw new Exception("review expired");}
 public synchronized JSONObject place(String id)throws Exception{JSONObject row=places.get(id);if(row==null)throw new Exception("place expired");return new JSONObject(row.toString());}
 public synchronized void claim()throws Exception{if(invalid||claimed||selected.isEmpty())throw new Exception("review already claimed");claimed=true;}
}
