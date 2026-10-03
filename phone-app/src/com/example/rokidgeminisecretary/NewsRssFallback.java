package com.example.rokidgeminisecretary;

import java.io.StringReader;
import java.util.*;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/** Keyless RSS sources. Reject external entities and ignore unusable items. */
public final class NewsRssFallback {
    public static final String[] SOURCES = {
        "https://news.yahoo.co.jp/rss/topics/top-picks.xml",
        "https://www3.nhk.or.jp/rss/news/cat0.xml"
    };
    public interface Fetcher { String fetch(String url) throws Exception; }
    public interface Reporter { void report(String url,int count,int chars,String status); }
    public static final class Article {
        public String title="", description="", published="", link="", source="";
    }
    public static List<Article> fetch(String primary, int max, Fetcher fetcher) {
        return fetch(primary,max,fetcher,(url,count,chars,status)->{});
    }
    public static List<Article> fetch(String primary, int max, Fetcher fetcher, Reporter reporter) {
        List<String> urls = new ArrayList<>();
        if (primary != null && !primary.isEmpty()) urls.add(primary);
        urls.addAll(Arrays.asList(SOURCES));
        for (String url : urls) {
            try {
                List<Article> items = parse(fetcher.fetch(url), max);
                int chars=0;for(Article a:items)chars+=a.title.length()+a.description.length();
                reporter.report(url,items.size(),chars,items.isEmpty()?"empty":"parsed");
                if (!items.isEmpty()) {
                    for (Article item : items) if (item.source.isEmpty())
                        item.source = url.contains("yahoo") ? "Yahoo!ニュース" : url.contains("nhk") ? "NHK" : "Google News";
                    return items;
                }
            } catch (Exception error) { reporter.report(url,0,0,error.getClass().getSimpleName()); }
        }
        return Collections.emptyList();
    }
    public static List<Article> parse(String xml, final int max) throws Exception {
        if (xml == null || xml.length() > 1024*1024 || xml.toUpperCase(Locale.ROOT).contains("<!DOCTYPE"))
            throw new IllegalArgumentException("Invalid RSS size or DTD");
        final List<Article> items = new ArrayList<>();
        final Set<String> titles = new HashSet<>();
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.newSAXParser().parse(new InputSource(new StringReader(safeXml(xml))), new DefaultHandler() {
            Article item;
            String field="";
            int depth, fieldDepth;
            StringBuilder text = new StringBuilder();
            public void startElement(String uri, String local, String q, Attributes attrs) {
                String name = local.isEmpty() ? q : local;
                depth++;
                if (!field.isEmpty() && ("br".equals(name) || "p".equals(name) || "div".equals(name))) text.append("\n");
                if ("item".equals(name)) item = new Article();
                if (item != null && field.isEmpty() && Arrays.asList("title","description","pubDate","link","source").contains(name)) {
                    field=name; fieldDepth=depth; text.setLength(0);
                }
            }
            public void characters(char[] ch, int start, int length) { if (!field.isEmpty()) text.append(ch,start,length); }
            public void endElement(String uri, String local, String q) {
                String name = local.isEmpty() ? q : local;
                if (item != null && name.equals(field) && depth==fieldDepth) {
                    String value = plain(text.toString());
                    if ("title".equals(field)) item.title=value;
                    if ("description".equals(field)) item.description=value;
                    if ("pubDate".equals(field)) item.published=value;
                    if ("link".equals(field)) item.link=value;
                    if ("source".equals(field)) item.source=value;
                    field="";
                }
                if ("item".equals(name) && item != null) {
                    if (!item.title.isEmpty() && titles.add(item.title) && items.size()<max) items.add(item);
                    item=null; field="";
                }
                if (!field.isEmpty() && ("p".equals(name) || "div".equals(name))) text.append("\n");
                depth--;
            }
        });
        return items;
    }
    private static boolean xmlCodePoint(int cp) {
        return cp==9 || cp==10 || cp==13 || cp>=32 && cp<=0xD7FF
                || cp>=0xE000 && cp<=0xFFFD || cp>=0x10000 && cp<=0x10FFFF;
    }
    private static String safeXml(String value) {
        StringBuilder clean=new StringBuilder();
        value.codePoints().forEach(cp->{if(xmlCodePoint(cp))clean.appendCodePoint(cp);});
        String text=clean.toString().replaceAll("&(?!amp;|lt;|gt;|quot;|apos;|#[0-9]+;|#x[0-9a-fA-F]+;)","&amp;");
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("&#(x[0-9a-fA-F]+|[0-9]+);").matcher(text);
        StringBuffer result=new StringBuffer();
        while(m.find()) {
            boolean valid=false;
            try{String n=m.group(1);valid=xmlCodePoint(Integer.parseInt(n.startsWith("x")?n.substring(1):n,n.startsWith("x")?16:10));}catch(Exception ignored){}
            m.appendReplacement(result,java.util.regex.Matcher.quoteReplacement(valid?m.group():" "));
        }
        m.appendTail(result);return result.toString();
    }
    public static String plain(String value) {
        String text=value==null?"":value;
        // Descriptions may contain HTML escaped a second time inside CDATA.
        for(int pass=0;pass<2;pass++) {
            text=text.replace("&lt;","<").replace("&gt;",">").replace("&amp;","&")
                .replace("&nbsp;"," ").replace("&quot;","\"").replace("&apos;","'")
                .replace("&ndash;","–").replace("&mdash;","—").replace("&hellip;","…")
                .replace("&copy;","©").replace("&yen;","¥");
            java.util.regex.Matcher m=java.util.regex.Pattern.compile("&#(x[0-9a-fA-F]+|[0-9]+);").matcher(text);
            StringBuffer decoded=new StringBuffer();
            while(m.find()) {
                String replacement=" ";
                try { String n=m.group(1);int cp=Integer.parseInt(n.startsWith("x")?n.substring(1):n,n.startsWith("x")?16:10);
                    if(Character.isValidCodePoint(cp) && !(cp>=0xD800 && cp<=0xDFFF))replacement=new String(Character.toChars(cp));
                }catch(Exception ignored){}
                m.appendReplacement(decoded,java.util.regex.Matcher.quoteReplacement(replacement));
            }
            m.appendTail(decoded);text=decoded.toString();
        }
        return text.replaceAll("(?is)<(script|style)\\b[^>]*>.*?</\\1>", " ")
            .replaceAll("(?i)<br\\b[^>]*>|</?(?:p|div|li|h[1-6])\\b[^>]*>", "\n")
            .replaceAll("<[^>]*>", " ").replace("\r\n","\n").replace('\r','\n')
            .replaceAll("[\\p{Cntrl}&&[^\\n]]", " ").replaceAll("[\\h\\u00a0]+", " ")
            .replaceAll(" *\n *", "\n").replaceAll("\n{3,}", "\n\n").trim();
    }
}
