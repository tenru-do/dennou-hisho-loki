package com.example.rokidkeyboardbridge;
/** Visual-only, event-driven rest reactions. Never changes body pose or starts a network request. */
final class RestConversationReaction {
 static final int NONE=0,HAPPY=1,THINK=2,SURPRISE=3,CARE=4,ACK=5,TOUCH=6;
 private boolean priority;private int kind,mode=-1,direction;private long at=-1,until=-1;private final long[] used=new long[7];private final boolean[] seen=new boolean[7];
 private void offer(int next,long now,boolean direct,int sign){if(next<=0||next>6)return;long gap=direct?260:8000;if(seen[next]&&now>=used[next]&&now-used[next]<gap){if(!direct){kind=next;until=now+3000;direction=sign;if(at<0)at=now-2200;}return;}kind=next;at=now;until=now+3600;direction=sign;seen[next]=true;used[next]=now;}
 void clear(){priority=false;kind=NONE;at=until=-1;}
 void user(String text,long now){String t=RestPoseIntent.normalize(text);if(t.length()>120||t.matches("(?s).*(とは|意味|について|例文|引用|と言った|と言って|かわいくない|可愛くない|ありがとうと言|[？?]).*"))return;if(EmbodiedScenePolicy.has(t,"ありがとう","助かった","かわいい","可愛い","いいね","お疲れさま","お疲れ様"))offer(HAPPY,now,false,0);}
 void answer(FullBodyPoseCatalog.Kind value,long now){if(value==null)return;if(value==FullBodyPoseCatalog.Kind.RECOVERY){clear();offer(ACK,now,false,0);return;}int next=ConversationVisualCatalog.restReaction(value);if(ConversationVisualCatalog.priority(value)){clear();priority=true;seen[next]=false;}else if(next!=ACK)priority=false;if(next!=ACK||kind(now)==NONE||kind(now)==ACK)offer(next,now,false,next==TOUCH?1:0);}
 void mode(int next,long now,boolean visualTurn){if(next==mode)return;int previousMode=mode;mode=next;if(previousMode==2&&next==0&&!priority&&!visualTurn)offer(ACK,now,true,0);if(next==1&&!visualTurn&&(kind(now)==NONE||kind(now)==ACK))offer(THINK,now,false,0);}
 void touch(long now,int direction,boolean contact){if(priority&&kind(now)!=NONE)return;offer(contact?TOUCH:ACK,now,true,direction);}
 int kind(long now){return at>=0&&now>=at&&now<until?kind:NONE;}
 long age(long now){return at<0?0:Math.max(0,now-at);}int direction(){return direction;}
 int face(int manual,boolean pinned,long now){if(pinned&&!(priority&&kind(now)!=NONE))return manual;switch(kind(now)){case HAPPY:return 6;case THINK:return 7;case SURPRISE:return 8;case CARE:return 9;case TOUCH:return 5;default:return manual;}}
 boolean moving(long now){return kind(now)!=NONE&&age(now)<2200;}
}
