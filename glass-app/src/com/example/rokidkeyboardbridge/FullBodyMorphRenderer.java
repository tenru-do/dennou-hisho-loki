package com.example.rokidkeyboardbridge;
import android.content.Context;import android.graphics.*;import java.io.*;
/** One opaque source image is moved toward its neighbour; two silhouettes are never composited. */
final class FullBodyMorphRenderer {
 static final int COLUMNS=48,ROWS=96;static final String ASSET="loki_transition_flow_v486.bin";
 private volatile boolean loaded;private long[] keys=new long[0];private float[][] fields=new float[0][];
 private final float[] vertices=new float[(COLUMNS+1)*(ROWS+1)*2];
 private long cachedKey=Long.MIN_VALUE;private float[] cachedField;
 synchronized void load(Context context){
  if(loaded)return;
  try(DataInputStream in=new DataInputStream(new BufferedInputStream(context.getAssets().open(ASSET)))){
   if(in.readInt()!=0x4c4b4631||in.readInt()!=COLUMNS||in.readInt()!=ROWS)throw new IOException("Morph header");int count=in.readInt();if(count<1||count>2048)throw new IOException("Morph count");
   long[] nextKeys=new long[count];float[][] nextFields=new float[count][];
   for(int i=0;i<count;i++){int from=in.readInt(),to=in.readInt();nextKeys[i]=key(from,to);float[] v=new float[vertices.length];for(int j=0;j<v.length;j++){v[j]=in.readFloat();if(!Float.isFinite(v[j])||Math.abs(v[j])>.3f)throw new IOException("Morph displacement");}nextFields[i]=v;}
   if(in.read()!=-1)throw new IOException("Morph trailing data");keys=nextKeys;fields=nextFields;
  }catch(IOException e){android.util.Log.w("LokiFullBody","Single-image morph data unavailable",e);}finally{loaded=true;}
 }
 static long key(int from,int to){return ((long)from<<32)|(to&0xffffffffL);}
 static int source(float amount){return amount<.5f?0:1;}
 static float weight(float amount){return Math.max(0,Math.min(.5f,amount<.5f?amount:1-amount));}
 float[] field(int from,int to){if(!loaded)return null;long k=key(from,to);if(k==cachedKey)return cachedField;cachedKey=k;cachedField=null;for(int i=0;i<keys.length;i++)if(keys[i]==k){cachedField=fields[i];break;}return cachedField;}
 void draw(Canvas canvas,Bitmap bitmap,Paint paint,float[] flow,float amount,int viewWidth,int viewHeight,float scale,float[] breath,boolean breathing){
  int w=bitmap.getWidth(),h=bitmap.getHeight(),at=0;
  for(int row=0;row<=ROWS;row++)for(int col=0;col<=COLUMNS;col++){
   float x=w*col/(float)COLUMNS,y=h*row/(float)ROWS;
   if(breathing){float cx=col*FullBodyBreathPolicy.COLUMNS/(float)COLUMNS,ry=row*FullBodyBreathPolicy.ROWS/(float)ROWS;int c=Math.min(FullBodyBreathPolicy.COLUMNS-1,(int)cx),r=Math.min(FullBodyBreathPolicy.ROWS-1,(int)ry);float u=cx-c,v=ry-r;int a=(r*(FullBodyBreathPolicy.COLUMNS+1)+c)*2,b=a+2,d=a+(FullBodyBreathPolicy.COLUMNS+1)*2,e=d+2;
    x=(breath[a]*(1-u)+breath[b]*u)*(1-v)+(breath[d]*(1-u)+breath[e]*u)*v;
    y=(breath[a+1]*(1-u)+breath[b+1]*u)*(1-v)+(breath[d+1]*(1-u)+breath[e+1]*u)*v;
   }
   vertices[at]=x+flow[at]*viewWidth/scale*amount;vertices[at+1]=y+flow[at+1]*viewHeight/scale*amount;at+=2;
  }
  canvas.drawBitmapMesh(bitmap,COLUMNS,ROWS,vertices,0,null,0,paint);
 }
}
