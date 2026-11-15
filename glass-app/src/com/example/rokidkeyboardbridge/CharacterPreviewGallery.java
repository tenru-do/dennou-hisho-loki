package com.example.rokidkeyboardbridge;
import android.app.*;import android.content.*;import android.graphics.*;import android.view.*;import android.widget.*;import java.io.*;import java.util.zip.*;import org.json.*;
/** User-opened local gallery; same 480x640 camera for every cell, selected cell animates only. */
class CharacterPreviewGallery extends View {
 static final String[] LABELS={"立位","脚をそろえた座位","脚組み","寝姿","立位の手振り","座位の手振り"};
 private final Bitmap[] pictures=new Bitmap[4];private final Paint paint=new Paint(3),ink=new Paint(3);private final RectF rect=new RectF();private final FullBodyMascotView body;private final ApprovedQuietPoseRenderer quiet;private final FullBodyGestureRenderer gestures;
 private int selected,generation;private boolean closed;private final long ticket=ConversationStageMotion.epoch(),started=android.os.SystemClock.uptimeMillis();
 CharacterPreviewGallery(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);setFocusable(true);body=new FullBodyMascotView(c);body.layout(0,0,480,640);quiet=new ApprovedQuietPoseRenderer(this);quiet.natural(true);gestures=new FullBodyGestureRenderer(this);load();}
 private void load(){final int token=generation;new Thread(()->{Bitmap[] decoded=new Bitmap[4];try(ZipFile apk=new ZipFile(getContext().getApplicationInfo().sourceDir)){
  decoded[0]=FullBodyRealisticAssets.decode(apk,4,1);decoded[1]=FullBodyRealisticAssets.decode(apk,168,1);
  for(int i=2;i<4;i++)try(InputStream in=getContext().getAssets().open(ApprovedQuietPosePolicy.ASSETS[i==2?0:4])){decoded[i]=BitmapFactory.decodeStream(in);}
 }catch(Exception|OutOfMemoryError ignored){}post(()->{if(closed||token!=generation){recycle(decoded);return;}System.arraycopy(decoded,0,pictures,0,4);invalidate();});},"LokiPreviewGallery").start();gestures.request(0);gestures.request(1);}
 static void recycle(Bitmap[] frames){for(Bitmap b:frames)if(b!=null&&!b.isRecycled())b.recycle();}
 boolean ready(){for(Bitmap b:pictures)if(b==null)return false;return gestures.ready(0)&&gestures.ready(1);}
 void next(){if(closed)return;selected=(selected+1)%6;invalidate();}int selected(){return selected;}
 @Override protected void onDraw(Canvas c){c.drawColor(Color.BLACK);long now=android.os.SystemClock.uptimeMillis();boolean running=!closed&&ConversationStageMotion.current(ticket);int cw=getWidth()/2,ch=Math.max(1,getHeight()/3);ink.setTextSize(14*getResources().getDisplayMetrics().scaledDensity);
  for(int i=0;i<6;i++){int x=i%2*cw,y=i/2*ch;c.save();c.clipRect(x,y,x+cw,y+ch);ink.setColor(i==selected?Color.CYAN:Color.LTGRAY);c.drawText((i+1)+" "+LABELS[i],x+6,y+18,ink);float scale=Math.min(cw/480f,(ch-24)/640f);c.translate(x+(cw-480*scale)/2,y+23);c.scale(scale,scale);c.clipRect(0,0,480,640);
   long time=running&&i==selected?now-started:1100;
   if(i<2&&pictures[i]!=null)body.renderInspection(c,pictures[i],i==0?4:168,time,false,running&&i==selected,null,-1,0);
   else if(i<4&&pictures[i]!=null)quiet.renderPreview(c,pictures[i],i==2?0:4,480,640,time,running&&i==selected);
   else if(i>=4){if(gestures.ready(i-4))gestures.draw(c,480,640,i-4,time%4400,time+10000);}
   c.restore();if(i==selected){ink.setColor(Color.CYAN);ink.setStyle(Paint.Style.STROKE);ink.setStrokeWidth(2);rect.set(x+1,y+1,x+cw-1,y+ch-1);c.drawRect(rect,ink);ink.setStyle(Paint.Style.FILL);}
  }if(running&&isShown())postInvalidateDelayed(150);
 }
 @Override public boolean onTouchEvent(android.view.MotionEvent e){if(e.getAction()==MotionEvent.ACTION_UP){selected=Math.min(5,(int)(e.getY()/Math.max(1,getHeight()/3))*2+(e.getX()>=getWidth()/2?1:0));performClick();invalidate();return true;}return true;}
 @Override public boolean performClick(){super.performClick();return true;}
 void close(){if(closed)return;closed=true;generation++;body.releaseInspection();quiet.release();gestures.release();recycle(pictures);}
 @Override protected void onDetachedFromWindow(){close();super.onDetachedFromWindow();}
 private static String readSmall(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] block=new byte[4096];for(int n;(n=in.read(block))>=0;)out.write(block,0,n);return out.toString("UTF-8");}
 static boolean report(Context c,int selected){if(selected<0||selected>=LABELS.length)return false;try{File file=new File(c.getFilesDir(),"character-preview-review.json");JSONArray list=new JSONArray();if(file.isFile())try(FileInputStream in=new FileInputStream(file)){if(file.length()<65536)list=new JSONArray(readSmall(in));}JSONArray next=new JSONArray();for(int i=Math.max(0,list.length()-49);i<list.length();i++)next.put(list.get(i));next.put(new JSONObject().put("item",selected).put("label",LABELS[selected]).put("version",751).put("at",System.currentTimeMillis()));try(FileOutputStream out=new FileOutputStream(file)){out.write(next.toString().getBytes("UTF-8"));}return true;}catch(Exception e){return false;}}
 static void show(Activity a){CharacterPreviewGallery gallery=new CharacterPreviewGallery(a);ScrollView scroll=new ScrollView(a);scroll.addView(gallery,new ScrollView.LayoutParams(-1,Math.round(720*a.getResources().getDisplayMetrics().density)));AlertDialog dialog=new LokiAlertDialogBuilder(a).setTitle("画像・動作一覧（同じ距離）").setView(scroll).setPositiveButton("閉じる",null).setNeutralButton("次の画像",null).setNegativeButton("気になる画像を記録",null).create();dialog.setOnDismissListener(d->gallery.close());dialog.setOnShowListener(d->{dialog.getButton(-3).setOnClickListener(v->{gallery.next();scroll.smoothScrollTo(0,(gallery.selected()/2)*Math.max(1,gallery.getHeight()/3));});dialog.getButton(-2).setOnClickListener(v->Toast.makeText(a,report(a,gallery.selected())?LABELS[gallery.selected()]+"を端末内に記録しました":"記録できませんでした",Toast.LENGTH_SHORT).show());});dialog.show();}
}
