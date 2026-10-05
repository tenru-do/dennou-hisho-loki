package com.example.rokidkeyboardbridge;
public final class RouteOverviewTest {
    static void check(double[][] points,double lat,double lng,int w,int h,int pad) {
        RouteOverview fit=RouteOverview.fit(points,lat,lng,w,h,pad);
        double scale=256*(1<<fit.zoom),cx=RouteOverview.x(fit.longitude),cy=RouteOverview.y(fit.latitude);
        for(double[] p:points){
            if(Math.abs(RouteOverview.x(p[1])-cx)*scale>w/2d-pad+0.01 || Math.abs(RouteOverview.y(p[0])-cy)*scale>h/2d-pad+0.01)
                throw new AssertionError("route outside viewport");
        }
        if(Math.abs(RouteOverview.x(lng)-cx)*scale>w/2d-pad+0.01 || Math.abs(RouteOverview.y(lat)-cy)*scale>h/2d-pad+0.01)
            throw new AssertionError("current position outside viewport");
    }
    public static void main(String[] args) {
        check(new double[][]{{35.7,139.4},{35.71,139.405}},35.69,139.395,270,270,45);
        check(new double[][]{{35.7,139.4},{35.68,139.76},{34.7,135.5}},35.71,139.41,270,270,45);
        check(new double[][]{{35.7,139.4},{35.7,139.4}},35.7,139.4,270,270,45);
        check(new double[][]{{35.7,139.4},{36,140}},35.5,139,320,480,30);
        System.out.println("Route overview: short/long/zero routes, current position and bounds PASS");
    }
}
