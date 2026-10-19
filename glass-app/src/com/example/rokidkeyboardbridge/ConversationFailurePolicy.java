package com.example.rokidkeyboardbridge;

import org.json.JSONObject;

/** Fixed diagnostic vocabulary: no exception body, user text, URL or credentials. */
final class ConversationFailurePolicy {
    static String code(Throwable error, int http, String finish, boolean safety) {
        String transport = "";
        Throwable cause = error;
        for (int depth = 0; cause != null && depth < 8; depth++, cause = cause.getCause()) {
            if (cause instanceof ConversationAnswerGuard.Incomplete) return ((ConversationAnswerGuard.Incomplete) cause).reason;
            if (cause instanceof LocalConversationStop) return "REFERENCE_CHANGED";
            // HTTP/provider metadata is supplied separately by the activity.
            if (cause instanceof java.net.SocketTimeoutException) transport = "TIMEOUT";
            if (cause instanceof java.net.UnknownHostException || cause instanceof java.net.ConnectException)
                transport = "CONNECTION";
            if (cause instanceof InterruptedException || cause instanceof java.util.concurrent.CancellationException)
                transport = "INTERRUPTED";
        }
        if (safety) return "SAFETY_STOP";
        if (!transport.isEmpty()) return transport;
        if (http == 429) return "RATE_LIMIT";
        if (http == 401 || http == 403) return "AUTHORIZATION";
        if (http == 402) return "BILLING";
        if (http == 404) return "MODEL_UNAVAILABLE";
        if (http >= 500 && http <= 599) return "SERVICE_UNAVAILABLE";
        if (http > 0) return "API_REJECTED";
        if ("MAX_TOKENS".equals(finish)) return "OUTPUT_LIMIT";
        if (finish != null && !finish.isEmpty()) return "NO_ANSWER";
        return "PROCESSING";
    }

    static String title(String code) {
        if ("TIMEOUT".equals(code) || "CONNECTION".equals(code)) return "接続を完了できませんでした";
        if ("REFERENCE_CHANGED".equals(code)) return "画像参照の更新・失効";
        if ("INTERRUPTED".equals(code)) return "会話処理が中断しました";
        return "回答を完了できませんでした";
    }

    static String detail(String code) {
        switch (code) {
            case "REFERENCE_CHANGED": return LocalConversationStop.message(code);
            case "SAFETY_STOP": return "安全判定で停止しました。このやり取りは会話履歴に保存しません。";
            case "TIMEOUT": return "通信の応答待ちで時間切れになりました。送信状況を確認してから再送してください。";
            case "CONNECTION": return "接続できませんでした。Wi-Fiとスマホの接続を確認してください。";
            case "INTERRUPTED": return "処理が中断しました。送信状況を確認してから操作してください。";
            case "RATE_LIMIT": return "Gemini APIの利用制限です。表示された待機時間を待ち、APIの利用枠を確認してください。";
            case "AUTHORIZATION": return "Gemini APIの認証・権限エラーです。APIキーとプロジェクトの権限を確認してください。";
            case "BILLING": return "Gemini APIの請求設定を確認してください。";
            case "MODEL_UNAVAILABLE": return "設定されたGeminiモデルを利用できません。モデル設定を確認してください。";
            case "SERVICE_UNAVAILABLE": return "Gemini APIが一時的に応答できません。少し待って送信状況を確認してください。";
            case "API_REJECTED": return "Gemini APIが依頼を受け付けませんでした。設定と送信状況を確認してください。";
            case "OUTPUT_LIMIT": return "回答の出力上限に達し、完了できませんでした。";
            case "INCOMPLETE_RESPONSE": return "回答の終了を確認できなかったため停止しました。途中の回答は記憶・読み上げに使いません。自動再送はしていません。";
            case "NO_ANSWER": return "Geminiから有効な回答を取得できませんでした。";
            default: return "会話の処理を完了できませんでした。依頼・転送・動作チェックと送信状況を確認してください。";
        }
    }

    static String stage(String value) {
        if (ConversationProgressPolicy.PREPARE.equals(value)) return "PREPARE";
        if (ConversationProgressPolicy.ANSWER.equals(value)) return "ANSWER";
        if (ConversationProgressPolicy.REVIEW.equals(value)) return "REVIEW";
        if (ConversationProgressPolicy.REPAIR.equals(value)) return "REPAIR";
        return "PROCESS";
    }

    static String stageLabel(String code) {
        switch (code) {
            case "PREPARE": return ConversationProgressPolicy.PREPARE;
            case "ANSWER": return ConversationProgressPolicy.ANSWER;
            case "REVIEW": return ConversationProgressPolicy.REVIEW;
            case "REPAIR": return ConversationProgressPolicy.REPAIR;
            default: return "会話の処理";
        }
    }

    static boolean validCode(String code) {
        return code != null && code.matches("REFERENCE_CHANGED|SAFETY_STOP|TIMEOUT|CONNECTION|INTERRUPTED|RATE_LIMIT|AUTHORIZATION|BILLING|MODEL_UNAVAILABLE|SERVICE_UNAVAILABLE|API_REJECTED|OUTPUT_LIMIT|INCOMPLETE_RESPONSE|NO_ANSWER|PROCESSING");
    }

    static JSONObject record(String code, String stage, long at) throws org.json.JSONException {
        if (!validCode(code) || at <= 0) throw new IllegalArgumentException("Invalid failure record");
        return new JSONObject().put("code", code).put("stage", stage(stage)).put("at", at);
    }

    static String summary(String raw) {
        try {
            JSONObject record = new JSONObject(raw);
            String code = record.optString("code"); long at = record.optLong("at");
            if (!validCode(code) || at <= 0) return "";
            return "直近の会話停止：" + stageLabel(record.optString("stage")) + "\n"
                    + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.JAPAN).format(new java.util.Date(at))
                    + "\n" + detail(code);
        } catch (Exception invalid) { return ""; }
    }

    private ConversationFailurePolicy() {}
}
