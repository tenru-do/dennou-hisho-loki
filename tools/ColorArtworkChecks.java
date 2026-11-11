package com.example.rokidkeyboardbridge;
import android.content.*;import android.graphics.*;import android.os.*;import android.view.*;import java.lang.reflect.*;import java.io.*;import java.util.concurrent.atomic.*;
public final class ColorArtworkChecks {
 static int n;static void ok(boolean b,String why){n++;if(!b)throw new AssertionError("color artwork: "+why);}
 static void ui(Runnable r)throws Exception{ApprovedQuietPoseChecks.ui(r);}
 // Inspect what is actually visible on the black glasses background. Transparent
 // RGB and nearly invisible alpha fringes are not a person's crop boundary.
 static boolean visibleOnBlack(int pixel){int channel=Math.max(Color.red(pixel),Math.max(Color.green(pixel),Color.blue(pixel)));return Color.alpha(pixel)*channel>=24*255;}
 static int[] visibleBounds(Bitmap bitmap){int w=bitmap.getWidth(),h=bitmap.getHeight(),left=w,top=h,right=-1,bottom=-1,count=0,transparent=0;int[] pixels=new int[w*h];bitmap.getPixels(pixels,0,w,0,0,w,h);for(int i=0;i<pixels.length;i++){int pixel=pixels[i];if(Color.alpha(pixel)<16)transparent++;if(!visibleOnBlack(pixel))continue;int x=i%w,y=i/w;left=Math.min(left,x);top=Math.min(top,y);right=Math.max(right,x);bottom=Math.max(bottom,y);count++;}return new int[]{left,top,right,bottom,count,transparent};}
 static boolean completeSourceCrop(int[] bounds,int width,int height){return bounds[4]>0&&bounds[0]>0&&bounds[1]>0&&bounds[2]<width-1&&bounds[3]<height-1;}
 static void realisticTextureChecks(){
  Bitmap sample=Bitmap.createBitmap(120,120,Bitmap.Config.ARGB_8888);sample.eraseColor(0xffb58a6b);
  ok(FullBodyFaceAnimator.naturalColor(sample),"natural warm skin detected");
  ok(!FullBodyRealisticAssets.transparentArtwork(sample),"opaque color is not an alpha cutout");
  sample.setPixel(0,0,0x00112233);
  Bitmap portrait=FullBodyPortraitInk.render(sample);
  ok(portrait!=sample&&portrait.sameAs(sample),"portrait conversion preserves natural color and alpha without aliasing input");
  sample.recycle();ok(!portrait.isRecycled()&&portrait.getPixel(30,30)==0xffb58a6b,"portrait survives caller recycling source");
  portrait.recycle();sample=Bitmap.createBitmap(120,120,Bitmap.Config.ARGB_8888);sample.eraseColor(0xffb58a6b);
  Canvas sourceCanvas=new Canvas(sample);Paint ink=new Paint();ink.setColor(0xff643c3c);sourceCanvas.drawRect(49,68,71,73,ink);
  Bitmap animated=sample.copy(Bitmap.Config.ARGB_8888,true);FullBodyFaceAnimator face=new FullBodyFaceAnimator();face.prepare(sample);
  face.drawFeatures(new Canvas(animated),sample,new float[]{120,120,40,35,8,0,3,1,80,35,8,0,3,1,60,70,20,0,2},4600,true,true);face.clear();
  int[] before=new int[14400],after=new int[14400];sample.getPixels(before,0,120,0,0,120,120);animated.getPixels(after,0,120,0,0,120,120);int changed=0,minRed=255;
  for(int i=0;i<after.length;i++){minRed=Math.min(minRed,Color.red(after[i]));if(before[i]!=after[i])changed++;}
  ok(minRed>=100,"realistic motion adds no synthetic black over skin");ok(changed>0,"realistic texture actually moves");animated.recycle();
  sample.eraseColor(0xff888888);ok(!FullBodyFaceAnimator.naturalColor(sample),"monochrome stays on original path");
  sample.eraseColor(0x00b58a6b);ok(!FullBodyFaceAnimator.naturalColor(sample),"transparent RGB is not skin");ok(FullBodyRealisticAssets.transparentArtwork(sample),"alpha cutout recognized");sample.recycle();
 }
 public static int run(Context output,Context assets)throws Exception{
  n=0;realisticTextureChecks();Method measure=FullBodyGestureRenderer.class.getDeclaredMethod("measure",Bitmap.class);measure.setAccessible(true);
  Bitmap synthetic=Bitmap.createBitmap(120,120,Bitmap.Config.ARGB_8888);Canvas test=new Canvas(synthetic);Paint paint=new Paint();paint.setColor(Color.BLACK);test.drawRect(40,5,60,110,paint);paint.setColor(0xffa88268);test.drawRect(43,20,57,35,paint);synthetic.setPixel(0,0,0x00ffffff);
  float[] silhouette=(float[])measure.invoke(null,synthetic);ok(silhouette[2]==5,"black hair retained by alpha");ok(silhouette[1]==109,"black shoes retained by alpha");ok(silhouette[4]==40&&silhouette[5]==59,"transparent RGB ignored");
  ok(!visibleOnBlack(0x00ffffff)&&!visibleOnBlack(0x0100ffff),"invisible RGB does not become a displayed figure");ok(visibleOnBlack(0xff303030)&&visibleOnBlack(0xffb58a6b),"visible dark hair and skin remain part of the displayed figure");
  ok(completeSourceCrop(visibleBounds(synthetic),120,120),"uncut synthetic figure has a visible source margin");Bitmap cut=Bitmap.createBitmap(synthetic,0,20,120,100);ok(!completeSourceCrop(visibleBounds(cut),120,100),"a genuine head cut at the source boundary is rejected");cut.recycle();synthetic.recycle();
  AtomicReference<FullBodyGestureRenderer> ref=new AtomicReference<>();ui(()->ref.set(new FullBodyGestureRenderer(new View(assets){public boolean post(Runnable r){return new Handler(Looper.getMainLooper()).post(r);}})));FullBodyGestureRenderer renderer=ref.get();
  Field frames=FullBodyGestureRenderer.class.getDeclaredField("frames"),anchors=FullBodyGestureRenderer.class.getDeclaredField("anchors");frames.setAccessible(true);anchors.setAccessible(true);
  for(int clip:new int[]{0,3,7}){
   ui(()->renderer.request(clip));long deadline=SystemClock.uptimeMillis()+10000;while(!renderer.ready(clip)&&SystemClock.uptimeMillis()<deadline)Thread.sleep(30);ok(renderer.ready(clip),"sheet decoded "+clip);
   Bitmap[] pictures=((Bitmap[][])frames.get(renderer))[clip];float[] geometry=((float[][])anchors.get(renderer))[clip];ok(pictures.length==4,"four frames preserved");float min=Float.MAX_VALUE,max=0;int[][] visible=new int[4][];
   for(int i=0;i<4;i++){
    Bitmap picture=pictures[i];int w=picture.getWidth(),h=picture.getHeight();int[] bounds=visible[i]=visibleBounds(picture);float height=bounds[3]-bounds[1];min=Math.min(min,height);max=Math.max(max,height);
    ok(bounds[4]>w*h/50,"meaningful visible person clip="+clip+" frame="+i);
    ok(completeSourceCrop(bounds,w,h),"visible head/hands/shoes remain inside source crop clip="+clip+" frame="+i+" visible="+java.util.Arrays.toString(bounds));
    ok(bounds[5]>w*h/20&&FullBodyRealisticAssets.transparentArtwork(picture),"current color artwork retains a real alpha background");
    ok(geometry[i*6+2]<=bounds[1]&&geometry[i*6+1]>=bounds[3]&&geometry[i*6+4]<=bounds[0]&&geometry[i*6+5]>=bounds[2],"runtime silhouette encloses the actual visible person");
    float margin=Math.max(3,h*.04f);
    ok(bounds[1]-geometry[i*6+2]<=margin&&geometry[i*6+1]-bounds[3]<=margin,"invisible fringe cannot substantially bias the crown/floor anchors");
   }
   ok(max/min<1.08f,"actual visible frame stature stays within eight percent");
   for(int[] size:new int[][]{{512,768},{320,900},{900,320}}){
    Method placement=FullBodyGestureRenderer.class.getDeclaredMethod("fit",int.class,int.class,int.class,int.class);placement.setAccessible(true);
    for(int i=0;i<pictures.length;i++){float[] box=(float[])placement.invoke(renderer,clip,i,size[0],size[1]);float scale=box[2]/pictures[i].getWidth(),top=box[1];ok(top+visible[i][1]*scale>=CharacterViewport.headMargin(size[1])-.01f,"each projected visible crown retains the head margin");}
    Bitmap rendered=Bitmap.createBitmap(size[0],size[1],Bitmap.Config.ARGB_8888);Canvas c=new Canvas(rendered);AtomicBoolean drawn=new AtomicBoolean();ui(()->drawn.set(renderer.draw(c,size[0],size[1],clip,1200,SystemClock.uptimeMillis())));ok(drawn.get(),"gesture renders at aspect "+size[0]);int[] renderedBounds=visibleBounds(rendered);ok(renderedBounds[4]>500&&renderedBounds[1]>=CharacterViewport.headMargin(size[1])-1,"actual Canvas keeps meaningful head pixels below the upper margin");if(size[0]==512)try(FileOutputStream f=new FileOutputStream(new File(output.getFilesDir(),"color-gesture-"+clip+".png"))){rendered.compress(Bitmap.CompressFormat.PNG,100,f);}rendered.recycle();
   }
  }
  ui(renderer::release);return n;
 }
}
