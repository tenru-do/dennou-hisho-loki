package com.example.rokidgeminisecretary;

import org.json.JSONArray;
import org.json.JSONObject;

/** Exclusion history is short lived; it is not a permanent source of new topics. */
final class TopicHistoryPolicy {
    static final long TTL_MS = 30L * 60L * 1000L;
    static final int MAX_ENTRIES = 12;
    static JSONArray retain(JSONArray history, long now) {
        JSONArray result = new JSONArray();
        if (history == null) return result;
        for (int i = Math.max(0, history.length() - MAX_ENTRIES); i < history.length(); i++) {
            JSONObject item = history.optJSONObject(i);
            long at = item == null ? 0L : item.optLong("at", 0L);
            if (at > 0L && at <= now && now - at < TTL_MS) result.put(item);
        }
        return result;
    }
}
