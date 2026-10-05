package com.example.rokidkeyboardbridge;

public final class HudMapPresentationTest {
    public static void main(String[] args) {
        if (HudMapPresentation.roadIntensity(0xffe0dfdf) != 0
                || HudMapPresentation.roadIntensity(0xffd9d0c9) != 0
                || HudMapPresentation.roadIntensity(0xfff2efe9) != 0
                || HudMapPresentation.roadIntensity(0xffaad3df) != 0
                || HudMapPresentation.roadIntensity(0xffffffff) < 100
                || HudMapPresentation.roadIntensity(0xfffcd6a4) < 150
                || HudMapPresentation.roadIntensity(0xffe892a2) < 150)
            throw new AssertionError("Roads must stand out from buildings/background");
        if (HudMapPresentation.tileIntensity(0xff222222,0xffffffff,0,0,0) < 200
                || HudMapPresentation.tileIntensity(0xff222222,0xffe0dfdf,0xffe0dfdf,0xffe0dfdf,0xffe0dfdf) != 0)
            throw new AssertionError("Only road-adjacent ink should remain");
        if (HudMapPresentation.SIZE_DP < 120 || HudMapPresentation.MAP_SCALE != 1.0f)
            throw new AssertionError("Native map readability");
        System.out.println("native map palette checks passed");
        if (HudMapPresentation.sizeDp(false) != 82
                || HudMapPresentation.sizeDp(true) != 180)
            throw new AssertionError("Explicit compact/large modes must stay stable");
    }
}
