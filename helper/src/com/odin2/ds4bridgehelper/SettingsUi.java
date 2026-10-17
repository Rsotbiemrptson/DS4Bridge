package com.odin2.ds4bridgehelper;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

/** Shared native Android styling for the approved grouped settings design. */
final class SettingsUi {
    final Activity activity;
    final boolean dark;
    final int background,panel,foreground,secondary,accent;
    SettingsUi(Activity activity){
        this.activity=activity;int mode=activity.getSharedPreferences("PickerActivity",0).getInt("theme",0);
        dark=mode==2||(mode==0&&(activity.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);
        background=Color.parseColor(dark?"#151B1F":"#F5F8F8");panel=Color.parseColor(dark?"#20292E":"#FFFFFF");
        foreground=Color.parseColor(dark?"#F1F6F5":"#152825");secondary=Color.parseColor(dark?"#B6C5C4":"#526763");accent=Color.parseColor(dark?"#67D4C8":"#006D63");
    }
    int dp(int value){return Math.round(value*activity.getResources().getDisplayMetrics().density);}
    LinearLayout column(){LinearLayout view=new LinearLayout(activity);view.setOrientation(1);return view;}
    TextView text(String value,int size){TextView view=new TextView(activity);view.setText(value);view.setTextSize(size);view.setTextColor(foreground);view.setPadding(0,dp(4),0,dp(4));return view;}
    TextView heading(String value){TextView view=text(value,21);view.setTypeface(null,Typeface.BOLD);return view;}
    TextView note(String value){TextView view=text(value,13);view.setTextColor(secondary);return view;}
    GradientDrawable background(boolean selected){GradientDrawable drawable=new GradientDrawable();drawable.setColor(selected?(dark?Color.parseColor("#214D4D"):Color.parseColor("#D8F2ED")):panel);drawable.setCornerRadius(dp(8));drawable.setStroke(dp(1),selected?accent:(dark?Color.parseColor("#39454A"):Color.parseColor("#CCD9D7")));return drawable;}
    LinearLayout card(){LinearLayout view=column();view.setPadding(dp(12),dp(8),dp(12),dp(8));view.setBackground(background(false));return view;}
    Button button(String value,boolean primary){Button view=new Button(activity);view.setText(value);view.setAllCaps(false);view.setTextSize(14);view.setMinHeight(dp(44));view.setTextColor(primary?(dark?Color.parseColor("#082D29"):Color.WHITE):foreground);view.setBackgroundTintList(ColorStateList.valueOf(primary?accent:panel));return view;}
    void space(LinearLayout page,int height){page.addView(new View(activity),new LinearLayout.LayoutParams(1,dp(height)));}
    void equal(LinearLayout row,View view){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(dp(2),0,dp(2),0);row.addView(view,p);}
    View divider(){View view=new View(activity);view.setBackgroundColor(dark?Color.parseColor("#39454A"):Color.parseColor("#D8E2E0"));view.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(1)));return view;}
}
