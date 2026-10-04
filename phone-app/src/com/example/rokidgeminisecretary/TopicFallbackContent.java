package com.example.rokidgeminisecretary;
import android.content.Context;
import android.content.SharedPreferences;
import java.time.LocalDateTime;
import java.util.*;
import org.json.JSONObject;
import org.json.JSONArray;

final class TopicFallbackContent {
    private static final String HISTORY="offline_long_column_history_v3";
    static synchronized JSONArray freshNews(Context context, JSONArray incoming) throws Exception {
        SharedPreferences prefs=context.getSharedPreferences("phone_secretary",Context.MODE_PRIVATE);
        JSONArray history=new JSONArray(prefs.getString("topic_rss_history_v1","[]"));
        Set<String> seen=new HashSet<>();
        for(int i=0;i<history.length();i++) seen.add(history.getString(i));
        JSONArray result=new JSONArray();
        for(int i=0;i<incoming.length();i++) {
            JSONObject item=incoming.getJSONObject(i);
            String key=item.optString("title");
            if(seen.add(key)) { result.put(item); history.put(key); }
        }
        JSONArray retained=new JSONArray();
        for(int i=Math.max(0,history.length()-200);i<history.length();i++) retained.put(history.getString(i));
        prefs.edit().putString("topic_rss_history_v1",retained.toString()).apply();
        return result;
    }
    static synchronized JSONObject local(Context context) throws Exception {
        SharedPreferences prefs=context.getSharedPreferences("phone_secretary",Context.MODE_PRIVATE);
        Set<String> used=new HashSet<>(prefs.getStringSet(HISTORY,Collections.emptySet()));
        String[] topic=LongColumnCatalog.select(used,new Random());
        prefs.edit().putStringSet(HISTORY,used).apply();
        return new JSONObject().put("title",topic[0]).put("text",topic[1]);
    }
}
