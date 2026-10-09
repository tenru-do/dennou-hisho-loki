package com.example.rokidkeyboardbridge;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.view.Gravity;
import android.widget.Button;
/** Shared metrics and a focus border that does not change the caption width. */
final class HudButtonStyle {
 static final float TEXT_SIZE_SP=9f;
 static void apply(Button button){
  button.setTextSize(TEXT_SIZE_SP);button.setTypeface(Typeface.DEFAULT,Typeface.NORMAL);button.setAllCaps(false);
  button.setGravity(Gravity.CENTER);button.setIncludeFontPadding(false);button.setSingleLine(true);
  button.setMinHeight(0);button.setMinimumHeight(0);button.setMinWidth(0);button.setMinimumWidth(0);button.setPadding(0,0,0,0);
  button.setBackgroundTintList(null);button.setStateListAnimator(null);
  button.setTextColor(new ColorStateList(new int[][]{{android.R.attr.state_focused},{android.R.attr.state_pressed},{}},new int[]{Color.WHITE,Color.WHITE,Color.rgb(90,255,130)}));
  StateListDrawable states=new StateListDrawable();int stroke=Math.max(1,Math.round(button.getResources().getDisplayMetrics().density));
  for(int state:new int[]{android.R.attr.state_focused,android.R.attr.state_pressed}){GradientDrawable border=new GradientDrawable();border.setColor(Color.TRANSPARENT);border.setStroke(stroke,Color.WHITE);states.addState(new int[]{state},border);}
  states.addState(new int[]{},new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));button.setBackground(states);
 }
}
