package com.example.rokidgeminisecretary;
import org.json.*;
/** One immutable parsed route and prefix distances, in memory only. */
public final class NavigationGeometryCache {
 public final String routeText,maneuverText,identity;public final JSONArray route,maneuvers;private final double[] prefix;
 public NavigationGeometryCache(String points,String steps)throws Exception{routeText=points;maneuverText=steps;identity=points+"|"+steps;route=new JSONArray(points);maneuvers=new JSONArray(steps);prefix=new double[route.length()];for(int i=1;i<route.length();i++){JSONArray a=route.getJSONArray(i-1),b=route.getJSONArray(i);double x=Math.sin(Math.toRadians(a.getDouble(0)-b.getDouble(0))/2),y=Math.sin(Math.toRadians(a.getDouble(1)-b.getDouble(1))/2);double h=x*x+Math.cos(Math.toRadians(a.getDouble(0)))*Math.cos(Math.toRadians(b.getDouble(0)))*y*y;prefix[i]=prefix[i-1]+12742000*Math.asin(Math.sqrt(Math.max(0,Math.min(1,h))));}}
 public boolean matches(String points,String steps){return routeText.equals(points)&&maneuverText.equals(steps);}
 public float distance(int from,int to){if(prefix.length==0)return 0;int a=Math.max(0,Math.min(prefix.length-1,from)),b=Math.max(a,Math.min(prefix.length-1,to));return (float)(prefix[b]-prefix[a]);}
}
