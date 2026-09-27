package com.example.rokidgeminisecretary;

/** Extract explicit places without changing the current-location weather cache. */
final class WeatherPlaceQuery {
    static String extract(String query) {
        if (query == null) return "";
        String text = query.trim().replaceAll("[？?！!。]", "");
        text = text.replaceAll("^(?:今日|明日|あした|明後日|あさって|今夜|今朝)(?:の|は|、|\\s)*", "");
        java.util.regex.Matcher match = java.util.regex.Pattern.compile(
                "^(.+?)(?:の|では|で|は)(?:(?:今日|明日|あした|明後日|あさって)の?)?"
                + "(?:天気|気温|降水|雨|傘|かさ|カサ|雨具)").matcher(text);
        if (!match.find()) return "";
        String place = match.group(1).trim();
        if (place.matches("ここ|今いる場所|現在地|この辺|このあたり|今日|明日|明後日")) return "";
        if (place.equals("ディズニーシー")) return "東京ディズニーシー";
        if (place.equals("ディズニーランド")) return "東京ディズニーランド";
        return place;
    }
}
