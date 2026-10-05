package com.example.rokidkeyboardbridge;

public final class CustomInstructionsMergeTest {
    private static void check(String expected, String actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected=" + expected + " actual=" + actual);
        }
    }

    public static void main(String[] args) {
        check("glass", CustomInstructionsMerge.merge("glass", "", 20));
        check("glass\nphone", CustomInstructionsMerge.merge("glass", "phone", 20));
        check("glass\nphone", CustomInstructionsMerge.merge("glass\nphone", "glass\nphone", 20));
        check("glass\nnew", CustomInstructionsMerge.merge("glass", "new", 9));
        check(null, CustomInstructionsMerge.merge("glass", "new", 8));
        check("glass\nold\nnew", CustomInstructionsMerge.merge("glass\nold", "new", 100));
        check("edited\nfull", CustomInstructionsMerge.replaceIfCurrent("glass", "glass", "edited\nfull", 20));
        check(null, CustomInstructionsMerge.replaceIfCurrent("changed", "glass", "edited", 20));
        check(null, CustomInstructionsMerge.replaceIfCurrent("glass", "glass", "", 20));
        check(null, CustomInstructionsMerge.replaceIfCurrent("glass", "glass", "too long", 4));
        System.out.println("CustomInstructionsMergeTest passed");
    }
}
