package com.example.rokidgeminisecretary;
import java.nio.charset.StandardCharsets;

/** Explicit limits before any TOPIC text crosses the phone/glass boundary. */
public final class TopicTextPolicy {
    public static final int NEWS_CHARS=300, SCRIPT_CHARS=1400, WIRE_BYTES=6144;
    public static String limit(String value,int max) {
        String text=NewsRssFallback.plain(value);
        if(max<=0)return "";
        if(text.length()<=max)return text;
        int end=Math.max(0,max-1);
        if(end>0 && Character.isHighSurrogate(text.charAt(end-1)))end--;
        return text.substring(0,end)+"…";
    }
    public static int bytes(String value) { return value.getBytes(StandardCharsets.UTF_8).length; }
    public static String region(String region) {
        String value=region==null?"":region.trim();
        value=value.replaceAll("[\\s　]+", " ");
        // Do not require both prefecture and municipality to appear in every headline.
        int split=value.lastIndexOf(' ');
        if(split>=0)value=value.substring(split+1);
        if(value.contains("立川"))return "立川";
        return value.isEmpty()||value.equals("取得済")?"多摩":value;
    }
}
