package com.example.rokidkeyboardbridge;
/** Fictional scene state, separate from transcript memory, devices and short feedback. */
final class VirtualContactPolicy {
 static final int NONE=0,OFFER=1,HAND=2,HUG=3,PAT=4;
 static final int HAND_PART=1,HEAD=2,SHOULDER=3,CHEEK=4,LOKI=1,USER=2,BOTH=3;
 static final int REQUEST=1,ACCEPT=2,STROKE=3,RELEASE=4,EXPIRE=5;
 static final long TTL=2L*60*60*1000,OFFER_TTL=120000;
 static final String INSTRUCTION="これはユーザーが選んだ仮想空間内の触れ合いの状態である。現実の肉体や装置で接触が起きたという意味ではない。この場面では仮想の動作として自然に受け答えし、毎回『肉体がないのでできない』という説明で会話を切らない。ユーザー・ロキのどちらがどこに触れているか、手を差し出して待っているだけか、手を取って保持しているかを区別する。保存ログの昔の描写よりこの現在状態と今回の発言を優先する。HANDなら、離す指示がないまま毎回答で手を差し出す段階へ戻さない。OFFERでofferOnly=trueなら、まだユーザーが手を取ったと決めつけず待つ。通常の会話中に姿勢が落ち着いても成立した関係を忘れない。NONEなら過去の接触を勝手に復活させない。suspendedは実務中に表示を控えているだけで、離した意味ではない。離す・やめる・拒否は尊重する。台詞は現在の質問に直接、自然な短い返答にし、長い動作描写を繰り返さない。身体に関する別の質問・実務・検索を接触の依頼に読み替えない。必要な境界や意思表示は保つ。";
 static final class Frame {
  static final Frame EMPTY=new Frame(0,1,1,0,0,0,0,0,false,false);
  final int kind,part,actor,event,direction,pending;final long at,activity,revision;final boolean suspended,offerOnly;
  Frame(int k,int p,int a,int e,int dir,int next,long time,long v,boolean quiet,boolean wait){this(k,p,a,e,dir,next,time,time,v,quiet,wait);}
  Frame(int k,int p,int a,int e,int dir,int next,long time,long active,long v,boolean quiet,boolean wait){activity=active;kind=k;part=p;actor=a;event=e;direction=dir;pending=next;at=time;revision=v;suspended=quiet;offerOnly=wait;}
  boolean visible(){return kind!=NONE&&!suspended;}
 }
 private int kind,part=HAND_PART,actor=LOKI,event,direction,pending=HAND,priorKind,priorPart=HAND_PART,priorActor=LOKI;
 private Frame cached=Frame.EMPTY;private long at,activity,revision;private boolean suspended,offerOnly;private String request="";
 static String clean(String s){return s==null?"":s.replaceAll("[\\s　]","").replaceFirst("^(?:仮想空間で|仮想で|電脳上で|この空間で)","").replaceFirst("^ロキ[、,]","").replace("、","");}
 static boolean has(String s,String...words){for(String word:words)if(s.contains(word))return true;return false;}
 static boolean excluded(String s){return has(s,"とは","意味","方法","例文","小説","物語","登場人物","彼女が","彼氏が","友達が","同僚が","仕様","実装","設定","写真","画像","動画","触診","医師","医療");}
 static boolean release(String text){String s=clean(text);if(excluded(s))return false;s=s.replaceAll("離さない(?:で)?|放さない(?:で)?|やめない(?:で)?|離したくない|離して(?:い)?ない","");return has(s,"手を離す","手を離して","離して","離そう","放して","接触を終了","触れ合いを終了","触れ合いをやめ","触らないで","触るな","触れないで","やめて","もういい","ストップ","普通に戻","通常に戻","手を握らないで","手をつながないで","抱きしめないで","撫でないで","なでないで","手をつないでない","手をつないでいない","手を握ってない","手を握っていない");}
 static boolean task(String s){s=s.replaceAll("検索(?:や外部操作)?(?:は)?(?:しないで|しない|不要)|調べ(?:なくていい|ないで)","");return has(s,"天気","予定","ニュース","ナビ","目的地","経路","会議","検索","調べて","教えて","メール","計算");}
 static boolean contactWords(String s){return has(s,"手を","その手","握り返","抱き","抱いて","撫で","なで","肩に触","頬に触","触れ合い","接触","そのまま","離さない","やめない");}
 private void change(int next,int target,int who,int why,long now){kind=next;part=target;actor=who;event=why;at=activity=now;direction=0;suspended=false;revision++;}
 void discard(){kind=NONE;event=0;at=activity=0;direction=0;suspended=false;offerOnly=false;request="";revision++;}
 void clear(long now){change(NONE,HAND_PART,LOKI,RELEASE,now);offerOnly=false;request="";priorKind=NONE;}
 private void expire(long now){if(kind!=NONE&&(at<=0||now<at||now-(kind==OFFER?at:activity)>(kind==OFFER?OFFER_TTL:TTL))){change(NONE,HAND_PART,LOKI,EXPIRE,now);request="";}}
 Frame frame(long now){expire(now);if(cached.revision!=revision)cached=new Frame(kind,part,actor,event,direction,pending,at,activity,revision,suspended,offerOnly);return cached;}
 long revision(){return revision;}
 void restore(int k,int p,int who,int why,int dir,int next,long time,boolean quiet,boolean wait,long now){restore(k,p,who,why,dir,next,time,time,quiet,wait,now);}
 void restore(int k,int p,int who,int why,int dir,int next,long time,long active,boolean quiet,boolean wait,long now){
  if(k<0||k>4||p<1||p>4||who<1||who>3||why<0||why>5||dir<0||dir>4||next<2||next>4||time<=0||time>now||active<time||active>now||now-(k==OFFER?time:active)>(k==OFFER?OFFER_TTL:TTL)){discard();return;}
  kind=k;part=p;actor=who;event=why;direction=dir;pending=next;at=time;activity=active;suspended=quiet;offerOnly=wait;revision++;request="";
 }
 private void propose(int target,int targetPart,int who,boolean wait,String text,long now){
  if(kind==target&&part==targetPart&&actor==who&&!suspended){activity=now;revision++;request=text;return;}
  priorKind=kind==OFFER?NONE:kind;priorPart=part;priorActor=actor;pending=target;offerOnly=wait;request=text;change(OFFER,targetPart,who,REQUEST,now);
 }
 void user(String text,long now){
  expire(now);String s=clean(text);request=s;
  if(ConversationTopicBoundary.reset(text)&&!has(s,"手をつないだまま","手を握ったまま","抱きしめたまま")||release(s)){clear(now);return;}
  if(excluded(s)){if(kind!=NONE){suspended=true;revision++;}return;}
  if(s.matches("(?s).*(?:つないで(?:い)?る|握って(?:い)?る|繋いで(?:い)?る|抱きしめて(?:い)?る|撫でて(?:い)?る|なでて(?:い)?る)(?:の|んですか|か|かな|よね|ね|っけ)?[？?。]*$")||has(s,"今の状態","どんな状態","つないでいる？","つないでいる?","握っている？","握っている?","抱きしめている？","抱きしめている?","撫でている？","撫でている?","つないだっけ","握ったっけ")){if(kind!=NONE){activity=now;revision++;}return;}
  if(s.matches("(?:ロキの|君の|あなたの|その)?手を(?:取る|握る|つなぐ|繋ぐ|取った|握った|つないだ|繋いだ)(?:よ|ね|[。！!])*" )||s.matches("(?:その手を)?握り返す(?:よ|ね|[。！!])*")){change(HAND,HAND_PART,BOTH,ACCEPT,now);return;}
  if(has(s,"手を差し出して","手を出して","手を差し伸べて")){propose(HAND,HAND_PART,BOTH,true,s,now);return;}
  if(has(s,"手を握って","手をつないで","手を繋いで","手を取って","手をつなごう","手を繋ごう","手を握ろう","手をつなぎたい","手つないで","手つなごう","手握って")){if(has(s,"握っていない","つないでいない","つないでない","握ってない"))return;propose(HAND,HAND_PART,BOTH,false,s,now);return;}
  if(s.matches("(?:ロキを|君を|あなたを)?(?:抱きしめる|抱きしめた|抱きつく)(?:よ|ね|[。！!])*")){change(HUG,SHOULDER,USER,ACCEPT,now);return;}
  if(has(s,"抱きしめて","抱いていて","ハグして","ハグしよう")){propose(HUG,SHOULDER,LOKI,false,s,now);return;}
  int target=has(s,"頬","ほお")?CHEEK:has(s,"肩")?SHOULDER:has(s,"手")?HAND_PART:HEAD;
  if(s.matches("(?:ロキの|君の|あなたの)?(?:頭|肩|頬|ほお|手)を(?:撫でる|なでる|撫でた|なでた)(?:よ|ね|[。！!])*")){change(PAT,target,USER,STROKE,now);return;}
  if(has(s,"頭を撫でて","頭をなでて","肩を撫でて","肩をなでて","頬を撫でて","頬をなでて","手を撫でて","手をなでて")){propose(PAT,target,LOKI,false,s,now);return;}
  if(kind!=NONE){suspended=task(s)||has(s,"キス","口づけ");if(!suspended){activity=now;}revision++;}
 }
 void answer(String prompt,String reply,long now){
  expire(now);if(kind==NONE||!clean(prompt).equals(request)||reply==null)return;
  String a=clean(reply);boolean refused=a.matches("(?s).*(?:握れません|つなげません|抱きしめられません|撫でられません|お断り|触れないで|触らないで|接触は控え|手を離します|手をほどきます).*" );
  if(refused){if(kind==OFFER){change(priorKind,priorPart,priorActor,RELEASE,now);offerOnly=false;}else if(has(a,"手を離します","手をほどきます","触れないで","触らないで"))clear(now);return;}
  if(kind==OFFER&&!offerOnly){
   boolean accepts=pending==HAND&&has(a,"手を握","手をつな","手を繋","握り返","つなぎま","繋ぎま")||pending==HUG&&has(a,"抱きしめ","抱いて")||pending==PAT&&has(a,"撫で","なで");
   if(accepts)change(pending,part,actor,ACCEPT,now);
  }
 }
 boolean gesture(int dir,long now){expire(now);if(kind==NONE||suspended||dir<0||dir>4)return false;
  if(kind==OFFER){int next=pending;change(next,part,next==HAND?BOTH:actor,ACCEPT,now);offerOnly=false;}
  else{event=dir==0?ACCEPT:STROKE;at=activity=now;revision++;}direction=dir;return true;
 }
 boolean context(long now){expire(now);return kind!=NONE||at>0&&now>=at&&now-at<900000&&(event==RELEASE||event==EXPIRE);}
}
