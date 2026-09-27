package com.example.rokidgeminisecretary;
import java.util.ArrayList;
public class NavigationPolicyTest {
    public static void main(String[] args) {
        double[][] points = new double[6001][2];
        for (int i=0; i<points.length; i++) {
            points[i][0]=35 + Math.min(i, 3000) * 0.00001;
            points[i][1]=139 + Math.max(0, i-3000) * 0.00001;
        }
        ArrayList<Integer> kept = RouteGeometry.indices(points,4096);
        if (kept.get(0)!=0 || kept.get(kept.size()-1)!=6000 || !kept.contains(3000)) throw new AssertionError("corner/endpoints lost");
        if (kept.size()>4096) throw new AssertionError("unbounded route");
        if (!"国立駅".equals(NavigationLabels.destination("目的地：国立駅"))) throw new AssertionError("destination");
        if (!"".equals(NavigationLabels.destination("300m先を左折"))) throw new AssertionError("false destination");
        if (!"bicycling".equals(NavigationLabels.mode("自転車で到着"))) throw new AssertionError("bike");
        if (NavigationLabels.clean("\u202e新宿\u200e").contains("\u202e")) throw new AssertionError("bidi");
        System.out.println("navigation regression checks passed");
        if (!"立飛駅".equals(NavigationLabels.transitAccessStation("徒歩 28 分（1.9 km）", "立飛駅 · 午後6:13 発"))) throw new AssertionError("transit access leg");
        if (!"".equals(NavigationLabels.transitAccessStation("電車 28分", "立飛駅 · 午後6:13 発"))) throw new AssertionError("rail is not walking");
        if (!"".equals(NavigationLabels.transitAccessStation("徒歩 28分", "午後7:18 着"))) throw new AssertionError("arrival is not station");
        if (!"".equals(NavigationLabels.destination("立飛駅 · 午後6:13 発"))) throw new AssertionError("station is not final destination");
        System.out.println("transit access leg checks passed");
        if (!"国立駅".equals(NavigationLabels.transitAccessStation("自転車 8分", "国立駅 · 午前9:30 発"))) throw new AssertionError("bicycle access");
        if (!"bicycling".equals(NavigationLabels.accessMode("自転車 8分"))) throw new AssertionError("bicycle mode");
        if (!NavigationLabels.accessMode("電車 8分").isEmpty()) throw new AssertionError("rail must not be road access");
        if (!NavigationLabels.transitAccessStation("自転車 8分", "職場").isEmpty()) throw new AssertionError("do not guess station");
    }
}
