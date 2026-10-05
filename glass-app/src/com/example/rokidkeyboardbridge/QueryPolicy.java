package com.example.rokidkeyboardbridge;

final class QueryPolicy {
    static boolean clock(String value) {
        String text = value == null ? "" : value.replaceAll("[\\s　？?。！!]", "");
        return text.matches("(?:今|いま|現在)?(?:何時|なんじ|の?時刻|の?時間)(?:ですか|かな|なの|は|を教えて|教えて|でしょうか)?");
    }
    static boolean search(String value) {
        if (value == null) return false;
        return value.matches("(?s).*(営業時間|開店|閉店|定休日|営業して|調べて|検索して|最新|速報|試合結果|ブレイキングダウン|BreakingDown|勝った|優勝|今日の.*結果|本日の.*結果).*")
                || (value.matches("(?s).*(店|ショップ|レストラン|カフェ|水族館|博物館).*")
                && value.matches("(?s).*(何時まで|何時から).*") );
    }
}
