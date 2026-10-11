package com.example.rokidkeyboardbridge;
import android.content.Context;
import android.widget.TextView;
import android.view.View;
import java.util.function.BooleanSupplier;
/** Hidden legacy status remains the data source for the full-body diagnostics. */
final class FullBodyStatusView extends TextView {
 private BooleanSupplier selected;private Runnable changed;
 FullBodyStatusView(Context context,BooleanSupplier selected,Runnable changed){super(context);this.selected=selected;this.changed=changed;}
 @Override public void setVisibility(int visibility){boolean full=selected!=null&&selected.getAsBoolean();super.setVisibility(full?View.GONE:visibility);if(full&&changed!=null)changed.run();}
 @Override protected void onTextChanged(CharSequence text,int start,int before,int count){super.onTextChanged(text,start,before,count);if(changed!=null)changed.run();}
}
