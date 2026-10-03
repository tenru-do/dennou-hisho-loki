package com.example.rokidgeminisecretary;
import java.util.*;
/** Officially documented Tachikawa landmarks. Approximate representative coordinates, not entrance routing. */
public final class LocalPoiCatalog {
    public static final double RADIUS_METERS=10000;
    public static final class Poi {
        public final String name, description, url;
        public final double latitude, longitude;
        Poi(String name,String description,String url,double lat,double lon){this.name=name;this.description=description;this.url=url;latitude=lat;longitude=lon;}
    }
    private static final List<Poi> ITEMS=Arrays.asList(
        new Poi("国営昭和記念公園","散策や自然観察の立ち寄り候補です。","https://www.showakinen-koen.jp/access/",35.7047,139.4030),
        new Poi("GREEN SPRINGS","立川駅北側の商業・文化施設です。","https://greensprings.jp/access/",35.7034,139.4120),
        new Poi("ららぽーと立川立飛","買い物や食事の立ち寄り候補です。","https://mitsui-shopping-park.com/lalaport/tachikawa/access/",35.7142,139.4178));
    public static double distance(double lat,double lon,double a,double b) {
        double dlat=Math.toRadians(a-lat),dlon=Math.toRadians(b-lon);
        double h=Math.pow(Math.sin(dlat/2),2)+Math.cos(Math.toRadians(lat))*Math.cos(Math.toRadians(a))*Math.pow(Math.sin(dlon/2),2);
        return 6371000*2*Math.asin(Math.sqrt(Math.min(1,h)));
    }
    public static List<Poi> within(double lat,double lon) {
        List<Poi> found=new ArrayList<>();
        if(!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180)return found;
        for(Poi p:ITEMS)if(distance(lat,lon,p.latitude,p.longitude)<=RADIUS_METERS)found.add(p);
        found.sort((a,b)->Double.compare(distance(lat,lon,a.latitude,a.longitude),distance(lat,lon,b.latitude,b.longitude)));
        return found;
    }
}
