package com.example.rokidkeyboardbridge;
public final class ImageQuestionPolicyTest {
    public static void main(String[] args){
        for(String s:new String[]{"これを読んで","この予約票を読み取って","写真を撮って解析して","カメラで分析して","目の前のものを撮影して調べて"})if(!ImageQuestionPolicy.capture(s))throw new AssertionError(s);
        for(String s:new String[]{"昨日の写真はきれいだった","写真を撮って","この予約を家族カレンダーに登録して","眠気がくる","この件を調べて"})if(ImageQuestionPolicy.capture(s))throw new AssertionError("unrequested capture: "+s);
        for(String s:new String[]{"確定して登録","確認した予定を登録","その内容で登録して","確定して登録。","はい、確定して登録","確定して登録してください"})if(!ImageQuestionPolicy.confirm(s))throw new AssertionError(s);
        for(String s:new String[]{"登録して","わかった","その画像には確定して登録と書いてある","いや"})if(ImageQuestionPolicy.confirm(s))throw new AssertionError("unreviewed confirmation: "+s);
        if(!ImageQuestionPolicy.cancel("登録しない")||!ImageQuestionPolicy.cancel("登録をキャンセル"))throw new AssertionError("cancel");
        if(!ImageQuestionPolicy.instructions().contains("音声は含まれていない")||!ImageQuestionPolicy.instructions().contains("同一動画")&&!ImageQuestionPolicy.instructions().contains("別の添付と数えない"))throw new AssertionError("video scope missing");
        String two=ImageQuestionPolicy.instructions(2);if(!two.contains("【画像1】【画像2】")||!two.contains("関係がない画像")||!two.contains("最後の画像だけ"))throw new AssertionError("missing independent image coverage");
        if(!ImageQuestionPolicy.instructions(1).equals(ImageQuestionPolicy.instructions()))throw new AssertionError("single changed");
        try{ImageQuestionPolicy.instructions(7);throw new AssertionError("unbounded images");}catch(IllegalArgumentException expected){}
        System.out.println("PASS explicit camera intent, no casual capture, exact reviewed confirmation and cancel");
    }
}
