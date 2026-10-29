package com.example.rokidgeminisecretary;
/** Full addresses can be set directly; facility-only names still need review. */
final class CalendarLocationPolicy {static boolean explicit(String s){return s!=null&&s.length()<=200&&(s.matches(".*[都道府県].*[市区町村].*[0-9０-９一二三四五六七八九十]+.*")||s.matches("[-+]?[0-9]{1,2}\\.[0-9]+[,、 ]+[-+]?[0-9]{1,3}\\.[0-9]+"));}private CalendarLocationPolicy(){}}
