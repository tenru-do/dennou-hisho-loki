package com.example.rokidkeyboardbridge;
import android.content.Context;
import android.content.ContextWrapper;
import android.widget.Button;
/** All activation paths (touch, keyboard and accessibility) share one reaction. */
class LokiActionButton extends Button {
    LokiActionButton(Context context){super(context);}
    static void react(Context context,CharSequence label){
        for(int i=0;i<12&&context!=null;i++){
            if(context instanceof MainActivity){((MainActivity)context).reactToButton(label);return;}
            if(!(context instanceof ContextWrapper))return;
            Context next=((ContextWrapper)context).getBaseContext();if(next==context)return;context=next;
        }
    }
    @Override public boolean performClick(){
        if(!isEnabled())return false;
        LokiActionButton.react(getContext(),getText());return super.performClick();
    }
}
