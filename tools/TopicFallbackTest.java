package com.example.rokidgeminisecretary;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
public final class TopicFallbackTest {
    static void check(boolean ok, String message) { if(!ok) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        String feed="<rss><channel><item><title>Science &amp; IT</title><description><![CDATA[<b>Summary</b> details]]></description><pubDate>Today</pubDate><link>https://example.com/1</link></item><item><title>Science &amp; IT</title></item><item><title> </title></item></channel></rss>";
        List<NewsRssFallback.Article> items=NewsRssFallback.parse(feed,10);
        check(items.size()==1,"empty/duplicate titles");
        check(items.get(0).title.equals("Science & IT"),"XML entities");
        check(items.get(0).description.equals("Summary details"),"CDATA HTML summary");
        check(items.get(0).link.equals("https://example.com/1"),"link");
        check(NewsRssFallback.parse(feed,0).isEmpty(),"limit");
        AtomicInteger requests=new AtomicInteger();
        check(NewsRssFallback.fetch("primary",3,url->{ if(requests.incrementAndGet()==1) throw new IllegalStateException("HTTP 429"); return feed; }).size()==1,"primary failure RSS recovery");
        check(requests.get()==2,"fallback order");
        requests.set(0);
        check(NewsRssFallback.fetch("primary",3,url->{ int n=requests.incrementAndGet(); return n==1?"<rss/>":n==2?"malformed":feed; }).size()==1 && requests.get()==3,"empty and malformed feed recovery");
        check(NewsRssFallback.fetch("primary",3,url->{throw new IllegalStateException("offline");}).isEmpty(),"offline");
        boolean rejected=false;
        try { NewsRssFallback.parse("<!DOCTYPE rss [<!ENTITY x SYSTEM 'file:///secret'>]><rss/>",3); } catch(Exception e) {rejected=true;}
        check(rejected,"external entities");
        check(OfflineTopicCatalog.size()>=50,"dictionary size");
        Set<String> used=new HashSet<>(); Random random=new Random(42);
        LocalDateTime now=LocalDateTime.of(2026,10,3,8,0);
        for(int i=0;i<OfflineTopicCatalog.size();i++) {
            String[] topic=OfflineTopicCatalog.select(now,used,random);
            check(used.size()==i+1,"repeat before complete cycle");
            check(topic[1].contains("10月3日 土曜日、朝"),"device date weekday hour");
        }
        OfflineTopicCatalog.select(now,used,random);
        check(used.size()==1,"exhausted dictionary recovers");
        for(int month=1;month<=12;month++) for(int hour:new int[]{0,5,11,18,23})
            check(!OfflineTopicCatalog.select(LocalDateTime.of(2026,month,1,hour,0),new HashSet<>(),random)[1].isEmpty(),"season/time boundaries");
        AtomicInteger fallbacks=new AtomicInteger();
        try(TopicPlaybackLoader<String> loader=new TopicPlaybackLoader<>()) {
            check("ok".equals(loader.loadLazy(()->"ok",()->{fallbacks.incrementAndGet();return "local";},1000,(name,e)->{})),"ready playback");
            check(fallbacks.get()==0,"successful build must not consume offline topic");
            check("local".equals(loader.loadLazy(()->{throw new IllegalStateException();},()->{fallbacks.incrementAndGet();return "local";},1000,(name,e)->{})),"lazy local recovery");
        }
        System.out.println("TopicFallbackTest PASS: RSS failover/parser, "+OfflineTopicCatalog.size()+" unique topics, context boundaries, lazy fallback");
    }
}
