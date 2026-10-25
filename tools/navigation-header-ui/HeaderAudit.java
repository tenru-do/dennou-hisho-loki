package com.example.rokidkeyboardbridge;
import android.app.*;import android.os.*;import android.content.*;import android.content.res.*;import android.graphics.*;import android.view.*;import android.widget.*;import org.json.*;
public class HeaderAudit extends Instrumentation{
 int passed;void ok(boolean b,String why){if(!b)throw new AssertionError(why);passed++;}
 public void onCreate(Bundle b){super.onCreate(b);start();}
 public void onStart(){Bundle b=new Bundle();try{runOnMainSync(()->{try{
  for(float scale:new float[]{1f,1.3f,2f})for(int width:new int[]{110,190})for(boolean longText:new boolean[]{false,true}){
   Configuration conf=new Configuration(getTargetContext().getResources().getConfiguration());conf.fontScale=scale;Context c=getTargetContext().createConfigurationContext(conf);float density=c.getResources().getDisplayMetrics().density;
   TextView view=new TextView(c);NavigationHeaderStyle.apply(view);view.setText(longText?"GPS：通信更新待ち・最後に受信した位置の情報です\n位置：精度15m・更新120秒前\n現在の案内はスマホで確認してください":"09:16 67%\nGPS：更新待ち\n位置：精度15m");
   int px=Math.round(width*density);view.setLayoutParams(new android.view.ViewGroup.LayoutParams(px,-2));int height=NavigationHeaderStyle.height(view);view.measure(View.MeasureSpec.makeMeasureSpec(px,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));view.layout(0,0,px,height);
   ok(view.getLayout()!=null,"text layout absent");int lines=view.getLayout().getLineCount();ok(lines>0&&lines<=3,"unexpected status lines");ok(view.getLayout().getLineBottom(lines-1)<=height,"glyph bottom clipped height="+height+" last="+view.getLayout().getLineBottom(lines-1)+"");ok(Math.abs(view.getTextSize()-9f*density)<.1f,"system font scale expands HUD text");
   Bitmap image=Bitmap.createBitmap(px,height,Bitmap.Config.ARGB_8888);view.draw(new Canvas(image));image.recycle();
   for(int viewport:new int[]{320,640}){int map=NavigationHeaderLayout.mapHeight(Math.round(180*density),viewport,height+Math.round(4*density),Math.round(24*density),Math.round(4*density));ok(map>0,"empty map");ok(map+height+Math.round(32*density)<=viewport,"map overlaps header or controls");}
  }
 }catch(Throwable e){throw new RuntimeException(e);}});
 b.putString("safeReport",new JSONObject().put("passed",passed).put("fontScales",new JSONArray(new float[]{1f,1.3f,2f})).put("viewports",new JSONArray(new int[]{320,640})).put("productionHeaderStyle",true).put("privateDataRead",false).toString());finish(-1,b);
 }catch(Throwable e){b.putString("failure",e.getClass().getSimpleName()+":"+e.getMessage());finish(1,b);}}
}