package com.example.rokidkeyboardbridge;
/** Opt-in conversational rapport, distinct from quiet idle habits. */
final class FullBodyAffectionCue {
 private int requested=-1,family=-1;private long until,started;
 static boolean has(String text,String... words){if(text==null)return false;for(String word:words)if(text.contains(word))return true;return false;}
 static boolean stop(String text){if(text!=null)text=text.replaceAll("しませんか(?=[？?。！!、 ]|$)","しましょうか").replaceAll("できませんか(?=[？?。！!、 ]|$)","できますか");return has(text,"やめて","やめよう","やめます","やめてお","嫌だ","いやだ","嫌です","嫌が","触らない","触るな","普通に戻","通常に戻","会話を終了","話題を変","別の話","断り","お断り","できません","しません","無理です","普通にして","もういい","ストップ","気分じゃない","誘わない","甘えない","誘惑しない","キスしない","口づけしない");}
 static int request(String text){
  if(text==null||stop(text)||has(text,"とは","意味","について教","方法","彼女が","彼氏が","登場人物","小説","物語","料理","レストラン","釣り","魚","は何","友達","同僚","上司","部下","メール","文面","例文"))return -1;
  if(EmbodiedScenePolicy.kissWords(text))return 4+KissGesturePolicy.type(text);
  if(has(text,"誘って","誘惑して","手招きして","誘う感じ","誘惑する感じ","誘う仕草","誘惑する仕草","誘惑するよう","ドキドキさせて","ドキッとさせて","色っぽくして"))return 3;
  if(has(text,"甘えて","甘えん坊","甘える感じ","そばにいて","ロキが好き","ロキ大好き","ロキかわいい","ロキ可愛い","もっと近くに","甘える仕草","甘えるよう","一緒にいて","隣にいて","好きだよ","会いたかった","手を握って","手をつないで"))return 2;
  return -1;
 }
 void clear(){requested=family=-1;until=started=0;}
 void user(String text,long now){
  if(stop(text)||has(text,"天気","予定","ニュース","ナビ","目的地","経路","会議","検索","教えて","調べて","とは","意味","方法","文面","例文","料理","レストラン","釣り","魚","探して","は何")){clear();return;}int next=request(text);if(next<0&&requested>=4&&now<until&&has(text,"フレンチ","ディープ","突然","めろめろ","メロメロ"))next=4+KissGesturePolicy.type(text);if(next<0&&requested>=4&&now<until&&KissGesturePolicy.strongFollowup(text))next=4+KissGesturePolicy.stronger(requested-4);if(next<0&&requested>=4&&now<until&&KissGesturePolicy.normalFollowup(text))next=4+KissGesturePolicy.base(requested-4);
  if(next>=0){requested=next;family=-1;until=now+90000;return;}
  if(now<until&&has(text,"もっと","そのまま","続けて","いいね","かわいいね","可愛いね","ありがとう","うん","おいで"))return;
  clear();
 }
 static boolean negative(FullBodyPoseCatalog.Kind kind){return kind==FullBodyPoseCatalog.Kind.REFUSE||kind==FullBodyPoseCatalog.Kind.ANGER||kind==FullBodyPoseCatalog.Kind.SAD||kind==FullBodyPoseCatalog.Kind.CRY||kind==FullBodyPoseCatalog.Kind.WORRY||kind==FullBodyPoseCatalog.Kind.STRAIN||kind==FullBodyPoseCatalog.Kind.TIRED||kind==FullBodyPoseCatalog.Kind.BREATH||kind==FullBodyPoseCatalog.Kind.PANT||kind==FullBodyPoseCatalog.Kind.WALK;}
 void answer(String text,FullBodyPoseCatalog.Kind kind,long now){
  if(now>=until||stop(text)||negative(kind)){clear();return;}
  if(kind==FullBodyPoseCatalog.Kind.SURPRISE&&!(KissGesturePolicy.base(requested-4)==2&&requested>=4&&has(text,"キス","口づけ","口付け"))){clear();return;}
  if(requested<0)return;
  boolean warm=requested>=4&&EmbodiedScenePolicy.kissWords(text)||kind==FullBodyPoseCatalog.Kind.SHY||kind==FullBodyPoseCatalog.Kind.JOY||kind==FullBodyPoseCatalog.Kind.CONFIDENT
   ||has(text,"そばに","そばで","一緒に","甘え","手を差し","こちらへ","こっちへ","おいで","近くに","手招き","キス","口づけ","口付け");
  if(warm&&family!=requested){family=requested;started=now;}
 }
 int clip(long now,boolean seated){return family>=2&&now>=started&&now<until&&(now-started)%18000<4600?FullBodyEverydayGesture.clip(seated,family):-1;}
 long local(long now){return Math.max(0,now-started)%18000;}
}
