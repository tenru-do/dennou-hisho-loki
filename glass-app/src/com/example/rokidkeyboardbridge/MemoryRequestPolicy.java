package com.example.rokidkeyboardbridge;

/** Classifies only the current utterance, never stored context or conversation history. */
final class MemoryRequestPolicy {
    static String requiredFunction(String request) {
        if (request == null) return "";
        if (request.matches("(?s).*(しないで|しなくて|しない|不要|やめて|ないで|なくていい|とは言っていない).*")
                || request.matches("(?s).*(という|と言う|といった|っていう|という意味|の意味|と発話|と言った|って言った|と頼んだ).*") ) return "";
        if (request.matches("(?s).*(記憶|メモ|覚えた).*(削除して|消して|忘れて).*")
                || request.matches("(?s).*(忘れてください|忘れておいて).*") ) return "delete_memory";
        if (request.matches("(?s).*(覚えておいて|覚えていて|覚えてね|覚えてください|記憶して|記録して|メモして).*")
                || request.matches("(?s).*(記憶|メモ).*(保存して).*") ) return "save_memory";
        return "";
    }

    static boolean concernsMemory(String request) {
        return !requiredFunction(request).isEmpty()
                || (request != null && request.matches("(?s).*(記憶|メモ|覚えて|覚えた).*") );
    }
}
