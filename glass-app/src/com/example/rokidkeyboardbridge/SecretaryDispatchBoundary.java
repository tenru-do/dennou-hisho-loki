package com.example.rokidkeyboardbridge;
/** Explicit new operations bypass inbox classification; no calendar write or provider call here. */
final class SecretaryDispatchBoundary {
 static boolean calendar(String current){if(current==null)return false;String t=SecretaryInboxPolicy.compact(current);if(t.length()>3000||t.matches("(?s).*(と言った|と言われ|という例文|という意味|とは言って|って言った).*"))return false;if(t.matches(".*(?:あとで|後で|後ほど).*(?:登録|追加).*"))return false;if(SecretaryInboxPolicy.defer(t)||SecretaryInboxPolicy.resume(t)||SecretaryInboxPolicy.list(t)||!SecretaryInboxPolicy.id(t).isEmpty())return false;return CalendarWriteIntent.requested(t,"");}
 static boolean attachment(String current){return calendar(current)&&SecretaryInboxPolicy.compact(current).matches("(?s).*(添付|予定表|予約票|予約券|この画像|この写真|その画像|その写真).*");}
}
