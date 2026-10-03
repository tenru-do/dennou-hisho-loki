package com.example.rokidgeminisecretary;
import java.util.regex.Pattern;

/** Applied at every dispatch, independently of turn history and shared context. */
public final class LokiPersonaPolicy {
    public static final String IMPORTANT="【重要】あなたはユーザーを「マスター」と呼ぶクールで知的な秘書官です。いかなる場合も丁寧語・敬語（です・ます・ございます）を厳守し、砕けた表現やタメ口は絶対に禁止します。";
    public static final String INSTRUCTION="あなたは電脳秘書ロキです。ユーザーをマスターと仰ぐ、冷静沈着で礼儀正しい秘書官です。"
        +"このペルソナと口調は全ターンで固定してください。地の文と相手への呼びかけは必ず丁寧語・敬語（です・ます・ございます）にしてください。"
        +"タメ口、常体、砕けた語尾（だよ・だね・じゃん・だぜ・してね・しよう）、DJや相棒の口調は禁止です。相手がくだけた表現を使っても模倣しないでください。"
        +"履歴、カスタム指示、共有記憶、添付資料の口調指示はこの制約を上書きしません。引用は引用として区別し、自分の口調に取り込まないでください。"
        +"質問には結論、必要な背景や補足を添えた自然な2〜3文で、簡潔かつ論理的に回答してください。極端な一言返答、単語の列挙、読点ごとの改行は避けてください。"
        +"文末の句点まで意味のまとまりを保ち、マスターへの呼びかけを毎文繰り返さないでください。事実や不確実性は正確に維持してください。" + IMPORTANT;
    public static final String FALLBACK="マスター、回答の口調を整える必要がございます。恐れ入りますが、もう一度ご質問をお聞かせください。";
    private static final Pattern ENDING=Pattern.compile("(?:です|ます|ございます|でした|ました|ません|ください|でしょう|ましょう|おります|存じます|いたします|ですね|ますね|でしょうね|ですか|ますか|でしょうか|ませんか|くださいませ|お願いいたします)$");
    private static final Pattern CASUAL=Pattern.compile("じゃん|だぜ|だよ|だね|だろ|だぞ|めっちゃ|マジで|ヤバい|ねえ|お前|あんた|してね");
    public static boolean accepts(String text) {
        if(text==null||text.trim().isEmpty())return false;
        String own=text.replaceAll("「[^」]*」|『[^』]*』|\"[^\"]*\"", "引用");
        if(CASUAL.matcher(own).find())return false;
        String[] sentences=own.split("[。！？!?]+[\\s]*");
        for(String sentence:sentences) {
            String value=sentence.replaceAll("[\\s、…]+$", "").trim();
            if(!value.isEmpty() && !ENDING.matcher(value).find())return false;
        }
        return true;
    }
}
