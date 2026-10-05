package com.example.rokidkeyboardbridge;
public final class ContextFieldTextTest {
    public static void main(String[] args) {
        for (Object invalid : new Object[]{null, "", "  ", 123, new Object()})
            if (!ContextFieldText.value(invalid).isEmpty()) throw new AssertionError("invalid field accepted");
        if (!"歩数 123".equals(ContextFieldText.value(" 歩数 123 ")))
            throw new AssertionError("valid sibling lost");
        System.out.println("PASS: null, empty, whitespace, number, null sentinel, valid sibling");
    }
}
