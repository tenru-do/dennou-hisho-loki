package com.example.rokidgeminisecretary;

import java.util.ArrayList;
import java.util.ArrayDeque;

/** Preserve bends, unlike fixed-stride decimation which cuts across streets. */
final class RouteGeometry {
    static ArrayList<Integer> indices(double[][] points, int limit) {
        ArrayList<Integer> result = new ArrayList<Integer>();
        if (points.length <= limit) {
            for (int i = 0; i < points.length; i++) result.add(i);
            return result;
        }
        for (double tolerance = 2; ; tolerance *= 1.5) {
            boolean[] keep = new boolean[points.length];
            keep[0] = keep[points.length - 1] = true;
            ArrayDeque<int[]> stack = new ArrayDeque<int[]>();
            stack.push(new int[]{0, points.length - 1});
            while (!stack.isEmpty()) {
                int[] pair = stack.pop();
                double max = tolerance * tolerance;
                int chosen = -1;
                for (int i = pair[0] + 1; i < pair[1]; i++) {
                    double d = distance(points[i], points[pair[0]], points[pair[1]]);
                    if (d > max) { max = d; chosen = i; }
                }
                if (chosen >= 0) {
                    keep[chosen] = true;
                    stack.push(new int[]{pair[0], chosen});
                    stack.push(new int[]{chosen, pair[1]});
                }
            }
            result.clear();
            for (int i = 0; i < keep.length; i++) if (keep[i]) result.add(i);
            if (result.size() <= limit) return result;
        }
    }
    private static double distance(double[] p, double[] a, double[] b) {
        double scale = 111320 * Math.cos(Math.toRadians(a[0]));
        double x = (p[1] - a[1]) * scale, y = (p[0] - a[0]) * 111320;
        double dx = (b[1] - a[1]) * scale, dy = (b[0] - a[0]) * 111320;
        double len = dx * dx + dy * dy;
        double t = len == 0 ? 0 : Math.max(0, Math.min(1, (x * dx + y * dy) / len));
        return (x - t * dx) * (x - t * dx) + (y - t * dy) * (y - t * dy);
    }
}
