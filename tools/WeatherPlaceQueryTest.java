package com.example.rokidgeminisecretary;

public class WeatherPlaceQueryTest {
    public static void main(String[] args) {
        check("ディズニーシーの天気は？", "東京ディズニーシー");
        check("明日のディズニーシーの天気は？", "東京ディズニーシー");
        check("ディズニーシーの明日の天気は？", "東京ディズニーシー");
        check("浦安市では明日雨が降る？", "浦安市");
        check("横浜で傘いる？", "横浜");
        check("立川の天気は", "立川");
        check("今日の天気は？", "");
        check("明日は傘いる？", "");
        check("現在地の天気は？", "");
        check("ここは雨が降る？", "");
        check("天気は？", "");
        System.out.println("11 weather place tests passed");
    }
    private static void check(String query, String expected) {
        String actual = WeatherPlaceQuery.extract(query);
        if (!expected.equals(actual)) throw new AssertionError(query + ": " + actual);
    }
}
