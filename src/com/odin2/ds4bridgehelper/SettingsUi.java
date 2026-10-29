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
    final int background,surface,panel,foreground,secondary,accent,accentText;
    SettingsUi(Activity activity){
        this.activity=activity;int mode=activity.getSharedPreferences("PickerActivity",0).getInt("theme",0);
        dark=mode==2||(mode==0&&(activity.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);
        background=Color.parseColor(dark?"#131519":"#F5F5F8");surface=Color.parseColor(dark?"#1F2228":"#E9EAF0");panel=Color.parseColor(dark?"#272B32":"#FFFFFF");
        foreground=Color.parseColor(dark?"#F1F2F6":"#20232B");secondary=Color.parseColor(dark?"#A9B0BC":"#586171");accent=activity.getSharedPreferences("PickerActivity",0).getInt("accentColor",0xff0070cc)|0xff000000;accentText=AppearanceColors.readable(accent,panel);
    }
    int dp(int value){return Math.round(value*activity.getResources().getDisplayMetrics().density);}
    LinearLayout column(){LinearLayout view=new LinearLayout(activity);view.setOrientation(1);return view;}
    TextView text(String value,int size){TextView view=new TextView(activity);view.setText(value);view.setTextSize(size);view.setTextColor(foreground);view.setPadding(0,dp(4),0,dp(4));return view;}
    TextView heading(String value){TextView view=text(value,25);view.setTypeface(null,Typeface.BOLD);return view;}
    TextView note(String value){TextView view=text(value,13);view.setTextColor(secondary);return view;}
    GradientDrawable shape(int color,int radius){GradientDrawable drawable=new GradientDrawable();drawable.setColor(color);drawable.setCornerRadius(dp(radius));return drawable;}
    GradientDrawable background(boolean selected){int base=panel;if(selected)base=Color.rgb(Math.round(Color.red(panel)*.83f+Color.red(accent)*.17f),Math.round(Color.green(panel)*.83f+Color.green(accent)*.17f),Math.round(Color.blue(panel)*.83f+Color.blue(accent)*.17f));GradientDrawable drawable=shape(base,18);if(selected)drawable.setStroke(dp(1),accent);return drawable;}
    LinearLayout card(){LinearLayout view=column();view.setPadding(dp(14),dp(10),dp(14),dp(10));view.setBackground(background(false));return view;}
    Button button(String value,boolean primary){Button view=new Button(activity);view.setText(value);view.setAllCaps(false);view.setTextSize(14);view.setMinHeight(dp(44));view.setMinWidth(0);view.setMinimumWidth(0);view.setStateListAnimator(null);view.setElevation(0);view.setPadding(dp(14),dp(8),dp(14),dp(8));view.setTextColor(primary?AppearanceColors.ink(accent):foreground);view.setBackgroundTintList(null);view.setBackground(shape(primary?accent:panel,24));return view;}
    Button overflow(){
        Button button=new Button(activity){final android.graphics.Paint paint=new android.graphics.Paint(3);
            @Override protected void onDraw(android.graphics.Canvas canvas){super.onDraw(canvas);paint.setColor(foreground);float x=getWidth()/2f,y=getHeight()/2f;for(int i=-1;i<=1;i++)canvas.drawCircle(x,y+i*dp(6),dp(2),paint);}};
        button.setText("");button.setPadding(0,0,0,0);button.setMinWidth(0);button.setMinimumWidth(0);button.setMinHeight(0);button.setMinimumHeight(0);button.setStateListAnimator(null);button.setBackgroundTintList(null);button.setBackground(shape(panel,24));button.setContentDescription("More options");return button;
    }
    View sectionHeader(String label,LinearLayout body,boolean expanded,Runnable changed){
        LinearLayout header=new LinearLayout(activity);header.setGravity(Gravity.CENTER_VERTICAL);header.setMinimumHeight(dp(48));
        TextView title=text(label,18);title.setTypeface(null,Typeface.BOLD);title.setPadding(dp(8),0,0,0);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        View chevron=new View(activity){final android.graphics.Paint paint=new android.graphics.Paint(3);@Override protected void onDraw(android.graphics.Canvas canvas){paint.setColor(foreground);paint.setStyle(android.graphics.Paint.Style.STROKE);paint.setStrokeWidth(dp(2));paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);paint.setStrokeJoin(android.graphics.Paint.Join.ROUND);float x=getWidth()/2f,y=getHeight()/2f,d=body.getVisibility()==View.VISIBLE?-1:1;android.graphics.Path path=new android.graphics.Path();path.moveTo(x-dp(6),y-d*dp(3));path.lineTo(x,y+d*dp(3));path.lineTo(x+dp(6),y-d*dp(3));canvas.drawPath(path,paint);}};
        header.addView(chevron,new LinearLayout.LayoutParams(dp(32),dp(32)));chevron.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        body.setVisibility(expanded?View.VISIBLE:View.GONE);header.setContentDescription((expanded?"Collapse ":"Expand ")+label);header.setFocusable(true);header.setClickable(true);
        header.setOnClickListener(v->{boolean show=body.getVisibility()!=View.VISIBLE;body.setVisibility(show?View.VISIBLE:View.GONE);header.setContentDescription((show?"Collapse ":"Expand ")+label);chevron.invalidate();if(changed!=null)changed.run();});return header;
    }
    android.app.AlertDialog.Builder dialog(){return new android.app.AlertDialog.Builder(activity,activity.getResources().getIdentifier(dark?"RavenAboutDark":"RavenAboutLight","style",activity.getPackageName())){@Override public android.app.AlertDialog create(){android.app.AlertDialog result=super.create();result.setOnShowListener(d->{for(int which:new int[]{-1,-2,-3}){Button b=result.getButton(which);if(b!=null)b.setTextColor(accentText);}});return result;}};}
    ImageView logo(){ImageView view=new ImageView(activity);view.setImageDrawable(activity.getApplicationInfo().loadIcon(activity.getPackageManager()));view.setBackground(shape(0xff1f2228,10));view.setContentDescription("Raven Gamepad icon");view.setScaleType(ImageView.ScaleType.FIT_CENTER);view.setClipToOutline(true);view.setOutlineProvider(new ViewOutlineProvider(){@Override public void getOutline(View v,android.graphics.Outline outline){outline.setRoundRect(0,0,v.getWidth(),v.getHeight(),v.getWidth()*.24f);}});return view;}
    private static double channel(int c){double n=c/255.0;return n<=0.04045?n/12.92:Math.pow((n+.055)/1.055,2.4);}
    private static double luminance(int c){return .2126*channel((c>>16)&255)+.7152*channel((c>>8)&255)+.0722*channel(c&255);}
    private static double contrast(int a,int b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
    private static int readable(int color,int surface){if(contrast(color,surface)>=4.5)return color;for(int i=1;i<=100;i++){float t=i/100f;int r=Math.round(((color>>16)&255)*(1-t)+255*t),g=Math.round(((color>>8)&255)*(1-t)+255*t),b=Math.round((color&255)*(1-t)+255*t);int result=0xff000000|(r<<16)|(g<<8)|b;if(contrast(result,surface)>=4.5)return result;}return Color.WHITE;}
    void space(LinearLayout page,int height){page.addView(new View(activity),new LinearLayout.LayoutParams(1,dp(height)));}
    void equal(LinearLayout row,View view){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(dp(2),0,dp(2),0);row.addView(view,p);}
    View divider(){View view=new View(activity);view.setBackgroundColor(dark?Color.parseColor("#393F49"):Color.parseColor("#DCE3EC"));view.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(1)));return view;}
}
