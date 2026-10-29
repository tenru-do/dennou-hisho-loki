package com.example.rokidkeyboardbridge;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.*;
import java.lang.reflect.*;
/** Observes dialog input without replacing selection, dismissal or touch handlers. */
final class NavigationDialogWake {
 static void install(AlertDialog dialog,Context context){
  MainActivity activity=null;
  for(int n=0;n<12&&context!=null;n++){
   if(context instanceof MainActivity){activity=(MainActivity)context;break;}
   if(!(context instanceof ContextWrapper))break;
   Context next=((ContextWrapper)context).getBaseContext();if(next==context)break;context=next;
  }
  if(activity==null)return;
  final MainActivity owner=activity;final Window window=dialog.getWindow();
  if(window==null)return;
  final Window.Callback original=window.getCallback();
  window.setCallback((Window.Callback)Proxy.newProxyInstance(Window.Callback.class.getClassLoader(),new Class<?>[]{Window.Callback.class},(proxy,method,args)->{
   String name=method.getName();
   if((name.equals("dispatchKeyEvent")||name.equals("dispatchTouchEvent")||name.equals("dispatchGenericMotionEvent"))&&args!=null&&args.length>0)
    owner.noteDialogInteraction(window);
   try{return method.invoke(original,args);}catch(InvocationTargetException error){throw error.getCause();}
  }));
  window.getDecorView().addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
   public void onViewAttachedToWindow(View view){owner.noteDialogInteraction(window);}
   public void onViewDetachedFromWindow(View view){owner.endDialogInteraction(window);}
  });
 }
}
