package com.example.rokidkeyboardbridge;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Scrollable rows wrap within the left half, preserving full route information. */
final class NavigationOverviewView extends LinearLayout {
    private final TextView heading, metrics, detail;
    NavigationOverviewView(Context context) {
        super(context); setOrientation(VERTICAL); setBackgroundColor(Color.BLACK);
        setPadding(4,4,4,4); setClipChildren(true);
        heading=row(context,Color.CYAN,12);
        metrics=row(context,Color.WHITE,11);
        detail=row(context,Color.WHITE,10);
    }
    private TextView row(Context context,int color,float size) {
        TextView row=new TextView(context); row.setTextColor(color);row.setTextSize(size);
        row.setBackgroundColor(Color.BLACK);row.setPadding(0,0,0,4);
        row.setSingleLine(false);row.setHorizontallyScrolling(false);
        addView(row,new LayoutParams(-1,-2));return row;
    }
    void bind(NavigationOverviewSummary data) {
        heading.setText("経路全体"+(data.destination.isEmpty()?"":" · "+data.destination));
        metrics.setText("所要時間: "+data.duration+"\n到着予定: "+data.arrival+"\n総距離: "+data.distance);
        detail.setText(data.detailTitle+"\n"+data.detail);
    }
}
