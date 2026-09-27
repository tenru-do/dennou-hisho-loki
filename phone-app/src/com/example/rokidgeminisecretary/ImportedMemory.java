package com.example.rokidgeminisecretary;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Locale;

/** User-reviewed external history, deliberately separate from executable instructions. */
final class ImportedMemory {
    private static final String PREFS = "imported_reference_memory";
    static JSONArray read(Context c) throws Exception {
        return new JSONArray(c.getSharedPreferences(PREFS, 0).getString("entries", "[]"));
    }
    static String hash(String text) throws Exception {
        byte[] bytes = MessageDigest.getInstance("SHA-256").digest(
                text.replace("\r\n", "\n").trim().getBytes(StandardCharsets.UTF_8));
        StringBuilder s = new StringBuilder();
        for (byte b : bytes) s.append(String.format(Locale.ROOT, "%02x", b & 255));
        return s.toString();
    }
    static String identity(String user, String assistant) throws Exception {
        return hash(new JSONArray().put(user.trim()).put(assistant.trim()).toString());
    }
    static boolean contains(Context c, String id) throws Exception {
        JSONArray all = read(c);
        for (int i = 0; i < all.length(); i++) if (id.equals(all.getJSONObject(i).optString("id"))) return true;
        return false;
    }
    static synchronized boolean save(Context c, String id, String summary, boolean important) throws Exception {
        if (summary.trim().isEmpty() || summary.length() > 600) throw new Exception("記憶は1～600文字にしてください");
        if (contains(c, id)) return false;
        JSONArray all = read(c);
        if (all.length() >= 200) throw new Exception("保存上限200件です。整理してから追加してください");
        all.put(new JSONObject().put("id", id).put("summary", summary.trim())
                .put("important", important).put("importedAt", System.currentTimeMillis()).put("source", "Gemini・ユーザー確認済み"));
        if (!c.getSharedPreferences(PREFS, 0).edit().putString("entries", all.toString()).commit())
            throw new Exception("保存できませんでした");
        return true;
    }
    static synchronized void remove(Context c, String id) throws Exception {
        JSONArray kept = new JSONArray(), all = read(c);
        for (int i = 0; i < all.length(); i++) {
            if (!id.equals(all.getJSONObject(i).optString("id"))) kept.put(all.getJSONObject(i));
        }
        if (!c.getSharedPreferences(PREFS, 0).edit().putString("entries", kept.toString()).commit())
            throw new Exception("削除できませんでした");
    }
    static synchronized void update(Context c, String id, String expected, String summary, boolean important) throws Exception {
        if (summary.trim().isEmpty() || summary.length() > 600) throw new Exception("記憶は1～600文字にしてください");
        JSONArray all = read(c);
        for (int i = 0; i < all.length(); i++) {
            JSONObject entry = all.getJSONObject(i);
            if (!id.equals(entry.optString("id"))) continue;
            if (!expected.equals(entry.optString("summary"))) throw new Exception("記憶が変更されています。開き直してください");
            entry.put("summary", summary.trim()).put("important", important);
            if (!c.getSharedPreferences(PREFS, 0).edit().putString("entries", all.toString()).commit())
                throw new Exception("保存できませんでした");
            return;
        }
        throw new Exception("この記憶は削除されています");
    }
    static String excerpt(String user, String assistant) {
        // Extractive preview only: never claims an assistant suggestion is a user preference.
        return "本人の発言: " + cut(user.trim(), 280)
                + (assistant.trim().isEmpty() ? "" : "\nGeminiの回答（未検証）: " + cut(assistant.trim(), 220));
    }
    private static String cut(String s, int n) { return s.length() <= n ? s : s.substring(0, n) + "…"; }
    private static HashSet<String> grams(String value) {
        String s = value.toLowerCase(Locale.ROOT).replaceAll("[\\s\\p{Punct}。、！？]", "");
        HashSet<String> out = new HashSet<String>();
        for (int i = 0; i + 1 < s.length(); i++) out.add(s.substring(i, i + 2));
        return out;
    }
    static JSONObject search(Context c, String query) throws Exception {
        return select(read(c), query);
    }
    static JSONObject select(JSONArray all, String query) throws Exception {
        JSONArray result = new JSONArray();
        if (query == null || query.trim().length() < 3) return new JSONObject().put("entries", result);
        HashSet<String> q = grams(query);
        boolean[] used = new boolean[all.length()];
        int chars = 0;
        for (int pick = 0; pick < 2; pick++) {
            int best = -1, score = 0;
            for (int i = all.length() - 1; i >= 0; i--) {
                if (used[i]) continue;
                HashSet<String> g = grams(all.getJSONObject(i).optString("summary"));
                g.retainAll(q);
                if (g.size() < Math.min(3, q.size())) continue;
                int rank = g.size() * 2 + (all.getJSONObject(i).optBoolean("important") ? 1 : 0);
                if (rank > score) { best = i; score = rank; }
            }
            if (best < 0) break;
            used[best] = true;
            JSONObject entry = all.getJSONObject(best);
            String summary = entry.getString("summary");
            if (chars + summary.length() > 1000) continue;
            chars += summary.length();
            result.put(new JSONObject().put("summary", summary).put("source", entry.optString("source"))
                    .put("importedAt", entry.optLong("importedAt")));
        }
        return new JSONObject().put("entries", result);
    }
}
