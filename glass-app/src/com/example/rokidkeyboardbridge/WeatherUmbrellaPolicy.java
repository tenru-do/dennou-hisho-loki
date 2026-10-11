package com.example.rokidkeyboardbridge;
/** Kana must express an umbrella intent, not a suffix such as softness. */
final class WeatherUmbrellaPolicy {
 static boolean kanaUmbrella(String value){
  if(value==null)return false;
  return value.matches("(?:かさ|カサ)[？?！!。\\s]*") ||
   java.util.regex.Pattern.compile("(?:かさ|カサ)(?:は|を|が|って)?(?:必要|いる|要る|いら|要ら|持|使|差す|さす|忘|買)").matcher(value).find();
 }
}
