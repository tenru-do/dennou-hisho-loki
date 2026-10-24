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
        enableSelection(button);
    }
    static void enableSelection(Button button) {
        button.setFocusable(true); button.setFocusableInTouchMode(true); button.setClickable(true);
        button.setOnTouchListener((v,event)-> {
            if(!v.isEnabled())return false;
            if(event.getAction()==MotionEvent.ACTION_DOWN){v.requestFocus();v.setPressed(true);return true;}
            if(event.getAction()==MotionEvent.ACTION_UP){boolean activate=v.isPressed()&&event.getX()>=0&&event.getX()<v.getWidth()&&event.getY()>=0&&event.getY()<v.getHeight();v.setPressed(false);if(activate)v.performClick();return true;}
            if(event.getAction()==MotionEvent.ACTION_CANCEL){v.setPressed(false);return true;}
            return true;
        });
        button.setOnKeyListener((v,code,event)-> {
            if(code!=KeyEvent.KEYCODE_DPAD_CENTER && code!=KeyEvent.KEYCODE_ENTER
                    && code!=KeyEvent.KEYCODE_NUMPAD_ENTER && code!=KeyEvent.KEYCODE_BUTTON_SELECT)return false;
            if(event.getAction()==KeyEvent.ACTION_UP && !event.isCanceled())v.performClick();
            return true;
        });
    }
    private static void collectButtons(View view,java.util.List<Button> buttons) {
        if(view instanceof Button && view.isShown() && view.isEnabled())buttons.add((Button)view);
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)collectButtons(group.getChildAt(i),buttons);}
    }
    static void installDialogSelection(AlertDialog dialog,Button fallback) {
        final int[] held={-1}; final View[] pressed={null};
        final long[] nextSlideAt={0L};
        dialog.setOnKeyListener((d,code,event)-> {
            boolean forward=code==KeyEvent.KEYCODE_DPAD_RIGHT || code==KeyEvent.KEYCODE_DPAD_DOWN
                    || (code==KeyEvent.KEYCODE_TAB && !event.isShiftPressed());
            boolean backward=code==KeyEvent.KEYCODE_DPAD_LEFT || code==KeyEvent.KEYCODE_DPAD_UP
                    || (code==KeyEvent.KEYCODE_TAB && event.isShiftPressed());
            boolean select=code==KeyEvent.KEYCODE_DPAD_CENTER || code==KeyEvent.KEYCODE_ENTER
                    || code==KeyEvent.KEYCODE_NUMPAD_ENTER || code==KeyEvent.KEYCODE_BUTTON_SELECT;
            if(!forward && !backward && !select)return false;
            if(event.getAction()==KeyEvent.ACTION_UP){
                if(held[0]!=code)return true;
                View target=pressed[0];held[0]=-1;pressed[0]=null;
                if(select && !event.isCanceled() && target!=null && target.isShown() && target.isEnabled())target.performClick();
                return true;
            }
            if(event.getAction()!=KeyEvent.ACTION_DOWN)return true;
            // One physical slide moves one item; holding the pad cannot skip rows.
            if(event.getRepeatCount()>0 || held[0]==code)return true;
            held[0]=code;
            View root=dialog.getWindow().getDecorView(),focus=root.findFocus();
            if(select){pressed[0]=focus instanceof Button?focus:fallback;return true;}
            // The touchpad can emit fresh DOWN/UP pairs for one slide (repeatCount=0).
            long now=android.os.SystemClock.uptimeMillis();
            if(now<nextSlideAt[0])return true;
            nextSlideAt[0]=now+400L;
            java.util.List<Button> buttons=new java.util.ArrayList<>();collectButtons(root,buttons);
            if(buttons.isEmpty())return true;
            int index=buttons.indexOf(focus);
            int explicit=View.NO_ID;
            if(focus!=null){
                if(code==KeyEvent.KEYCODE_DPAD_RIGHT)explicit=focus.getNextFocusRightId();
                else if(code==KeyEvent.KEYCODE_DPAD_LEFT)explicit=focus.getNextFocusLeftId();
                else if(code==KeyEvent.KEYCODE_DPAD_DOWN)explicit=focus.getNextFocusDownId();
                else if(code==KeyEvent.KEYCODE_DPAD_UP)explicit=focus.getNextFocusUpId();
                else if(forward)explicit=focus.getNextFocusForwardId();
            }
            View target=explicit==View.NO_ID?null:root.findViewById(explicit);
            if(!(target instanceof Button) || !target.isShown() || !target.isEnabled())
                target=buttons.get(index<0?0:Math.max(0,Math.min(buttons.size()-1,index+(forward?1:-1))));
            target.requestFocus();return true;
        });
    }
    private static void blackContainers(View v){
        if(v instanceof ViewGroup){v.setBackgroundColor(Color.BLACK);ViewGroup g=(ViewGroup)v;
            for(int i=0;i<g.getChildCount();i++)blackContainers(g.getChildAt(i));}
    }
    static AlertDialog show(Activity activity,String title,String[] labels,int current,DialogInterface.OnClickListener action){
        return show(activity,title,labels,current,action,false);
    }
    static AlertDialog showRoutes(Activity activity,String title,String[] labels,DialogInterface.OnClickListener action){
        return show(activity,title,labels,-1,action,true);
    }
    private static AlertDialog show(Activity activity,String title,String[] labels,int current,DialogInterface.OnClickListener action,boolean wrap){
        int theme=activity.getResources().getIdentifier("NavigationDialogTheme","style",activity.getPackageName());
        AlertDialog dialog=new LokiAlertDialogBuilder(activity,theme).create();
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
            final int index=i;Button b=new LokiActionButton(context);b.setText((i==current?"✓ ":"")+labels[i]);b.setTextColor(i==current?Color.CYAN:Color.WHITE);
            b.setTextSize(16);b.setAllCaps(false);b.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);b.setPadding(dp(context,8),0,dp(context,8),0);
            b.setMinHeight(0);b.setMinimumHeight(0);b.setBackgroundTintList(null);b.setStateListAnimator(null);b.setBackground(background(context,i==current));
            enableSelection(b);b.setOnClickListener(v->{dialog.dismiss();action.onClick(dialog,index);});
            if(wrap){b.setPadding(dp(context,8),dp(context,6),dp(context,8),dp(context,6));b.setMinHeight(dp(context,48));b.setSingleLine(false);}
            choices.addView(b,new LinearLayout.LayoutParams(-1,wrap?-2:dp(context,48)));buttons[i]=b;
        }
        Button close=new LokiActionButton(context);close.setText("閉じる");close.setTextColor(Color.WHITE);close.setTextSize(16);close.setAllCaps(false);
        close.setMinHeight(0);close.setMinimumHeight(0);close.setBackgroundTintList(null);close.setStateListAnimator(null);close.setBackground(background(context,false));close.setOnClickListener(v->dialog.cancel());
        root.addView(close,new LinearLayout.LayoutParams(-1,dp(context,40)));
        dialog.setView(root,0,0,0,0);installDialogSelection(dialog,buttons.length>0?buttons[Math.max(0,Math.min(current,buttons.length-1))]:close);dialog.show();Window window=dialog.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));window.getDecorView().setBackgroundColor(Color.BLACK);window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        blackContainers(window.getDecorView());if(buttons.length>0)buttons[Math.max(0,Math.min(current,buttons.length-1))].requestFocus();return dialog;
    }
}
