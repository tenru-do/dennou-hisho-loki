package com.example.rokidkeyboardbridge;
import android.widget.TextView;import android.util.TypedValue;
/** Keep the compact information band readable when health text wraps. */
public final class HealthHudTypography {
 public static void apply(TextView view){view.setTextSize(10.5f);view.setPadding(2,0,2,0);view.setLineSpacing(0,.8f);view.setAutoSizeTextTypeUniformWithConfiguration(8,10,1,TypedValue.COMPLEX_UNIT_SP);}
 private HealthHudTypography(){}
}
