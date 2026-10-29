package com.example.rokidkeyboardbridge;
/** Non-graphic expressive timing shared by face expressions and gentle body approach. */
final class KissGesturePolicy {
 static final int[] LEAD={350,900,180,1000,450,100,500},HOLD={550,2200,450,1800,2600,550,2600},RETURN={700,1100,650,1200,900,650,1000};
 static int base(int type){return type>=4?type-3:type;}
 static boolean strong(int type){return type>=4&&type<=6;}
 static boolean strongWords(String text){return text!=null&&(text.contains("強め")||text.contains("激しく")||text.contains("激しい")||text.contains("動き大きく")||text.contains("動きを大きく"));}
 static boolean strongFollowup(String text){return text!=null&&text.trim().matches("^(もっと|もう少し)?(動きを|動き)?(強め|激しく|激しい|大きく)(バージョン|版)?(にして|して|でお願い|で|してね|お願いします|に)?[。！! ]*$");}
 static boolean normalFollowup(String text){return text!=null&&text.trim().matches("^(通常版|弱め|控えめ|普通の強さ|標準の強さ)(に戻して|にして|で|に|でお願い|お願いします)?[。！! ]*$");}
 static int stronger(int type){int b=base(type);return b>0?b+3:0;}
 static int type(String text){int normal=normalType(text);return strongWords(text)?stronger(normal):normal;}
 private static int normalType(String text){if(text==null)return 0;if(text.contains("ディープ")||text.contains("長め")||text.contains("じっくり")||text.contains("むさぼ")||text.contains("貪"))return 1;if(text.contains("突然")||text.contains("いきなり")||text.contains("不意"))return 2;if(text.contains("めろめろ")||text.contains("メロメロ")||text.contains("とろけ"))return 3;return 0;}
 static long duration(int type){return LEAD[type]+HOLD[type]+RETURN[type];}
 static int faceFrame(int type,long elapsed){return elapsed>=LEAD[type]&&elapsed<LEAD[type]+HOLD[type]?1:0;}
 static int bodyFrame(int type,long elapsed){long t=Math.max(0,elapsed);if(t<LEAD[type]/2)return 0;if(t<LEAD[type])return 1;if(t<LEAD[type]+HOLD[type])return 2;if(t<LEAD[type]+HOLD[type]+RETURN[type]/2)return 1;return 0;}
 static float envelope(int type,long elapsed){if(elapsed<0||elapsed>=duration(type))return 0;float s=(float)Math.sin(Math.PI*elapsed/duration(type));return s*s;}
 static float sway(int type,long elapsed){if(!strong(type))return 0;double period=type==4?520:type==5?230:760;return (float)Math.sin(elapsed*2*Math.PI/period)*envelope(type,elapsed);}
 static float approach(int type){if(strong(type))return type==4?.085f:type==5?.105f:.072f;return type==1?.035f:type==2?.055f:type==3?.023f:.012f;}
}
