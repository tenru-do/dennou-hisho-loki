package com.example.rokidkeyboardbridge;
import java.util.*;

/** Completed answers are buffered into sentence groups; commas/newlines are not dispatch points. */
public final class TtsSentenceBuffer {
    public static final int TARGET=240, MAX=420;
    public static String[] splitTopic(String input) { return split(input,true); }
    public static String[] split(String input) { return split(input,false); }
    private static String[] split(String input,boolean keepLines) {
        String text=input==null?"":input.replaceAll("[*#`]", "").replaceAll(keepLines ? "[\\h]+" : "\\s+", " ").trim();
        if(text.isEmpty())return new String[0];
        List<String> chunks=new ArrayList<>();
        int start=0;
        while(start<text.length()) {
            int hard=Math.min(text.length(),start+MAX);
            if(hard<text.length()&&Character.isHighSurrogate(text.charAt(hard-1)))hard--;
            int end=hard;
            if(hard<text.length()) {
                int sentence=-1, soft=-1;
                for(int i=start;i<hard;i++) {
                    char c=text.charAt(i);
                    if(c=='。'||c=='！'||c=='？'||c=='!'||c=='?') {
                        sentence=i+1;if(i+1-start>=TARGET){end=i+1;break;}
                    }
                    if((c=='、'||c==','||c==' ') && i-start>=TARGET)soft=i+1;
                }
                if(end==hard && sentence>start)end=sentence;
                else if(end==hard && soft>start)end=soft;
            }
            chunks.add(text.substring(start,end).trim());start=end;
        }
        if(chunks.size()>1) {
            int last=chunks.size()-1;
            if(chunks.get(last).length()<100 && chunks.get(last-1).length()+chunks.get(last).length()<=MAX) {
                chunks.set(last-1,chunks.get(last-1)+chunks.remove(last));
            }
        }
        return chunks.toArray(new String[0]);
    }
}
