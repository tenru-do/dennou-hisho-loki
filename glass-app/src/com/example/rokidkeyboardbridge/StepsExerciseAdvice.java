package com.example.rokidkeyboardbridge;

import java.time.*;
import java.util.regex.*;

/** On-demand advice from the existing step cache; no extra sensors or provider calls. */
final class StepsExerciseAdvice {
    static final class Sample {
        final String compact, source;
        final long at;
        Sample(String compact, long at, String source) {
            this.compact=compact==null?"":compact; this.at=at; this.source=source==null?"":source;
        }
    }
    static boolean requests(String text) {
        String s=text==null?"":text.replaceAll("[\\s　]", "");
        if(s.matches("(?s).*(昨日|先週|今週|先月|履歴|カレンダー|登録|検索|調べ|ニュース|痛|息切れ|めまい|病|薬|妊娠|リハビリ|しないで|いらない|不要).*"))return false;
        return s.matches("(?s).*(歩数|何歩|今日の運動|運動のアドバイス|運動アドバイス).*" )
            && s.matches("(?s).*(教えて|アドバイス|どう|どのくらい|どれくらい|何歩|見て|確認|提案|おすすめ|足り|今日の運動は).*" );
    }
    static long steps(String text) {
        Matcher m=Pattern.compile("^(?:歩数\\s+|歩:|steps:)\\s*([0-9]{1,7})(?:歩)?\\s*$").matcher(text==null?"":text.trim());
        if(!m.matches())return -1;
        long n=Long.parseLong(m.group(1));return n<=200000?n:-1;
    }
    static String reply(Sample sample,long now,ZoneId zone) {
        long count=steps(sample.compact), age=now-sample.at;
        boolean today=sample.at>0&&Instant.ofEpochMilli(sample.at).atZone(zone).toLocalDate()
                .equals(Instant.ofEpochMilli(now).atZone(zone).toLocalDate());
        if(count<0||sample.at<=0)return "歩数をまだ取得できていません。スマホのロキの歩数連携と権限を確認してください。歩数がなくても、体調のよいときに短い散歩や、座りっぱなしを区切る動きから始められます。";
        if(!today||age<0||age>900000L)return "歩数の記録が古いか、更新時刻を確認できないため、今日の運動量は判断できません。スマホとの接続と歩数連携を確認してください。";
        String time=Instant.ofEpochMilli(sample.at).atZone(zone).toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("H:mm"));
        String note="Health Connect".equals(sample.source)?"連携済みの記録です。未同期の歩数がある場合があります。":"端末で計測できた範囲です。一日全体の合計とは限りません。";
        String suggestion=count<3000?"余裕があれば、まず5分ほど気持ちよく歩くのはいかがでしょう。"
            :count<8000?"歩いた記録がありますね。余裕があれば、短い散歩をもう一回取り入れてみましょう。"
            :"よく歩いた記録がありますね。今日は歩数をさらに増やすことにこだわらず、疲れに合わせて休みましょう。";
        return "マスター、今日の連携歩数は"+count+"歩（"+time+"更新）です。"+note+"\n"+suggestion
            +"\n自転車や筋トレは歩数に十分表れないので、歩数だけで運動不足とは決めません。体調を優先し、痛みや強い疲れがあるときは休んでください。";
    }
}
