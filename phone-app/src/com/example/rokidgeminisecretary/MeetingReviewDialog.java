package com.example.rokidgeminisecretary;
import android.app.*;import android.widget.*;import org.json.*;import java.io.*;import java.nio.file.*;
final class MeetingReviewDialog {
 static void show(MainActivity owner,String id){showAt(owner,id,0,false);}
 static void showAt(MainActivity owner,String id,long anchor,boolean searched){Toast.makeText(owner,MeetingFeatureRetired.MESSAGE,Toast.LENGTH_LONG).show();}
 private MeetingReviewDialog(){}
}
