package com.example.rokidkeyboardbridge;
public final class ContextFieldText {
    public static String value(Object value) {
        return value instanceof String ? ((String) value).trim() : "";
    }
    private ContextFieldText() {}
}
