package com.example.rokidkeyboardbridge;

/** Elliptical questions keep their antecedent even after a short pause. */
final class ConversationFollowupPolicy {
    static final long REFERENCE_TTL_MS = 2L * 60L * 60L * 1000L;
    static final String INSTRUCTION = "短い追加質問（『年間では？』『手取りだと？』『それなら？』など）は、reference_contextにある直近の関連する質問と回答の条件を引き継いで答える。時給、勤務時間、日数、月額など提示済みの数値を使い、計算の前提と結果を簡潔に示す。月20日勤務の月給を年額にする場合は月額×12であり、勝手に週52週へ条件を変えない。計算だけなら外部検索は不要。以前の質問を忘れたと指摘されたら、過去の不適切な確認質問を繰り返さず、元の実質的な質問へ戻って答える。『忘れていません』と主張してから説明を求めない。履歴が実際に取得できない場合だけ、その旨を短く伝え、必要な条件を一つ確認する。話題変更や新しい条件が明示されたら現在の発言を優先する。";

    static boolean referencesPrevious(String text) {
        if (text == null) return false;
        if (ConversationTaskPolicy.continuesTask(text)) return true;
        String value = text.replaceAll("[\\s？?！!。]", "");
        if (value.isEmpty()) return false;
        if (value.matches(".*(?:前|直前|さっき|先ほど)の?(?:質問|回答|話|条件|計算).*")) return true;
        if (value.matches(".*(?:忘れた|忘れちゃった|覚えてない|忘れてる).*")) return true;
        // Do not interpret a complete new research request as an antecedent query.
        if (value.length() > 45 || value.matches(".*(?:検索|調べて|別の話|話題を変|会話をリセット).*")) return false;
        return value.matches(".*(?:では|だと|なら|の場合|にすると|にしたら|に換算|いくらになる|何倍|何割|何パーセント)$")
                || value.matches("(?:年間|年額|年収|月額|月収|手取り|税込|税抜|一日|1日|一週間|週|一か月|一年)(?:は|で|だと|では|なら)?")
                || value.matches("(?:じゃあ|では|それなら|そうすると).{0,25}");
    }

    static boolean fresh(long at, long now) {
        return at > 0 && at <= now && now - at <= REFERENCE_TTL_MS;
    }

    static boolean needsContextRepair(String prompt, String answer, boolean hasReference) {
        if (!hasReference || !referencesPrevious(prompt) || answer == null) return false;
        return answer.matches("(?s).*(何についての|何に関する|何の年間|具体的な内容をご|具体的な内容を教).*" )
                || (answer.matches("(?s).*(忘れてはおりません|忘れていません|忘れておりません).*" )
                && answer.matches("(?s).*(確認させて|教えて|お教え|でしょうか|ご教示|よろしければ).*"));
    }
}
