package com.example.rokidkeyboardbridge;

public final class NavigationHudTextTest {
    private static void check(String p, String s, String d, String expected) {
        if (!expected.equals(NavigationHudText.instruction(p,s,d))) throw new AssertionError(p);
    }
    public static void main(String[] args) {
        check("北東に進む", "午後11:17 着", "", "北東に進む");
        check("100m", "右折して昭和通りへ", "100m", "右折して昭和通りへ");
        check("100m 右折", "午後11:17 着", "100m", "右折");
        check("3分", "東へ進む", "", "東へ進む");
        check("新宿駅で乗り換え", "午後3:17 着", "", "新宿駅で乗り換え");
        check("", "南へ進む", "", "南へ進む");
        check("Googleマップ画面共有", "", "", "");
        System.out.println("7 navigation guidance text checks passed");
    }
}
