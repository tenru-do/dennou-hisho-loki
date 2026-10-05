package com.example.rokidkeyboardbridge;

/** Styles the native, GPS-centered map. No screen capture or image cropping. */
final class HudMapPresentation {
    static final int SIZE_DP = 126;
    static final float MAP_SCALE = 1.0f;

    static int sizeDp(boolean largeMap) {
        return largeMap ? 180 : 82;
    }

    static int roadIntensity(int color) {
        int r = (color >>> 16) & 255;
        int g = (color >>> 8) & 255;
        int b = color & 255;
        int min = Math.min(r, Math.min(g, b));
        int max = Math.max(r, Math.max(g, b));
        // White minor roads, warm-colored main roads; suppress grey buildings/land.
        if (min >= 246 && max - min <= 9) return 135;
        if (r >= 235 && g >= 170 && b >= 110 && b <= 218 && r - b >= 25) return 170;
        if (r >= 205 && g >= 115 && g <= 195 && b >= 125 && r - g >= 30) return 170;
        return 0;
    }

    static int tileIntensity(int color, int left, int right, int above, int below) {
        int road = roadIntensity(color);
        if (road > 0) return road;
        int r = (color >>> 16) & 255;
        int g = (color >>> 8) & 255;
        int b = color & 255;
        int luma = (r * 30 + g * 59 + b * 11) / 100;
        // Preserve dark ink/outlines adjacent to roads, not every building edge.
        boolean roadNeighbour = roadIntensity(left) > 0 || roadIntensity(right) > 0
                || roadIntensity(above) > 0 || roadIntensity(below) > 0;
        if (roadNeighbour && luma < 100) return 210;
        if (roadNeighbour && luma < 205) return 70;
        return 0;
    }
}
