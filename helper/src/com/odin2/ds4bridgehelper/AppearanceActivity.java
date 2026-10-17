package com.odin2.ds4bridgehelper;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;

public final class AppearanceActivity extends BridgeScreen {
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);SharedPreferences preferences=getSharedPreferences("PickerActivity",MODE_PRIVATE);
        LinearLayout page=ui.column();page.setPadding(ui.dp(18),ui.dp(12),ui.dp(18),ui.dp(18));page.addView(ui.heading("Appearance"));
        LinearLayout card=ui.card();page.addView(card);card.addView(ui.text("Theme",17));
        RadioGroup themes=new RadioGroup(this);themes.setOrientation(RadioGroup.HORIZONTAL);
        String[] labels={"Follow system","Light","Dark"};for(int i=0;i<labels.length;i++){RadioButton choice=new RadioButton(this);choice.setId(100+i);choice.setText(labels[i]);choice.setTextColor(ui.foreground);themes.addView(choice);}
        themes.check(100+preferences.getInt("theme",0));card.addView(themes);
        themes.setOnCheckedChangeListener((group,id)->{preferences.edit().putInt("theme",id-100).apply();recreate();});
        ScrollView scroll=new ScrollView(this);scroll.addView(page);setContentView(scroll);
    }
}
