package com.example.rokidgeminisecretary;
public final class MapsKeyInputTest {
    public static void main(String[] args) {
        String sample = "opaque_test_token-not-a-real-key";
        for (String input : new String[]{sample, " \n" + sample + "\r\n", "\ufeff\u00a0\"" + sample + "\"\u200b", "\u201c" + sample + "\u201d"}) {
            if (!sample.equals(MapsKeyInput.normalize(input))) throw new AssertionError("paste boundary normalization");
        }
        if (!MapsKeyInput.canSend(sample)) throw new AssertionError("must not assume prefix or 39 chars");
        for (String bad : new String[]{"", "test\r\nHeader:value", "test value", "test\u200bvalue", "全角"}) {
            if (MapsKeyInput.canSend(bad)) throw new AssertionError("unsafe header input");
        }
        System.out.println("Maps input normalization and transport checks passed");
    }
}
