package com.example.rokidkeyboardbridge;
import org.json.*;
import java.util.*;
/** Validated snapshot, without assuming home/work occupy particular array indexes. */
final class SavedNavigationPlace {
    final String name, query, kind;
    SavedNavigationPlace(String name,String query,String kind){this.name=name;this.query=query;this.kind=kind;}
    static List<SavedNavigationPlace> parse(JSONObject response) {
        JSONArray items=response.optJSONArray("places");
        if(items==null)items=response.optJSONArray("SavedLocation");
        List<SavedNavigationPlace> out=new ArrayList<>();if(items==null)return out;
        for(int i=0;i<items.length();i++) {
            JSONObject item=items.optJSONObject(i);if(item==null)continue;
            String name=item.optString("name").trim(),query=item.optString("query").trim();
            if(item.has("latitude") && item.has("longitude"))query=item.optDouble("latitude",Double.NaN)+","+item.optDouble("longitude",Double.NaN);
            if(name.isEmpty()||name.length()>200||query.isEmpty()||query.length()>200)continue;
            if(query.contains(",") && query.matches("[-+0-9.NaInfity\\s]+,[-+0-9.NaInfity\\s]+")) {
                try{String[] xy=query.split(",",-1);double a=Double.parseDouble(xy[0]),b=Double.parseDouble(xy[1]);
                    if(!Double.isFinite(a)||!Double.isFinite(b)||Math.abs(a)>90||Math.abs(b)>180)continue;
                }catch(Exception invalid){continue;}
            }
            out.add(new SavedNavigationPlace(name,query,item.optString("kind","favorite")));
        }
        return Collections.unmodifiableList(out);
    }
}
