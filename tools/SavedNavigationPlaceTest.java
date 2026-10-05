package com.example.rokidkeyboardbridge;
import org.json.*;
import java.util.*;
public final class SavedNavigationPlaceTest {
    public static void main(String[] args)throws Exception {
        JSONArray items=new JSONArray().put(new JSONObject().put("name","駅").put("latitude",35).put("longitude",139))
            .put(JSONObject.NULL).put(new JSONObject().put("name","勤務先").put("kind","work").put("query","35.1,139.2"))
            .put(new JSONObject().put("name","不正").put("query","999,139"));
        List<SavedNavigationPlace> places=SavedNavigationPlace.parse(new JSONObject().put("places",items));
        if(places.size()!=2||!places.get(0).query.equals("35.0,139.0")||!places.get(1).kind.equals("work"))throw new AssertionError("validated non-positional list");
        if(SavedNavigationPlace.parse(new JSONObject().put("SavedLocation",items)).size()!=2)throw new AssertionError("SavedLocation alias");
        if(!SavedNavigationPlace.parse(new JSONObject()).isEmpty())throw new AssertionError("missing list");
        System.out.println("SavedNavigationPlace PASS: coordinates, invalid/null entries, kinds, wire formats");
    }
}
