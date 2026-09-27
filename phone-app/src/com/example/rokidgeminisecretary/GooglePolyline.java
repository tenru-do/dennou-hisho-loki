package com.example.rokidgeminisecretary;
import java.util.ArrayList;
final class GooglePolyline {
    static double[][] decode(String encoded) {
        ArrayList<double[]> points = new ArrayList<double[]>();
        int[] cursor = {0};
        long lat = 0, lng = 0;
        while (cursor[0] < encoded.length()) {
            lat += component(encoded, cursor);
            lng += component(encoded, cursor);
            if (Math.abs(lat) > 9000000 || Math.abs(lng) > 18000000 || points.size() >= 100000)
                throw new IllegalArgumentException("invalid polyline");
            points.add(new double[]{lat / 1e5, lng / 1e5});
        }
        if (points.size() < 2) throw new IllegalArgumentException("empty polyline");
        return points.toArray(new double[points.size()][]);
    }
    private static long component(String s, int[] cursor) {
        long value = 0;
        for (int shift = 0; shift <= 30; shift += 5) {
            if (cursor[0] >= s.length()) throw new IllegalArgumentException("truncated polyline");
            int b = s.charAt(cursor[0]++) - 63;
            if (b < 0 || b > 63) throw new IllegalArgumentException("invalid polyline");
            value |= (long)(b & 31) << shift;
            if (b < 32) return (value & 1) == 0 ? value >> 1 : ~(value >> 1);
        }
        throw new IllegalArgumentException("invalid polyline");
    }
}
