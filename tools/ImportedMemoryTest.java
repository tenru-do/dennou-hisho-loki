package com.example.rokidgeminisecretary;
import org.json.JSONArray;
import org.json.JSONObject;
public class ImportedMemoryTest {
    static void check(boolean yes, String name) { if (!yes) throw new AssertionError(name); }
    public static void main(String[] args) throws Exception {
        check(ImportedMemory.identity(" hello ", "answer").equals(ImportedMemory.identity("hello", "answer")), "normalized duplicate");
        check(!ImportedMemory.identity("a", "bc").equals(ImportedMemory.identity("ab", "c")), "role boundary");
        String preview = ImportedMemory.excerpt("連携テスト", "UIを試しますか？");
        check(preview.contains("未検証"), "assistant not a fact");
        JSONArray all = new JSONArray();
        all.put(new JSONObject().put("summary", "Rokid Glassesのロキとの連携テストをした。方法はまだ未選択。"));
        all.put(new JSONObject().put("summary", "コーヒーを飲む習慣がある。").put("important", true));
        JSONArray found = ImportedMemory.select(all, "Rokidの連携テストについて").getJSONArray("entries");
        check(found.length() == 1 && found.getJSONObject(0).getString("summary").contains("Rokid"), "relevant only");
        check(ImportedMemory.select(all, "").getJSONArray("entries").length() == 0, "empty query");
        StringBuilder longText = new StringBuilder("連携テスト");
        while (longText.length() < 600) longText.append('あ');
        JSONArray longEntries = new JSONArray();
        for(int i=0;i<5;i++) longEntries.put(new JSONObject().put("summary", longText.toString()));
        check(ImportedMemory.select(longEntries, "連携テスト").getJSONArray("entries").length() == 1, "1000 char budget");
        JSONArray tied = new JSONArray().put(new JSONObject().put("summary", "連携テスト 通常"))
                .put(new JSONObject().put("summary", "連携テスト 重要").put("important", true));
        check(ImportedMemory.select(tied, "連携テスト").getJSONArray("entries").getJSONObject(0)
                .getString("summary").contains("重要"), "important tie break");
        check(ImportedMemory.select(new JSONArray().put(new JSONObject().put("summary", "本人の発言: テスト")),
                "テスト").getJSONArray("entries").length() == 1, "short Japanese query");
        System.out.println("PASS: 8 imported memory checks");
    }
}
