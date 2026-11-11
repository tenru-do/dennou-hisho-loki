package com.example.rokidkeyboardbridge;

/** Classifies only the current utterance, never stored context or conversation history. */
final class MemoryRequestPolicy {
    static String requiredFunction(String request) {
        if (request == null) return "";
        if(request.matches("^メモ[：:].+"))return "save_memory";
        request=request.replaceFirst("^(?:保留案件|保留|案件)(?:というか|じゃなく(?:て)?|ではなく(?:て)?)[、，\\s]*", "");
        if (request.matches("(?s).*(しないで|しなくて|しない|不要|やめて|ないで|なくていい|とは言っていない).*")
                || request.matches("(?s).*(という|と言う|といった|っていう|という意味|の意味|と発話|と言った|って言った|と頼んだ).*") ) return "";
        if (request.matches("(?s).*(記憶|メモ|覚えた).*(削除して|消して|忘れて).*")
                || request.matches("(?s).*(忘れてください|忘れておいて).*") ) return "delete_memory";
        if (request.matches("(?s).*(覚えておいて|覚えといて|覚えとけ|覚えていて|覚えてね|覚えてください|記憶して|記録して|メモして|メモしといて|メモしとけ|メモ(?:として|に)(?:残して|保存して)|メモ(?:を)?残して).*")
                || request.matches("(?s).*(記憶|メモ).*(保存して).*") ) return "save_memory";
        return "";
    }

    static boolean bypassInbox(String request) {
        if(request==null||request.startsWith("__"))return false;
        boolean reclassify=request.matches("(?s).*(?:保留案件|保留|案件)(?:というか|じゃなく(?:て)?|ではなく(?:て)?).*メモ.*");
        if(!reclassify&&request.matches("(?s).*(?:受信箱[にへ]|保留案件として|案件として|保留にして).*"))return false;
        if(!requiredFunction(request).isEmpty())return true;
        return concernsMemory(request)&&!request.matches("(?s).*(受信箱|保留案件|案件ID).*");
    }
    static boolean concernsMemory(String request) {
        return !requiredFunction(request).isEmpty()
                || (request != null && request.matches("(?s).*(記憶|メモ|覚えて|覚えた).*") );
    }
}
