package com.example.rokidgeminisecretary;

import com.example.rokidkeyboardbridge.CacheFreshnessPolicy;
import org.json.JSONArray;
import org.json.JSONObject;

public final class AmbCacheRegressionTest {
    private static int checks;
    private static void check(boolean ok, String message) {
        checks++; if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        long now = 10000000L, ttl = CacheFreshnessPolicy.SHARED_MEMORY_TTL_MS;
        check(CacheFreshnessPolicy.fresh(now - ttl + 1, now, ttl), "fresh boundary");
        check(!CacheFreshnessPolicy.fresh(now - ttl, now, ttl), "expiry boundary");
        check(!CacheFreshnessPolicy.fresh(0, now, ttl), "undated");
        check(!CacheFreshnessPolicy.fresh(now + 1, now, ttl), "clock rollback");
        JSONArray history = new JSONArray();
        history.put(new JSONObject().put("at", now - TopicHistoryPolicy.TTL_MS).put("topicKey", "expired"));
        history.put(new JSONObject().put("topicKey", "undated"));
        history.put(new JSONObject().put("at", now + 1).put("topicKey", "future"));
        JSONObject fresh = new JSONObject().put("at", now - 1).put("message", "unchanged");
        history.put(fresh);
        JSONArray retained = TopicHistoryPolicy.retain(history, now);
        check(retained.length() == 1, "stale undated future removed");
        check(retained.getJSONObject(0) == fresh, "fresh contents preserved");
        check(history.length() == 4, "input remains unchanged");
        JSONArray many = new JSONArray();
        for (int i = 0; i < 40; i++) many.put(new JSONObject().put("at", now - 1).put("order", i));
        JSONArray bounded = TopicHistoryPolicy.retain(many, now);
        check(bounded.length() == 12, "bounded history");
        check(bounded.getJSONObject(0).getInt("order") == 28, "newest twelve");
        check(TopicHistoryPolicy.retain(many, now + TopicHistoryPolicy.TTL_MS).length() == 0, "all expire without new topics");
        check(TopicHistoryPolicy.retain(null, now).length() == 0, "empty input");
        System.out.println("AmbCacheRegressionTest PASS: " + checks + " checks");
    }
}
