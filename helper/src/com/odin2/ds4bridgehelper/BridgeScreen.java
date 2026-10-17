package com.odin2.ds4bridgehelper;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

/** New screens share the app picker's theme and have a visible Back control. */
public class BridgeScreen extends Activity {
    SettingsUi ui;
    @Override public void onCreate(Bundle saved) {
        int mode = getSharedPreferences("PickerActivity", MODE_PRIVATE).getInt("theme", 0);
        boolean dark = mode == 2 || (mode == 0 && (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES);
        setTheme(dark ? android.R.style.Theme_Material : android.R.style.Theme_Material_Light);
        super.onCreate(saved);
        ui=new SettingsUi(this);
        getWindow().setStatusBarColor(ui.background);getWindow().setNavigationBarColor(ui.background);
        getWindow().getDecorView().setSystemUiVisibility(ui.dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setTitle("DS4 Bridge");getActionBar().setBackgroundDrawable(new ColorDrawable(ui.panel));
        }
    }
    @Override public void setContentView(View content){
        boolean wide=getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        LinearLayout shell=new LinearLayout(this);shell.setOrientation(wide?0:1);shell.setBackgroundColor(ui.background);
        LinearLayout nav=new LinearLayout(this);nav.setOrientation(wide?1:0);nav.setPadding(ui.dp(8),ui.dp(8),ui.dp(8),ui.dp(8));nav.setBackgroundColor(ui.panel);
        if(wide){TextView title=ui.note("Settings");title.setPadding(ui.dp(8),0,0,ui.dp(12));nav.addView(title);}
        Class<?>[] screens={BridgeActivity.class,MappingActivity.class,DiagnosticsActivity.class,AppearanceActivity.class};
        String[] names={"Bridge controls","Controller mapping","Diagnostics","Appearance"};
        int[] icons={android.R.drawable.ic_media_play,android.R.drawable.ic_menu_sort_by_size,android.R.drawable.ic_menu_info_details,android.R.drawable.ic_menu_preferences};
        for(int i=0;i<screens.length;i++){
            final Class<?> screen=screens[i];TextView item=ui.text(names[i],wide?14:12);item.setGravity(Gravity.CENTER_VERTICAL);item.setPadding(ui.dp(10),ui.dp(12),ui.dp(8),ui.dp(12));
            if(screen.isInstance(this))item.setBackground(ui.background(true));else item.setBackgroundColor(ui.panel);
            if(wide){android.graphics.drawable.Drawable icon=getDrawable(icons[i]).mutate();icon.setTint(screen.isInstance(this)?ui.accent:ui.secondary);icon.setBounds(0,0,ui.dp(20),ui.dp(20));item.setCompoundDrawables(icon,null,null,null);item.setCompoundDrawablePadding(ui.dp(10));}
            item.setFocusable(true);item.setClickable(true);item.setContentDescription("Settings: "+names[i]);
            item.setOnClickListener(v->{if(!screen.isInstance(this))navigateAway(()->{startActivity(new Intent(this,screen));finish();});});
            if(wide)nav.addView(item,new LinearLayout.LayoutParams(-1,-2));else nav.addView(item,new LinearLayout.LayoutParams(0,-2,1));
        }
        if(wide){ScrollView scroll=new ScrollView(this);scroll.addView(nav);shell.addView(scroll,new LinearLayout.LayoutParams(ui.dp(196),-1));shell.addView(content,new LinearLayout.LayoutParams(0,-1,1));}
        else{shell.addView(nav,new LinearLayout.LayoutParams(-1,-2));shell.addView(content,new LinearLayout.LayoutParams(-1,0,1));}
        super.setContentView(shell);
    }
    protected void navigateAway(Runnable action){action.run();}
    @Override public void onBackPressed(){navigateAway(this::finish);}
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { navigateAway(this::finish); return true; }
        return super.onOptionsItemSelected(item);
    }
}
