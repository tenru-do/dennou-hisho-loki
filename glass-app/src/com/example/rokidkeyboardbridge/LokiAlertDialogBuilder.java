package com.example.rokidkeyboardbridge;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
/** Native dialog selections also acknowledge only actual activation, never focus movement. */
final class LokiAlertDialogBuilder extends AlertDialog.Builder {
    LokiAlertDialogBuilder(Context c){super(c);}
    LokiAlertDialogBuilder(Context c,int theme){super(c,theme);}
    @Override public AlertDialog create(){AlertDialog dialog=super.create();NavigationDialogWake.install(dialog,getContext());return dialog;}
    private DialogInterface.OnClickListener wrap(CharSequence label,DialogInterface.OnClickListener action){
        return (dialog,which)->{LokiActionButton.react(getContext(),label);if(action!=null)action.onClick(dialog,which);};
    }
    @Override public AlertDialog.Builder setPositiveButton(CharSequence text,DialogInterface.OnClickListener action){return super.setPositiveButton(text,wrap(text,action));}
    @Override public AlertDialog.Builder setNegativeButton(CharSequence text,DialogInterface.OnClickListener action){return super.setNegativeButton(text,wrap(text,action));}
    @Override public AlertDialog.Builder setNeutralButton(CharSequence text,DialogInterface.OnClickListener action){return super.setNeutralButton(text,wrap(text,action));}
    @Override public AlertDialog.Builder setItems(CharSequence[] items,DialogInterface.OnClickListener action){return super.setItems(items,(dialog,which)->{LokiActionButton.react(getContext(),items[which]);if(action!=null)action.onClick(dialog,which);});}
    @Override public AlertDialog.Builder setSingleChoiceItems(CharSequence[] items,int selected,DialogInterface.OnClickListener action){return super.setSingleChoiceItems(items,selected,(dialog,which)->{LokiActionButton.react(getContext(),items[which]);if(action!=null)action.onClick(dialog,which);});}
    @Override public AlertDialog.Builder setMultiChoiceItems(CharSequence[] items,boolean[] checked,DialogInterface.OnMultiChoiceClickListener action){return super.setMultiChoiceItems(items,checked,(dialog,which,value)->{LokiActionButton.react(getContext(),items[which]);if(action!=null)action.onClick(dialog,which,value);});}
}
