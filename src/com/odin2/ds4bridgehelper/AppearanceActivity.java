package com.odin2.ds4bridgehelper;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.AlertDialog;
import android.graphics.Color;
import android.view.*;
import android.text.*;
import java.util.Locale;
import android.widget.*;

public final class AppearanceActivity extends BridgeScreen {
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);SharedPreferences preferences=getSharedPreferences("PickerActivity",MODE_PRIVATE);
        LinearLayout page=ui.column();page.setPadding(ui.dp(18),ui.dp(12),ui.dp(18),ui.dp(18));page.addView(ui.heading("Appearance"));
        LinearLayout card=ui.card();card.setPadding(ui.dp(18),ui.dp(14),ui.dp(18),ui.dp(14));page.addView(card);TextView title=ui.text("Theme",18);title.setPadding(0,0,0,0);card.addView(title);ui.space(card,8);
        String[] labels={"Follow system","Light","Dark"};int mode=preferences.getInt("theme",0);
        LinearLayout row=new LinearLayout(this);Button theme=ui.button("Appearance: "+labels[mode],true),accent=ui.button("Accent color",true);row.addView(theme,new LinearLayout.LayoutParams(0,ui.dp(44),1));LinearLayout.LayoutParams accentLayout=new LinearLayout.LayoutParams(0,ui.dp(44),1);accentLayout.leftMargin=ui.dp(10);row.addView(accent,accentLayout);card.addView(row);ui.space(card,8);
        theme.setOnClickListener(v->ui.dialog().setTitle("Appearance").setSingleChoiceItems(labels,mode,(d,index)->{preferences.edit().putInt("theme",index).apply();d.dismiss();recreate();}).setNegativeButton("Cancel",null).show());
        accent.setOnClickListener(v->chooseAccent());TextView instructions=ui.note("Choose a color with the wheel, RGB sliders or a hex value. Text contrast adjusts for readability.");instructions.setTextSize(12);instructions.setPadding(0,0,0,0);card.addView(instructions);
        ScrollView scroll=new ScrollView(this);scroll.addView(page);setContentView(scroll);
    }
    private void chooseAccent(){
        LinearLayout layout=ui.column();layout.setPadding(ui.dp(24),ui.dp(8),ui.dp(24),ui.dp(8));boolean narrow=getResources().getConfiguration().screenWidthDp<440;
        LinearLayout controls=new LinearLayout(this);controls.setOrientation(narrow?1:0);ColorWheelView wheel=new ColorWheelView(this,ui.accent,ui.foreground);controls.addView(wheel,new LinearLayout.LayoutParams(ui.dp(176),ui.dp(176)));
        LinearLayout fields=ui.column();fields.setPadding(narrow?0:ui.dp(20),0,0,0);controls.addView(fields,narrow?new LinearLayout.LayoutParams(-1,-2):new LinearLayout.LayoutParams(0,-2,1));
        TextView preview=ui.text("Accent preview",16);preview.setGravity(Gravity.CENTER);preview.setPadding(ui.dp(8),ui.dp(12),ui.dp(8),ui.dp(12));preview.setBackground(ui.shape(ui.accent,16));preview.setTextColor(AppearanceColors.ink(ui.accent));fields.addView(preview);ui.space(fields,8);
        EditText hex=new EditText(this);hex.setSingleLine(true);hex.setTextColor(ui.foreground);hex.setHintTextColor(ui.secondary);hex.setHint("#RRGGBB");hex.setContentDescription("Accent hex color");hex.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);hex.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE|android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);hex.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});fields.addView(hex,new LinearLayout.LayoutParams(-1,ui.dp(44)));
        SeekBar[] rgb=new SeekBar[3];TextView[] rgbLabels=new TextView[3];String[] channels={"Red","Green","Blue"};
        for(int i=0;i<3;i++){LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);rgbLabels[i]=ui.text(channels[i],12);rgbLabels[i].setPadding(0,0,0,0);line.addView(rgbLabels[i],new LinearLayout.LayoutParams(ui.dp(76),ui.dp(36)));rgb[i]=new SeekBar(this);rgb[i].setMax(255);rgb[i].setProgressTintList(android.content.res.ColorStateList.valueOf(ui.accentText));rgb[i].setThumbTintList(android.content.res.ColorStateList.valueOf(ui.accentText));line.addView(rgb[i],new LinearLayout.LayoutParams(0,ui.dp(36),1));fields.addView(line);}
        layout.addView(controls);ui.space(layout,12);
        LinearLayout swatches=new LinearLayout(this);for(int color:new int[]{0xffff8000,0xfffb0b24,0xff64c895,0xff8000ff,0xffffff00,0xffffffff}){
            Button swatch=colorPreset(color,()->wheel.setColor(color));swatch.setContentDescription(String.format(Locale.ROOT,"Use #%06X",color&0xffffff));swatches.addView(swatch,new LinearLayout.LayoutParams(0,ui.dp(42),1));
        }layout.addView(swatches);
        final boolean[] updating={false};wheel.changed=color->{updating[0]=true;hex.setText(String.format(Locale.ROOT,"#%06X",color&0xffffff));hex.setError(null);int[] values={Color.red(color),Color.green(color),Color.blue(color)};for(int i=0;i<3;i++){rgb[i].setProgress(values[i]);rgbLabels[i].setText(channels[i]+" "+values[i]);rgb[i].setContentDescription(channels[i]+" channel "+values[i]+" of 255");}preview.setBackground(ui.shape(color,16));preview.setTextColor(AppearanceColors.ink(color));updating[0]=false;};
        hex.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int after){}public void onTextChanged(CharSequence s,int a,int before,int c){if(!updating[0]&&s.toString().matches("#[0-9a-fA-F]{6}")){wheel.setColor(Color.parseColor(s.toString()));hex.setSelection(hex.length());}}public void afterTextChanged(Editable e){}});
        for(int i=0;i<3;i++)rgb[i].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar bar,int value,boolean user){if(user&&!updating[0])wheel.setColor(Color.rgb(rgb[0].getProgress(),rgb[1].getProgress(),rgb[2].getProgress()));}public void onStartTrackingTouch(SeekBar bar){}public void onStopTrackingTouch(SeekBar bar){}});
        wheel.setColor(ui.accent);ScrollView scroll=new ScrollView(this);scroll.addView(layout);
        AlertDialog popup=ui.dialog().setTitle("Accent color").setView(scroll).setPositiveButton("Apply",null).setNeutralButton("Default",(d,w)->{getSharedPreferences("PickerActivity",0).edit().remove("accentColor").apply();recreate();}).setNegativeButton("Cancel",null).create();layout.setFocusableInTouchMode(true);layout.requestFocus();popup.show();popup.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);popup.getWindow().setLayout(Math.min(getResources().getDisplayMetrics().widthPixels-ui.dp(40),ui.dp(600)),-2);
        popup.getButton(-1).setOnClickListener(v->{String value=hex.getText().toString();if(!value.matches("#[0-9a-fA-F]{6}")){hex.setError("Use #RRGGBB");return;}getSharedPreferences("PickerActivity",0).edit().putInt("accentColor",Color.parseColor(value)).apply();popup.dismiss();recreate();});
    }
    private Button colorPreset(int color,Runnable action){
        Button preset=new Button(this){final android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);@Override protected void onDraw(android.graphics.Canvas canvas){super.onDraw(canvas);paint.setColor(color);float radius=Math.max(0,Math.min(ui.dp(16),Math.min(getWidth(),getHeight())/2f-ui.dp(4)));canvas.drawCircle(getWidth()/2f,getHeight()/2f,radius,paint);if(color==Color.WHITE){paint.setStyle(android.graphics.Paint.Style.STROKE);paint.setStrokeWidth(ui.dp(1));paint.setColor(ui.secondary);canvas.drawCircle(getWidth()/2f,getHeight()/2f,radius,paint);paint.setStyle(android.graphics.Paint.Style.FILL);}}};
        preset.setText("");preset.setBackground(ui.shape(ui.panel,24));preset.setStateListAnimator(null);preset.setElevation(0);preset.setMinHeight(0);preset.setMinimumHeight(0);preset.setMinWidth(0);preset.setMinimumWidth(0);preset.setPadding(0,0,0,0);preset.setOnClickListener(v->action.run());return preset;
    }
}
