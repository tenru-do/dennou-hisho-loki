package com.example.rokidkeyboardbridge;
public final class ImageReferencePolicyTest {
    public static void main(String[] args){long at=1700000000000L,now=at+300000;
        for(String text:new String[]{"その予約は何時？","さっきの写真の病院は？","それはどこ？","予約はいつ？","その商品を調べて","写っている値段を教えて"})if(!ImageReferencePolicy.followup(text,at,now))throw new AssertionError(text);
        for(String text:new String[]{"今日の予定を教えて","明日の予定は？","別の話だけどそれは？","カレンダーを確認して","眠気がくる"})if(ImageReferencePolicy.followup(text,at,now))throw new AssertionError(text);
        if(ImageReferencePolicy.fresh(at,at+ImageReferencePolicy.TTL_MS)||ImageReferencePolicy.fresh(at,at-1)||ImageReferencePolicy.fresh(0,now))throw new AssertionError("expiry/clock/no reference");
        if(!ImageReferencePolicy.fresh(at,at+ImageReferencePolicy.TTL_MS-1))throw new AssertionError("fresh boundary");
        if(ImageReferencePolicy.followup("その予約は？",at,at+ImageReferencePolicy.TTL_MS))throw new AssertionError("stale followup");
        System.out.println("PASS image antecedents, calendar/small-talk isolation, 2-hour expiry and clock boundaries");
    }
}
