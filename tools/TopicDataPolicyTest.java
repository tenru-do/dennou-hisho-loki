package com.example.rokidgeminisecretary;
import java.util.*;
public final class TopicDataPolicyTest {
    static void check(boolean value,String name){if(!value)throw new AssertionError(name);}
    public static void main(String[] args)throws Exception {
        String xml="<rss><channel><item><title><![CDATA[<b>科学 &amp; IT</b> &#x1F680;]]></title><description><p>文章 <b>内側<title>見出し</title></b> 続き</p></description></item>"
                +"<item><title>A & B &nbsp; &#0; "+(char)1+" 特殊文字</title><description><![CDATA[&lt;script&gt;bad&lt;/script&gt;<b>安全</b>]]></description></item></channel></rss>";
        List<NewsRssFallback.Article> items=NewsRssFallback.parse(xml,10);
        check(items.size()==2,"tolerant XML");
        check(items.get(0).title.equals("科学 & IT 🚀"),"CDATA HTML numeric entities");
        check(items.get(0).description.equals("文章 内側見出し 続き"),"nested HTML field ownership");
        check(items.get(1).title.equals("A & B 特殊文字"),"bare ampersand HTML entity invalid XML characters");
        check(items.get(1).description.equals("安全"),"escaped script sanitization");
        for(int max:new int[]{0,1,2,3,299,300}) {
            String limited=TopicTextPolicy.limit(String.join("",Collections.nCopies(200,"🚀日本語")),max);
            check(limited.length()<=max,"news cap");
            for(int i=0;i<limited.length();i++) {
                char c=limited.charAt(i);
                if(Character.isHighSurrogate(c)){check(i+1<limited.length()&&Character.isLowSurrogate(limited.charAt(++i)),"unpaired high surrogate");}
                else check(!Character.isLowSurrogate(c),"unpaired low surrogate");
            }
        }
        check(TopicTextPolicy.region("東京都 立川市").equals("立川"),"relaxed query");
        check(TopicTextPolicy.region("東京都 八王子市").equals("八王子市"),"municipality query");
        check(LocalPoiCatalog.within(35.698,139.413).size()==3,"Tachikawa offline POI");
        check(LocalPoiCatalog.within(34.69,135.50).isEmpty(),"no distant POI");
        check(LocalPoiCatalog.within(Double.NaN,139.4).isEmpty(),"invalid location");
        check(TopicTextPolicy.bytes("日本語")==9,"UTF8 payload bytes");
        System.out.println("TopicDataPolicyTest PASS: XML edge cases, Unicode limits, relaxed query, 10km POI");
    }
}
