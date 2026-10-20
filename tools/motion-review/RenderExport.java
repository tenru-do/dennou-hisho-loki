package com.example.rokidkeyboardbridge;
import android.content.Context;import android.graphics.*;import java.io.*;import java.util.zip.ZipFile;import org.json.*;
/** Native snapshots only; streaming decode keeps at most two full-body images. */
public final class RenderExport {
 static final String[] STAGES={"still","idle","speech","blink"};
 static final long[][] TIMES={{1000},{0,500,1000,1500},{0,110,220,330,440,550},{4500,4540,4580,4620,4660,4700,4740}};
 static void save(Bitmap b,File file)throws Exception{int ink=0;for(int y=0;y<b.getHeight();y+=3)for(int x=0;x<b.getWidth();x+=3)if((b.getPixel(x,y)&0xffffff)!=0)ink++;if(ink<3)throw new IOException("Empty native drawing: "+file.getName());try(FileOutputStream f=new FileOutputStream(file)){if(!b.compress(Bitmap.CompressFormat.PNG,100,f))throw new IOException("PNG encoding failed");}}
 static void routes(Context context,ZipFile apk,File images,JSONArray entries)throws Exception{
  String[] labels={"座位の驚き","座位の微笑み","涙の反応","考える反応","考える反応・別系列"};
  FullBodyMascotView view=new FullBodyMascotView(context);view.layout(0,0,320,480);
  for(int kind=0;kind<5;kind++){JSONArray shots=new JSONArray(),ids=new JSONArray();Bitmap source=null;int previous=-1;
   try{for(long time=0;time<=2520;time+=90){int id=FullBodyConversationMotion.pose(kind,FullBodyConversationMotion.frame(kind,time));
    if(id!=previous){view.releaseInspection();if(source!=null)source.recycle();source=FullBodyRealisticAssets.decode(apk,id,2);previous=id;ids.put(id);}
    Bitmap b=Bitmap.createBitmap(320,480,Bitmap.Config.ARGB_8888);b.eraseColor(Color.BLACK);view.renderInspection(new Canvas(b),source,id,time,true,true,null,0,0);
    String file="route-"+kind+"-"+time+".png";save(b,new File(images,file));b.recycle();shots.put(new JSONObject().put("file","frames/"+file).put("time",time).put("pose",id));
   }}finally{view.releaseInspection();if(source!=null)source.recycle();}
   JSONObject stages=new JSONObject().put("still",new JSONArray().put(shots.get(0))).put("speech",shots).put("transition",shots);
   entries.put(new JSONObject().put("key","route-"+kind).put("kind","route").put("id",kind).put("label",labels[kind]+" / 実際の反応フレーム順").put("width",320).put("height",480).put("focus",new JSONArray(new int[]{0,0,320,480})).put("sequence",ids).put("stages",stages));
  }
 }
 static JSONArray point(float x,float y)throws Exception{return new JSONArray(new float[]{x,y});}
 static JSONObject faceAnchors(float[] f,float left,float top,float scale)throws Exception{return new JSONObject().put("leftEye",point(left+f[2]*scale,top+f[3]*scale)).put("rightEye",point(left+f[8]*scale,top+f[9]*scale)).put("mouth",point(left+f[14]*scale,top+f[15]*scale)).put("visibleEyes",new JSONArray(new boolean[]{f[4]>0,f[10]>0})).put("closedEyes",new JSONArray(new boolean[]{f[7]<=0,f[13]<=0})).put("visibleMouth",f[16]>0).put("coordinateBasis","停止コマの登録位置。動いた各コマの検出結果ではありません");}
 public static void run(Context context,String destination,String selected)throws Exception{
  File dir=new File(destination);dir.mkdirs();File images=new File(dir,"frames");images.mkdirs();JSONArray entries=new JSONArray(),errors=new JSONArray();
  int version=(int)context.getPackageManager().getPackageArchiveInfo(context.getApplicationInfo().sourceDir,0).getLongVersionCode();
  try(ZipFile apk=new ZipFile(context.getApplicationInfo().sourceDir)){
   FullBodyMascotView view=new FullBodyMascotView(context);view.layout(0,0,320,480);
   for(int id=0;id<FullBodyRealisticAssets.REGIONS.length;id++){
    if(!"all".equals(selected)&&!(","+selected+",").contains(","+id+","))continue;
    Bitmap source=null,next=null;try{
     source=FullBodyRealisticAssets.decode(apk,id,1);int nextId=id+1<FullBodyRealisticAssets.REGIONS.length?id+1:id;next=FullBodyRealisticAssets.decode(apk,nextId,1);
     JSONObject item=new JSONObject().put("key","body-"+id).put("kind","body").put("id",id).put("label",id<FullBodyPoseCatalog.ALL.length?FullBodyPoseCatalog.label(id):(FullBodyMotionPolicy.isSeated(FullBodyMascotView.inspectionBasePose(id))?"座位フレーム ":"動作フレーム ")+id).put("width",320).put("height",480).put("base_pose",FullBodyMascotView.inspectionBasePose(id)).put("next",nextId).put("transition_flow",view.inspectionHasTransition(id,nextId));
     float[] f=FullBodyFaceAnimator.features(id);float scale=FullBodyScalePolicy.scale(id,source.getWidth(),source.getHeight(),320,480),left=FullBodyAnchorPolicy.left(id,source.getWidth(),320,scale,FullBodyMotionPolicy.bodyHeightFraction(FullBodyMascotView.inspectionBasePose(id))),top=480-source.getHeight()*scale,eye=Math.abs(f[8]-f[2]);
     item.put("anchors",faceAnchors(f,left,top,scale));item.put("focus",new JSONArray(new float[]{Math.max(0,left+(Math.min(f[2],f[8])-eye)*scale),Math.max(0,top+(Math.min(f[3],f[9])-eye)*scale),Math.min(320,eye*3*scale),Math.min(480,(f[15]+eye*2.8f-Math.min(f[3],f[9])+eye)*scale)}));
     JSONObject stages=new JSONObject();
     for(int s=0;s<STAGES.length;s++){JSONArray shots=new JSONArray();for(int k=0;k<TIMES[s].length;k++){long time=TIMES[s][k];Bitmap b=Bitmap.createBitmap(320,480,Bitmap.Config.ARGB_8888);b.eraseColor(Color.BLACK);view.renderInspection(new Canvas(b),source,id,time,s==2,s!=0,null,0,0);String file="body-"+id+"-"+STAGES[s]+"-"+k+".png";save(b,new File(images,file));b.recycle();shots.put(new JSONObject().put("file","frames/"+file).put("time",time));}stages.put(STAGES[s],shots);}
     JSONArray transition=new JSONArray();for(int k=0;k<5;k++){Bitmap b=Bitmap.createBitmap(320,480,Bitmap.Config.ARGB_8888);b.eraseColor(Color.BLACK);view.renderInspection(new Canvas(b),next,nextId,k*100,true,true,source,id,k/4f);String file="body-"+id+"-transition-"+k+".png";save(b,new File(images,file));b.recycle();transition.put(new JSONObject().put("file","frames/"+file).put("time",k*100));}stages.put("transition",transition);item.put("stages",stages);entries.put(item);
    }catch(Throwable e){errors.put(new JSONObject().put("key","body-"+id).put("error",e.getClass().getSimpleName()+": "+e.getMessage()));}
    finally{view.releaseInspection();if(source!=null)source.recycle();if(next!=null)next.recycle();}
   }
   if(!"routes".equals(selected)){ ApprovedFaceRenderer face=new ApprovedFaceRenderer(context);try{
    if(!face.ready())throw new IOException("Portrait atlas unavailable");
    for(int id=0;id<37;id++){Bitmap warm=Bitmap.createBitmap(192,240,Bitmap.Config.ARGB_8888);face.drawAt(new Canvas(warm),192,240,id,false,9000);warm.recycle();JSONObject item=new JSONObject().put("key","face-"+id).put("kind","face").put("id",id).put("label","顔 "+id).put("width",192).put("height",240).put("focus",new JSONArray(new int[]{25,55,145,150}));float[] af=ApprovedFaceRenderer.landmarks(id,false);BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;try(java.io.InputStream meta=context.getAssets().open("loki_face_cool_v559.png")){BitmapFactory.decodeStream(meta,null,bounds);}float cellW=bounds.outWidth/8f,cellH=bounds.outHeight/5f;float ph=Math.min(240*.94f,192*cellH/cellW),pw=ph*cellW/cellH,px=(192-pw)*.5f,py=(240-ph)*.45f;item.put("anchors",new JSONObject().put("leftEye",point(px+af[0]*pw,py+af[1]*ph)).put("rightEye",point(px+af[2]*pw,py+af[3]*ph)).put("mouth",point(px+af[4]*pw,py+af[5]*ph)).put("visibleEyes",new JSONArray(new boolean[]{true,true})).put("closedEyes",new JSONArray(FullBodyPortraitGeometry.CLOSED[id])).put("visibleMouth",true).put("coordinateBasis","停止コマの登録位置。動いた各コマの検出結果ではありません"));JSONObject stages=new JSONObject();for(int s=0;s<STAGES.length;s++){JSONArray shots=new JSONArray();for(int k=0;k<TIMES[s].length;k++){Bitmap b=Bitmap.createBitmap(192,240,Bitmap.Config.ARGB_8888);b.eraseColor(Color.BLACK);face.drawAt(new Canvas(b),192,240,id,s==2,s==3?12300+TIMES[s][k]-4500:TIMES[s][k]+10000);String file="face-"+id+"-"+STAGES[s]+"-"+k+".png";save(b,new File(images,file));b.recycle();shots.put(new JSONObject().put("file","frames/"+file).put("time",TIMES[s][k]));}stages.put(STAGES[s],shots);}item.put("stages",stages);entries.put(item);}
   }finally{face.release();}}
   if("routes".equals(selected))routes(context,apk,images,entries);
  }
  JSONObject manifest=new JSONObject().put("schema",1).put("version",version).put("renderer","Android FullBodyMascotView.drawFrame / drawSingleTransition and ApprovedFaceRenderer.drawAt").put("selection",selected).put("entries",entries).put("errors",errors);
  try(FileOutputStream out=new FileOutputStream(new File(dir,"manifest.json"))){out.write(manifest.toString().getBytes("UTF-8"));}
  if(errors.length()>0)throw new IOException("Native review failures: "+errors.length()+"; see manifest errors");
 }
}
