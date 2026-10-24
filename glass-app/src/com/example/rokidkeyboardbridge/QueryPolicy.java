package com.example.rokidkeyboardbridge;

final class QueryPolicy {
    static boolean clock(String value) {
        String text = value == null ? "" : value.replaceAll("[\\s　？?。！!]", "");
        return text.matches("(?:今|いま|現在)?(?:何時|なんじ|の?時刻|の?時間)(?:ですか|かな|なの|は|を教えて|教えて|でしょうか)?");
    }
    static boolean search(String value) {
        if (value == null || ConversationIntentPolicy.personalState(value)) return false;
        return place(value) || ConversationPlaceContinuity.spatial(value) || value.matches("(?s).*(営業時間|開店|閉店|定休日|営業して|調べて|調べろ|調査して|検索して|検索しろ|イベント|催し|祭り|開催|使い分け|最新|速報|試合結果|ブレイキングダウン|BreakingDown|勝った|優勝|今日の.*結果|本日の.*結果).*")
                || (value.matches("(?s).*(店|ショップ|レストラン|カフェ|水族館|博物館).*")
                && value.matches("(?s).*(何時まで|何時から).*") );
    }
    static boolean place(String value){
        if(value==null||ConversationIntentPolicy.personalState(value))return false;
        String text=value.replaceAll("[\\s　？?。！!]", "");
        if(text.matches("(?s).*(原因|問題|間違い|誤り|違い|責任|気持ち|痛み|エラー|バグ).*")||text.matches("(?s).*(写真|画像|ファイル|保存|メモ|回答|設定|入力欄|ボタン).*"))return false;
        return text.matches("(?s).+(?:は|って)(?:どこ|何処)(?:にある|にあります|です|でしょう|なの|にあるの|にありますか|ですか|にあるんですか)?(?:か)?")
            ||text.matches("(?s).+(?:の場所|の所在地|の住所|へのアクセス)(?:は|を教えて|を教えてください|を知りたい)?");
    }
    static boolean refinement(String value){
        if(value==null||ConversationIntentPolicy.personalState(value))return false;
        if(ConversationTaskPolicy.continuesTask(value))return true;
        return value.matches("(?s).*(しぼって|絞って|絞り込|に限定|方面|地元|近所|近く|ほかにも|他にも|ほかは|他は|無料の|子供向け|子ども向け|雨でも|土曜日だけ|日曜日だけ|公式サイト|詳しく|会場|アクセス).*");
    }
    static boolean search(String value,String previous,long age){
        return search(value)||(age>=0&&age<=15*60*1000L&&search(previous)&&refinement(value));
    }

}
