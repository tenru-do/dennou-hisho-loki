package com.example.rokidkeyboardbridge;
import android.widget.TextView;
/** Compact physical HUD size; reserve full font ascent/descent for all status rows. */
final class NavigationHeaderStyle {
 static void apply(TextView view){
  view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP,9f);
  view.setIncludeFontPadding(true);view.setLineSpacing(0f,1f);view.setMaxLines(3);
  view.setEllipsize(android.text.TextUtils.TruncateAt.END);
  view.setGravity(android.view.Gravity.TOP|android.view.Gravity.RIGHT);
 }
 static int height(TextView view){
  android.view.ViewGroup.LayoutParams params=view.getLayoutParams();
  int width=params!=null&&params.width>0?params.width:view.getWidth();
  if(width<=0)width=Math.max(1,Math.round(190*view.getResources().getDisplayMetrics().density));
  view.measure(android.view.View.MeasureSpec.makeMeasureSpec(width,android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec(0,android.view.View.MeasureSpec.UNSPECIFIED));
  android.graphics.Paint.FontMetricsInt f=view.getPaint().getFontMetricsInt();
  // Japanese/emoji fallback fonts can exceed the default Latin font metrics.
  return Math.max(view.getMeasuredHeight(),NavigationHeaderLayout.textHeight(view.getLineHeight(),3,f.top,f.ascent,f.descent,f.bottom));
 }
 private NavigationHeaderStyle(){}
}
