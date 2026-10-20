package com.example.rokidkeyboardbridge;
/** Cleans request framing, never deletes particles inside a topic or quoted text. */
final class NewsQueryPolicy {
 static String extract(String input){
  String v=input==null?"":java.text.Normalizer.normalize(input,java.text.Normalizer.Form.NFKC).trim();
  if(v.isEmpty())return "";
  // Quoted names may themselves contain words such as "ニュース" or "教えて".
  java.util.ArrayList<String> quoted=new java.util.ArrayList<>();java.util.regex.Matcher quotes=java.util.regex.Pattern.compile("「[^「」]*」|『[^『』]*』|\"[^\"]*\"").matcher(v);StringBuffer masked=new StringBuffer();while(quotes.find()){String key="\uE000"+quoted.size()+"\uE001";quoted.add(quotes.group());quotes.appendReplacement(masked,java.util.regex.Matcher.quoteReplacement(key));}quotes.appendTail(masked);v=masked.toString();
  if(!(v.startsWith("「")||v.startsWith("『")||v.startsWith("\""))){
   for(int i=0;i<12;i++){
    String old=v;
    v=v.replaceFirst("^えっとね(?=のニュース)","");
    v=v.replaceFirst("^(?:そうですね|そうだね|そうね|そーね|そね|そっか|そっかあ|そっかー|そうか|そうかあ|えーっとね|えーっと|えっとね|えっと|ええと|あのね|あのー|うーん|ちょっと)(?=[wｗ笑\\s、，,。]|$|(?:今日の?|本日の?|最新の?)?ニュース)(?:[wｗ笑\\s、，,。]*)", "").trim();
    if(v.equals(old))break;
   }
  }
  v=v.replaceAll("(?i)(?:今日|本日|昨日|最新)の?ニュース|news|ニュース", " ");
  v=v.replaceAll("(?:を)?(?:教えてください|教えて下さい|教えてほしい|教えて|調べてください|調べて|聞かせてください|聞かせて)"," ");
  v=v.replaceAll("(?:よっぱらっていたよね|酔っぱらっていたよね|酔っていたよね|よっぱらっていた|酔っぱらっていた|酔っていた|だったよね|だよね)"," ");
  v=v.replaceAll("^[\\s、，,。!?？！（）()]+|[\\s、，,。!?？！（）()]+$","").replaceAll("\\s+"," ").trim();
  v=v.replaceFirst("(?:について|に関する|に関連する|のこと|の|を|は)$", "").trim();
  if(v.matches("(?:今日|本日|昨日|最新|今の|お願いします|お願い|ください|下さい|ちょうだい|見せて|何か|何かある|ある)"))return "";
  if(v.contains("高市")&&!v.contains("高市早苗"))v=v.replace("高市","高市早苗");
  for(int i=0;i<quoted.size();i++)v=v.replace("\uE000"+i+"\uE001",quoted.get(i));
  return v.length()<=1?"":v.substring(0,Math.min(80,v.length()));
 }
 private NewsQueryPolicy(){}
}
