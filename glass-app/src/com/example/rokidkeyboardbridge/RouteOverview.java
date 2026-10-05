package com.example.rokidkeyboardbridge;

/** Web-Mercator fitting independent of Android so bounds can be regression tested. */
final class RouteOverview {
    final double latitude, longitude;
    final int zoom;
    RouteOverview(double lat, double lng, int z) { latitude = lat; longitude = lng; zoom = z; }
    static double x(double lng) { return (lng + 180) / 360; }
    static double y(double lat) {
        double r = Math.toRadians(Math.max(-85, Math.min(85, lat)));
        return (1 - Math.log(Math.tan(r) + 1 / Math.cos(r)) / Math.PI) / 2;
    }
    static RouteOverview fit(double[][] points, double lat, double lng, int width, int height, int padding) {
        double minX=x(lng), maxX=minX, minY=y(lat), maxY=minY;
        for (double[] p : points) {
            if (p == null || p.length < 2 || !Double.isFinite(p[0]) || !Double.isFinite(p[1])) continue;
            double px=x(p[1]), py=y(p[0]);
            minX=Math.min(minX,px); maxX=Math.max(maxX,px);
            minY=Math.min(minY,py); maxY=Math.max(maxY,py);
        }
        int z=16;
        while (z>1 && ((maxX-minX)*256*(1<<z)>Math.max(1,width-2*padding)
                || (maxY-minY)*256*(1<<z)>Math.max(1,height-2*padding))) z--;
        double cy=(minY+maxY)/2;
        return new RouteOverview(Math.toDegrees(Math.atan(Math.sinh(Math.PI*(1-2*cy)))),
                (minX+maxX)/2*360-180,z);
    }
}
