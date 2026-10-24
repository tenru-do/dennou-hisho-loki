package com.example.rokidkeyboardbridge;
/** Only recent explicit physical contact cues affect button feedback. */
final class ButtonReactionContext {
    static boolean touching(String value){
        if(value==null)return false;
        if(value.contains("触っていない")||value.contains("触ってない")||value.contains("触れていない")||value.contains("触るな")||value.contains("触らない")||value.contains("触れない")||value.contains("やめて")||value.contains("話題を変")||value.contains("会話を終了"))return false;
        for(String cue:new String[]{"撫で","なで","肩を叩","肩をたた","手を握","手をつな","触れる","触れて","触れられ","触って","触られ","さわって","さわられ","抱きしめ","くすぐ","つつく"})if(value.contains(cue))return true;
        return false;
    }
}
