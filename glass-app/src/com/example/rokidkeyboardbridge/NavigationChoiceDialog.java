package com.example.rokidkeyboardbridge;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

/** Navigation-only optical black surfaces; selection and focus remain distinct. */
final class NavigationChoiceDialog {
    private static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    private static Drawable background(Context c,boolean selected){
        StateListDrawable states=new StateListDrawable();
        for(int state:new int[]{android.R.attr.state_focused,android.R.attr.state_pressed}){
            GradientDrawable focus=new GradientDrawable();focus.setColor(Color.BLACK);focus.setStroke(dp(c,2),Color.WHITE);
            states.addState(new int[]{state},focus);
        }
        GradientDrawable idle=new GradientDrawable();idle.setColor(Color.BLACK);
        if(selected)idle.setStroke(dp(c,1),Color.CYAN);
        states.addState(new int[]{},idle);return states;
    }
    static void styleButton(Context context,Button button) {
        button.setTextColor(Color.WHITE); button.setTextSize(16); button.setAllCaps(false);
        button.setMinWidth(0); button.setMinimumWidth(0); button.setMinHeight(0); button.setMinimumHeight(0);
        button.setBackgroundTintList(null); button.setStateListAnimator(null); button.setBackground(background(context,false));
        button.setFocusable(true); button.setFocusableInTouchMode(true);
    }
    private static void blackContainers(View v){
        if(v instanceof ViewGroup){v.setBackgroundColor(Color.BLACK);ViewGroup g=(ViewGroup)v;
            for(int i=0;i<g.getChildCount();i++)blackContainers(g.getChildAt(i));}
    }
    static AlertDialog show(Activity activity,String title,String[] labels,int current,DialogInterface.OnClickListener action){
        int theme=activity.getResources().getIdentifier("NavigationDialogTheme","style",activity.getPackageName());
        AlertDialog dialog=new AlertDialog.Builder(activity,theme).create();
        Context context=dialog.getContext();
        LinearLayout root=new LinearLayout(context);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.BLACK);
        root.setPadding(dp(context,8),dp(context,6),dp(context,8),dp(context,6));
        TextView heading=new TextView(context);heading.setText(title);heading.setTextColor(Color.WHITE);heading.setTextSize(18);heading.setBackgroundColor(Color.BLACK);root.addView(heading);
        ScrollView scroll=new ScrollView(context);scroll.setBackgroundColor(Color.BLACK);scroll.setFillViewport(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout choices=new LinearLayout(context);choices.setOrientation(LinearLayout.VERTICAL);choices.setBackgroundColor(Color.BLACK);scroll.addView(choices);
        int available=activity.getWindow().getDecorView().getHeight();if(available<=0)available=context.getResources().getDisplayMetrics().heightPixels;
        root.addView(scroll,new LinearLayout.LayoutParams(-1,Math.min(dp(context,192),Math.max(dp(context,48),available-dp(context,100)))));
        Button[] buttons=new Button[labels.length];
        for(int i=0;i<labels.length;i++){
            final int index=i;Button b=new Button(context);b.setText((i==current?"✓ ":"")+labels[i]);b.setTextColor(i==current?Color.CYAN:Color.WHITE);
            b.setTextSize(16);b.setAllCaps(false);b.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);b.setPadding(dp(context,8),0,dp(context,8),0);
            b.setMinHeight(0);b.setMinimumHeight(0);b.setBackgroundTintList(null);b.setStateListAnimator(null);b.setBackground(background(context,i==current));
            b.setFocusable(true);b.setFocusableInTouchMode(true);b.setOnClickListener(v->{dialog.dismiss();action.onClick(dialog,index);});
            choices.addView(b,new LinearLayout.LayoutParams(-1,dp(context,48)));buttons[i]=b;
        }
        Button close=new Button(context);close.setText("閉じる");close.setTextColor(Color.WHITE);close.setTextSize(16);close.setAllCaps(false);
        close.setMinHeight(0);close.setMinimumHeight(0);close.setBackgroundTintList(null);close.setStateListAnimator(null);close.setBackground(background(context,false));close.setOnClickListener(v->dialog.dismiss());
        root.addView(close,new LinearLayout.LayoutParams(-1,dp(context,40)));
        dialog.setView(root,0,0,0,0);dialog.show();Window window=dialog.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));window.getDecorView().setBackgroundColor(Color.BLACK);window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        blackContainers(window.getDecorView());buttons[Math.max(0,Math.min(current,buttons.length-1))].requestFocus();return dialog;
    }
}
