package com.example.rokidkeyboardbridge;
public final class AudioNewsRegressionTest {
 static int checks;static void eq(String want,String value){checks++;if(!want.equals(value))throw new AssertionError(checks+": expected "+want+" got "+value);}static void ok(boolean v){checks++;if(!v)throw new AssertionError("check "+checks);}
 public static void main(String[] args){
 eq("",NewsQueryPolicy.extract("そーねｗ、えっとね ニュース教えて"));
 eq("",NewsQueryPolicy.extract("そね、えっとねのニュースを教えて"));
 eq("",NewsQueryPolicy.extract("そうですね、ええと、今日のニュースを教えてください"));
 eq("",NewsQueryPolicy.extract("ニュース教えて"));
 eq("経済",NewsQueryPolicy.extract("えっとね、経済のニュースを教えて"));
 eq("神奈川",NewsQueryPolicy.extract("神奈川のニュースを教えて"));
 eq("はがね",NewsQueryPolicy.extract("はがねのニュース教えて"));
 eq("そね",NewsQueryPolicy.extract("「そね」のニュース教えて").replace("「", "").replace("」", ""));
 eq("そね",NewsQueryPolicy.extract("そねのニュース教えて"));
 eq("ソニー",NewsQueryPolicy.extract("ソニーのニュースを教えて"));
 eq("高市早苗",NewsQueryPolicy.extract("高市のニュース教えて"));
 eq("",NewsQueryPolicy.extract("そっか、えっとね、ちょっとニュース教えて"));
 eq("",NewsQueryPolicy.extract("そっか えっとね ちょっと ニュースを教えて"));
 eq("経済",NewsQueryPolicy.extract("そっか、ちょっと、経済のニュースを教えて"));
 eq("あのねこ",NewsQueryPolicy.extract("あのねこのニュース教えて"));
 eq("えっとねこ",NewsQueryPolicy.extract("えっとねこのニュース教えて"));
 eq("ちょっと書店",NewsQueryPolicy.extract("ちょっと書店のニュース教えて"));
 eq("「ニュースの現場」",NewsQueryPolicy.extract("「ニュースの現場」のニュース教えて"));
 eq("「教えてニュース」",NewsQueryPolicy.extract("えっと、 「教えてニュース」のニュース教えて"));
 ok(ConversationCorrections.explicit("電線じゃなく沿線だよ"));
 ok(ConversationCorrections.explicit("名前ではなく地名の話だよ"));
 ok(!ConversationCorrections.explicit("沿線のニュースを教えて"));
 NativeSpeechSession g=new NativeSpeechSession();ok(!g.takeStop(100));g.sent(100,10000);ok(g.active(500));ok(g.takeStop(500));ok(!g.takeStop(501));g.sent(600,10000);ok(g.takeStop(601));g.sent(1000,2000);ok(!g.takeStop(3000));ok(!g.active(3001));
 System.out.println("PASS "+checks+" news framing / media stop ownership checks");
 }
}
