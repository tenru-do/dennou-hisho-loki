package com.example.rokidgeminisecretary;
import org.json.*;
import java.util.*;

/** Short-lived, bounded phone-owned snapshots shared by both navigation controls. */
final class NavigationRouteChoices {
    private String destination="",mode="",coordinates="";
    private long fetchedAt;
    private final Map<String,JSONObject> routes=new LinkedHashMap<>();
    synchronized void clear(){routes.clear();fetchedAt=0;destination=mode=coordinates="";}
    synchronized JSONObject publish(String name,String travel,String point,JSONArray candidates,long now)throws Exception {
        destination=name;mode=travel;coordinates=point;fetchedAt=now;routes.clear();
        JSONArray labels=new JSONArray();
        for(int i=0;i<Math.min(3,candidates.length());i++){
            JSONObject route=new JSONObject(candidates.getJSONObject(i).toString());
            String id=UUID.randomUUID().toString();routes.put(id,route);
            labels.put(new JSONObject().put("id",id).put("label",label(route,i)).put("review",NavigationTripReview.text(route)+"\n"+route.optString("lokiBufferWarning")));
        }
        if(routes.isEmpty())throw new Exception("navigation_routes_empty");
        return new JSONObject().put("ok",true).put("destination",name).put("mode",travel).put("choices",labels)
            .put("notice","ロキ共通経路。Googleマップの候補・選択経路とは別です。");
    }
    synchronized JSONObject cached(String name,String travel,String point,long now)throws Exception {
        if(!matches(name,travel,point,now))return null;
        JSONArray labels=new JSONArray();int i=0;
        for(Map.Entry<String,JSONObject> e:routes.entrySet())labels.put(new JSONObject().put("id",e.getKey()).put("label",label(e.getValue(),i++)).put("review",NavigationTripReview.text(e.getValue())+"\n"+e.getValue().optString("lokiBufferWarning")));
        return new JSONObject().put("ok",true).put("destination",name).put("mode",travel).put("choices",labels)
            .put("notice","ロキ共通経路。Googleマップの候補・選択経路とは別です。");
    }
    synchronized JSONObject select(String id,String name,String travel,String point,long now)throws Exception {
        if(!matches(name,travel,point,now)||!routes.containsKey(id))throw new Exception("navigation_route_selection_expired");
        int index=new ArrayList<>(routes.keySet()).indexOf(id);
        return new JSONObject(routes.get(id).toString()).put("lokiSelectionId",id).put("lokiSelectionLabel",label(routes.get(id),index));
    }
    private boolean matches(String name,String travel,String point,long now){return name.equals(destination)&&travel.equals(mode)&&point.equals(coordinates)&&now>=fetchedAt&&now-fetchedAt<=180000&&!routes.isEmpty();}
    static String label(JSONObject route,int index)throws Exception {
        StringBuilder text=new StringBuilder((index+1)+". ");double seconds=TransitJourney.duration(route.optString("duration"));
        if(seconds>=0)text.append((long)Math.ceil(seconds/60)).append("分 / ");
        if("navitime".equals(route.optString("lokiProvider")))text.append("平均時間の参考 / ");
        JSONArray legs=route.optJSONArray("legs");int rides=0;
        if(legs!=null)for(int i=0;i<legs.length();i++){
            JSONArray steps=legs.getJSONObject(i).optJSONArray("steps");if(steps==null)continue;
            for(int j=0;j<steps.length();j++){
                JSONObject s=steps.getJSONObject(j),d=s.optJSONObject("transitDetails");if(d==null)continue;
                JSONObject stops=d.optJSONObject("stopDetails"),line=d.optJSONObject("transitLine");
                if(rides++>0)text.append(" → ");
                JSONObject vehicle=line==null?null:line.optJSONObject("vehicle");String type=vehicle==null?"":vehicle.optString("type");
                if(type.contains("BUS"))text.append("[バス] ");
                else if(type.contains("TRAIN")||type.contains("RAIL")||"SUBWAY".equals(type)||"TRAM".equals(type))text.append("[鉄道] ");
                text.append(line==null?"公共交通":line.optString("name","公共交通")).append(" ／ 行き先：").append(d.optString("headsign").isEmpty()?"未確認":d.optString("headsign"));
                if(stops!=null){JSONObject a=stops.optJSONObject("departureStop"),b=stops.optJSONObject("arrivalStop");
                    text.append(" ").append(a==null?"":a.optString("name")).append("→").append(b==null?"":b.optString("name"));}
            }
        }
        if(rides==0)text.append(route.optString("description","道路経路"));
        return text.toString()+(route.optString("lokiBufferWarning").isEmpty()?"":"\n"+route.optString("lokiBufferWarning"));
    }
}
