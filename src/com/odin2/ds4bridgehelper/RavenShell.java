package com.odin2.ds4bridgehelper;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;

/** Shared Raven navigation; add future sections here rather than duplicating each screen. */
final class RavenShell {
    interface Navigate { void open(Class<?> target); }
    static View wrap(Activity activity,SettingsUi ui,View page,int selected,Navigate navigate){
        boolean wide=activity.getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        LinearLayout root=new LinearLayout(activity);root.setOrientation(wide?0:1);root.setBackgroundColor(ui.background);
        LinearLayout nav=new LinearLayout(activity);nav.setOrientation(wide?1:0);nav.setGravity(wide?Gravity.TOP:Gravity.CENTER_VERTICAL);nav.setPadding(ui.dp(12),ui.dp(wide?16:10),ui.dp(12),ui.dp(12));nav.setBackgroundColor(ui.surface);nav.setTag("raven-navigation");nav.setContentDescription("App navigation");
        if(wide){
            LinearLayout brand=new LinearLayout(activity);brand.setGravity(Gravity.CENTER_VERTICAL);brand.addView(ui.logo(),new LinearLayout.LayoutParams(ui.dp(40),ui.dp(40)));
            TextView title=ui.text("Raven Gamepad",17);title.setTypeface(null,Typeface.BOLD);title.setPadding(ui.dp(6),0,0,0);brand.addView(title,new LinearLayout.LayoutParams(0,-2,1));nav.addView(brand,new LinearLayout.LayoutParams(-1,-2));ui.space(nav,24);
        }
        String[] names={"Apps","Appearance","Bridge controls","Controller mapping","Diagnostics"};
        Class<?>[] screens={PickerActivity.class,AppearanceActivity.class,BridgeActivity.class,MappingActivity.class,DiagnosticsActivity.class};
        for(int i=0;i<names.length;i++){
            final Class<?> target=screens[i];Button tab=ui.button(names[i],false);tab.setTag(target);tab.setContentDescription("Navigate: "+names[i]);tab.setPadding(ui.dp(6),ui.dp(8),ui.dp(6),ui.dp(8));tab.setTextSize(wide?14:13);tab.setOnClickListener(v->navigate.open(target));
            LinearLayout.LayoutParams params=wide?new LinearLayout.LayoutParams(-1,ui.dp(48)):new LinearLayout.LayoutParams(0,ui.dp(48),1);params.setMargins(wide?0:ui.dp(3),0,wide?0:ui.dp(3),wide?ui.dp(8):0);nav.addView(tab,params);
        }
        if(wide){root.addView(nav,new LinearLayout.LayoutParams(ui.dp(148),-1));root.addView(page,new LinearLayout.LayoutParams(0,-1,1));}
        else{root.addView(page,new LinearLayout.LayoutParams(-1,0,1));root.addView(nav,new LinearLayout.LayoutParams(-1,ui.dp(70)));}
        select(root,ui,activity.getClass());return root;
    }
    static void select(View root,SettingsUi ui,Class<?> target){
        ViewGroup nav=(ViewGroup)root.findViewWithTag("raven-navigation");boolean settings=target!=PickerActivity.class;
        for(int i=0;i<nav.getChildCount();i++)if(nav.getChildAt(i) instanceof Button){Button tab=(Button)nav.getChildAt(i);Class<?> type=(Class<?>)tab.getTag();boolean active=type==target;tab.setVisibility(type==PickerActivity.class||settings?View.VISIBLE:View.GONE);tab.setSelected(active);tab.setBackground(ui.shape(active?ui.accent:ui.panel,24));tab.setTextColor(active?AppearanceColors.ink(ui.accent):ui.foreground);}
    }
    private RavenShell(){}
}
