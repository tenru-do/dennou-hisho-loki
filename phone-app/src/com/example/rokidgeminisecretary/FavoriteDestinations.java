package com.example.rokidgeminisecretary;
import android.content.Context;
import org.json.*;
/** Device-local named addresses; reads always reflect committed edits. */
final class FavoriteDestinations {
    static JSONArray read(Context c) {
        try { return new JSONArray(c.getSharedPreferences("loki_destination_presets",0).getString("favorites","[]")); }
        catch (JSONException e) { return new JSONArray(); }
    }
    static boolean save(Context c, JSONArray items) {
        if(items.length()>100) return false;
        for(int i=0;i<items.length();i++) {
            JSONObject p=items.optJSONObject(i);
            if(p==null || !valid(p.optString("name"),p.optString("query"))) return false;
        }
        return c.getSharedPreferences("loki_destination_presets",0).edit().putString("favorites",items.toString()).commit();
    }
    static boolean valid(String name,String query) {
        if(name.trim().isEmpty() || name.length()>200 || query.trim().isEmpty() || query.length()>200) return false;
        if(query.matches("[-+0-9.\\s]+,[-+0-9.\\s]+")) {
            try { String[] xy=query.split(","); double lat=Double.parseDouble(xy[0].trim()),lng=Double.parseDouble(xy[1].trim());
                return Double.isFinite(lat)&&Double.isFinite(lng)&&Math.abs(lat)<=90&&Math.abs(lng)<=180;
            } catch(Exception e) { return false; }
        }
        return true;
    }
    static JSONArray all(Context c) throws JSONException {
        JSONArray all=new JSONArray();
        for(String kind:new String[]{"home","work"}) {
            DestinationPresets.Place p=DestinationPresets.read(c,kind);
            all.put(new JSONObject().put("kind",kind).put("name",p==null?"":p.label)
                .put("query",p==null?"":p.latitude+","+p.longitude));
        }
        JSONArray favorites=read(c);
        for(int i=0;i<favorites.length();i++) all.put(favorites.getJSONObject(i));
        return all;
    }
}