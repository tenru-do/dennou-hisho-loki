package com.example.rokidkeyboardbridge;

final class CalendarWriteIntent {
    static boolean supplement(String current) {
        if(current==null)return false;
        String t=current.replaceAll("\\s","");
        if(t.matches(".*(?:やめて|しないで|検索して|調べて|予定を教えて|予定を確認|予定は|一覧|見せて).*"))return false;
        return t.contains("カレンダー")||t.contains("日程")||t.contains("開催日")||t.contains("終日")
                ||t.matches(".*(?:さっき|先ほど|前の).*(?:答え|内容|日付).*" )
                ||t.matches("(?:登録して|それで|それでお願い|はい|お願いします|[0-9０-９]+月.*|[0-9０-９]+時.*)");
    }
    static boolean requested(String current,String context) {
        if(current==null)return false;
        String text=current.replaceAll("\\s","");
        if(text.matches(".*(?:しないで|しなくて|不要|やめて|ないで|ほしくない|とは言っていない).*"))return false;
        if(text.matches(".*(?:登録してある|登録している|登録していた|入れてある|入れている|いれてある|いれている).*"))return false;
        boolean write=text.matches(".*(?:登録して|登録してください|登録しといて|登録お願い|追加して|追加してください|入れて|入れといて).*");
        boolean calendar=text.contains("カレンダー")||text.contains("予定")||text.toLowerCase(java.util.Locale.ROOT).contains("calendar");
        boolean followup=text.matches("(?:それを|これを|さっきのを)?(?:登録して|登録しといて|登録お願い|追加して)");
        return write&&(calendar||(followup&&context!=null&&context.contains("カレンダー")));
    }
}
