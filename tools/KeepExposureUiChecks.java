package com.example.rokidkeyboardbridge;
import android.app.*;import android.widget.*;
/** Actual native dialog layout and explicit-selection semantics; no capture/settings writes. */
public final class KeepExposureUiChecks {
 static int checks;static void ok(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
 public static int run(Activity owner){checks=0;int[] selected={99},calls={0};
  for(int index=0;index<3;index++){
   AlertDialog dialog=KeepExposureDialog.create(owner,0,value->{selected[0]=value;calls[0]++;});try{dialog.show();ListView list=dialog.getListView();
    ok(list!=null&&list.getAdapter().getCount()==3,"all exposure choices exist");ok(list.getParent()!=null&&list.isShown(),"selection list attached and visible, not hidden by message");ok(list.getCheckedItemPosition()==1,"auto is selected initially");ok(calls[0]==index,"opening/focusing never changes selection");ok(dialog.getButton(-3)!=null,"separate explanation is available");list.performItemClick(null,index,list.getAdapter().getItemId(index));ok(calls[0]==index+1&&selected[0]==index-1,"one explicit selection delivers correct EV mode");
   }finally{dialog.dismiss();}
  }
  return checks;
 }
}
