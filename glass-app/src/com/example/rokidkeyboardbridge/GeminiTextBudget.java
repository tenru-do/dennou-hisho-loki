package com.example.rokidkeyboardbridge;

import org.json.JSONException;
import org.json.JSONObject;

/** Reserve enough of Flash's shared output budget for the spoken answer. */
final class GeminiTextBudget {
    static void apply(JSONObject config, String model, boolean imageRequest) throws JSONException {
        if (imageRequest || !"gemini-2.5-flash".equals(model)) return;
        config.put("maxOutputTokens", Math.max(3072, config.optInt("maxOutputTokens", 0)));
        config.put("thinkingConfig", new JSONObject().put("thinkingBudget", 512));
    }

    private GeminiTextBudget() {}
}
