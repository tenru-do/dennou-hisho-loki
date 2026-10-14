package com.example.rokidkeyboardbridge;

final class CalendarConversationPolicy {
    static boolean lookup(String raw) {
        if(raw==null)return false;
        String t=raw.replaceAll("\\s","");
        if(!t.matches(".*(?:予定|スケジュール|カレンダー|[Cc]alendar|予約日|予約時間).*"))return false;
        if(t.matches("(?:今日|明日|明後日|今週|来週|今月|来月|次|この後|今後)?の?(?:予定|スケジュール|カレンダー)[？?。]*(?:は[？?。]*)?"))return true;
        return t.matches(".*(?:教えて|見せて|確認して|確認したい|調べて|検索して|一覧|何日|何時|いつ|どうなって|ありますか|ある[？?]|予定は[？?]).*");
    }
    static boolean statement(String raw) {
        if(raw==null||lookup(raw))return false;
        String t=raw.replaceAll("\\s","");
        return t.matches(".*(?:カレンダー|[Cc]alendar|予定|スケジュール).*"
                +"(?:入れてある|いれてある|入れている|いれている|登録してある|登録している|記録してある|管理している|管理してる|入れた|登録した|というカレンダー|っていうカレンダー).*");
    }
}
