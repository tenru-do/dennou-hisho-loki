package com.example.rokidkeyboardbridge;
import org.json.*;
public final class NavigationPipelineTiming {
 public static void accept(NavigationSmoothMetrics metrics,JSONObject timing){if(timing==null||timing.optInt("version")!=1)return;long build=timing.optLong("phoneBuildNanos",-1),age=timing.optLong("locationAgeMs",-1);if(build>=0&&build<=600000000000L)metrics.sample("phoneBuild",build);if(age>=0&&age<=600000)metrics.sample("locationAgeAtPhone",age*1000000L);}
 public static void fixAge(NavigationSmoothMetrics metrics,JSONObject timing,long roundTripNanos,long receiveNanos,long frameNanos){if(timing==null||timing.optInt("version")!=1||!timing.has("locationAgeMs")||roundTripNanos<0||roundTripNanos>600000000000L||frameNanos<receiveNanos||frameNanos-receiveNanos>600000000000L)return;long age=timing.optLong("locationAgeMs",-1);if(age<0||age>600000)return;long lower=age*1000000L+(frameNanos-receiveNanos);if(lower>600000000000L)return;metrics.sample("fixAgeLower",lower);if(lower+roundTripNanos<=600000000000L)metrics.sample("fixAgeUpper",lower+roundTripNanos);}
 private NavigationPipelineTiming(){}
}
