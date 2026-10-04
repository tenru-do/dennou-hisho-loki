package com.example.rokidgeminisecretary;
import java.util.*;
public final class ColumnSpeechFormatterTest {
    static int checks;static void check(boolean ok){checks++;if(!ok)throw new AssertionError("column check "+checks);}
    public static void main(String[] args){
        String body="タイトル。\n本文です。\n\n続きです。";
        String opening="マスター、10月4日 日曜日、昼の知識コラムです。";
        check(ColumnSpeechFormatter.format(opening+body,true).equals(body));
        check(ColumnSpeechFormatter.format(opening+body,false).equals(opening+body));
        check(ColumnSpeechFormatter.format(body,true).equals(body));
        check(ColumnSpeechFormatter.format("朝の知識コラムです。"+body,true).equals(body));
        check(!ColumnSpeechFormatter.accepts("あ".repeat(399)));check(ColumnSpeechFormatter.accepts("あ".repeat(400)));
        check(ColumnSpeechFormatter.accepts("あ".repeat(600)));check(!ColumnSpeechFormatter.accepts("あ".repeat(601)));
        Set<String> used=new HashSet<>();Random random=new Random(42);
        for(int i=0;i<LongColumnCatalog.COLUMNS.length;i++){
            String[] c=LongColumnCatalog.select(used,random);
            System.out.println("column="+c[0]+" chars="+c[1].length());
            check(ColumnSpeechFormatter.accepts(c[1]));check(used.size()==i+1);check(c[1].contains("\n"));
            check(ColumnSpeechFormatter.format(c[1],true).equals(c[1]));
            String[] chunks=com.example.rokidkeyboardbridge.TtsSentenceBuffer.splitTopic(c[1]);
            check(String.join("",chunks).replaceAll("\\s","").equals(c[1].replaceAll("\\s","")));
            for(String chunk:chunks){int sentences=chunk.length()-chunk.replace("。","").length();check(sentences>=2&&sentences<=3);}
        }
        LongColumnCatalog.select(used,random);check(used.size()==1);
        System.out.println("Column speech PASS checks="+checks);
    }
}
