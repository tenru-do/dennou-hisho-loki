package com.example.rokidgeminisecretary;
import org.json.*;
final class NavigationHealthPolicy{
 static JSONObject snapshot(long now,long routeAt,long locationAt,String error,boolean fetching,String selected,String requested,String points)throws Exception{
  long routeAge=routeAt>0&&now>=routeAt?now-routeAt:-1,locationAge=locationAt>0&&now>=locationAt?now-locationAt:-1;String status=fetching?"fetching":locationAge<0?"location_missing":locationAge>120000?"location_stale":!error.isEmpty()?"route_error":points==null||points.length()<5?"route_missing":routeAge<0||routeAge>600000?"route_stale":"ready";
  String message=status.equals("fetching")?"経路を取得中です":status.equals("location_missing")?"現在地を取得できていません。位置情報の権限・GPSを確認してください":status.equals("location_stale")?"現在地が2分以上更新されていません":status.equals("route_error")?"経路取得でエラーがあります："+error:status.equals("route_missing")?"経路線が未取得です":status.equals("route_stale")?"経路情報が古いため再取得を確認してください":"現在地と保存経路は取得済みです";
  if(!selected.isEmpty()&&!requested.isEmpty()&&!selected.equals(requested)){status="destination_mismatch";message="指定した目的地と保存経路の目的地が異なります。目的地を確認して再取得してください";}
  return new JSONObject().put("status",status).put("message",message).put("routeAgeMs",routeAge).put("locationAgeMs",locationAge).put("selectedDestination",selected).put("requestedDestination",requested).put("destinationMismatch",!selected.isEmpty()&&!requested.isEmpty()&&!selected.equals(requested));
 }
 private NavigationHealthPolicy(){}
}
