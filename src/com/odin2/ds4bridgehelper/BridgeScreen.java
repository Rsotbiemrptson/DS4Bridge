package com.odin2.ds4bridgehelper;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

/** Raven tools with shared sidebar/bottom navigation and a compact Back header. */
public class BridgeScreen extends Activity {
    SettingsUi ui;
    final Bundle savePaneState(){Bundle state=new Bundle();onSaveInstanceState(state);return state;}
    final Bundle restorePaneState(Bundle saved){Bundle pane=getIntent().getBundleExtra("pane_state");if(pane!=null){if(saved==null)saved=new Bundle();saved.putAll(pane);getIntent().removeExtra("pane_state");}return saved;}
    private int appliedTheme;
    @Override public void onCreate(Bundle saved){
        appliedTheme=getSharedPreferences("PickerActivity",MODE_PRIVATE).getInt("theme",0);
        boolean dark=appliedTheme==2||(appliedTheme==0&&(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);
        setTheme(getResources().getIdentifier(dark?"RavenDark":"RavenLight","style",getPackageName()));
        super.onCreate(saved);ui=new SettingsUi(this);
        getWindow().setStatusBarColor(ui.background);getWindow().setNavigationBarColor(ui.background);
        getWindow().getDecorView().setSystemUiVisibility(ui.dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }
    @Override protected void onResume(){super.onResume();if(appliedTheme!=getSharedPreferences("PickerActivity",MODE_PRIVATE).getInt("theme",0)||ui.accent!=(getSharedPreferences("PickerActivity",0).getInt("accentColor",0xff0070cc)|0xff000000))recreate();}
    @Override public void setContentView(View content){
        if(getParent() instanceof PickerActivity){
            View body=content instanceof ScrollView?((ScrollView)content).getChildAt(0):content;
            removePaneHeading(body);
            super.setContentView(content);return;
        }
        LinearLayout shell=ui.column();shell.setBackgroundColor(ui.background);
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(ui.dp(12),ui.dp(4),ui.dp(16),ui.dp(4));header.setTag("raven-header");
        boolean wide=getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        if(!wide)header.addView(ui.logo(),new LinearLayout.LayoutParams(ui.dp(36),ui.dp(36)));
        TextView brand=ui.text(wide?"Settings":"Raven Gamepad",25);brand.setTypeface(null,android.graphics.Typeface.BOLD);brand.setPadding(ui.dp(10),0,0,0);header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        Button menu=ui.button("\u22ee",false);menu.setTextSize(26);menu.setPadding(0,0,0,0);menu.setContentDescription("More options");menu.setOnClickListener(v->showOptions(menu));header.addView(menu,new LinearLayout.LayoutParams(ui.dp(48),ui.dp(44)));
        shell.addView(header,new LinearLayout.LayoutParams(-1,ui.dp(52)));shell.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        super.setContentView(RavenShell.wrap(this,ui,shell,this instanceof BridgeActivity?0:this instanceof MappingActivity?1:this instanceof DiagnosticsActivity?2:3,target->navigateAway(()->{
            startActivity(new Intent(this,target).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));finish();
        })));
    }
    private boolean removePaneHeading(View parent){
        if(!(parent instanceof ViewGroup))return false;ViewGroup group=(ViewGroup)parent;
        for(int i=0;i<group.getChildCount();i++){View child=group.getChildAt(i);if(child instanceof TextView&&!(child instanceof Button)){String title=((TextView)child).getText().toString();if(title.equals("Appearance")||title.equals("Bridge controls")||title.equals("Controller mapping")||title.equals("Diagnostics")){group.removeViewAt(i);return true;}}if(removePaneHeading(child))return true;}return false;
    }
    private void showOptions(View anchor){
        PopupMenu popup=new PopupMenu(this,anchor);android.content.SharedPreferences prefs=getSharedPreferences("PickerActivity",0);
        popup.getMenu().add(0,1,0,"Show system apps").setCheckable(true).setChecked(prefs.getBoolean("show_system",false));popup.getMenu().add(0,2,1,"Settings");popup.getMenu().add(0,3,2,"About");
        popup.setOnMenuItemClickListener(item->{if(item.getItemId()==1)prefs.edit().putBoolean("show_system",!prefs.getBoolean("show_system",false)).apply();else if(item.getItemId()==3)AboutDialog.show(this);else if(!(this instanceof AppearanceActivity))navigateAway(()->{startActivity(new Intent(this,AppearanceActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));finish();});return true;});popup.show();
    }
    protected void navigateAway(Runnable action){action.run();}
    @Override public void recreate(){if(getParent() instanceof PickerActivity)getParent().recreate();else super.recreate();}
    @Override public void onBackPressed(){if(getParent() instanceof PickerActivity)((PickerActivity)getParent()).openPane(PickerActivity.class);else navigateAway(this::finish);}
    @Override public boolean onOptionsItemSelected(MenuItem item){if(item.getItemId()==android.R.id.home){navigateAway(this::finish);return true;}return super.onOptionsItemSelected(item);}
}
