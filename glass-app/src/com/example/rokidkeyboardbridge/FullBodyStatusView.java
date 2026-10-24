package com.example.rokidkeyboardbridge;
import android.content.Context;
import android.widget.TextView;
import android.view.View;
import java.util.function.BooleanSupplier;
/** Status is data for the explicit SET dialog, never an overlay in either visual mode. */
final class FullBodyStatusView extends TextView {
 private BooleanSupplier selected;private Runnable changed;
 FullBodyStatusView(Context context,BooleanSupplier selected,Runnable changed){super(context);this.selected=selected;this.changed=changed;super.setVisibility(View.GONE);}
 @Override public void setVisibility(int visibility){super.setVisibility(View.GONE);if(changed!=null)changed.run();}
 @Override protected void onTextChanged(CharSequence text,int start,int before,int count){super.onTextChanged(text,start,before,count);if(changed!=null)changed.run();}
}
