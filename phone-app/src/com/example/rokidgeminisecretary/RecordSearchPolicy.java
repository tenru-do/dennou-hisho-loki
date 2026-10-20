package com.example.rokidgeminisecretary;
final class RecordSearchPolicy {
 static String normalized(String value){return java.text.Normalizer.normalize(value==null?"":value,java.text.Normalizer.Form.NFKC).toLowerCase(java.util.Locale.ROOT).replaceAll("[\\s　]+"," ").trim();}
 static boolean matches(String text,String query){String value=normalized(text),q=normalized(query);if(q.isEmpty()||q.length()>160)return false;for(String term:q.split(" "))if(!value.contains(term))return false;return true;}
 private RecordSearchPolicy(){}
}
