package com.example.rokidgeminisecretary;

public final class TopicNoveltyPolicyTest {
    private static int checks;
    private static void check(boolean value) { checks++; if (!value) throw new AssertionError("check " + checks); }
    public static void main(String[] args) {
        check(!TopicNoveltyPolicy.duplicate(null, "a"));
        check(!TopicNoveltyPolicy.duplicate("a", ""));
        check(TopicNoveltyPolicy.duplicate("Hello, World!", "hello world"));
        check(TopicNoveltyPolicy.duplicate("今日は晴れ。", "今日は晴れ！"));
        String script = "虹が七色に見えるのは水滴が光を分けるためです。色の境目は本当は連続していて、国によって数え方も違うんですよ。";
        check(TopicNoveltyPolicy.duplicate(script, script + "面白いですね。"));
        check(TopicNoveltyPolicy.duplicate("ところで、" + script, script));
        check(!TopicNoveltyPolicy.duplicate(script, "鉄道の線路に隙間があるのは温度で金属が伸び縮みするためです。夏と冬では同じ線路でも少し長さが変わります。"));
        check(!TopicNoveltyPolicy.duplicate("空", "空と宇宙の話をしよう"));
        check(TopicNoveltyPolicy.normalize("ＡＢＣ、猫！ 123").equals("ａｂｃ猫123"));
        check(!TopicNoveltyPolicy.repeatedFacts(0, 3, true));
        check(!TopicNoveltyPolicy.repeatedFacts(1, 4, false));
        check(TopicNoveltyPolicy.repeatedFacts(2, 4, false));
        check(!TopicNoveltyPolicy.repeatedFacts(1, 3, false));
        check(TopicNoveltyPolicy.repeatedFacts(1, 3, true));
        check(!TopicNoveltyPolicy.repeatedFacts(2, 5, false));
        System.out.println("TopicNoveltyPolicyTest: " + checks + " checks passed");
    }
}
