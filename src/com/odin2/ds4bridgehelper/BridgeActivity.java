package com.odin2.ds4bridgehelper;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.*;
import android.view.View;
import java.util.*;

public final class BridgeActivity extends BridgeScreen {
    private TextView stateLabel,status;
    private boolean busy;
    private String state="UNKNOWN";
    private Button startButton,stopButton,restartButton;
    private final List<Button> maintenance=new ArrayList<>();
    private LinearLayout advanced;
    private LinearLayout advancedBody;
    private LinearLayout toolsRow;
    interface Work { String run() throws Exception; }
    @Override public void onCreate(Bundle saved){
        saved=restorePaneState(saved);
        super.onCreate(saved);LinearLayout page=ui.column();page.setPadding(ui.dp(18),ui.dp(4),ui.dp(18),ui.dp(6));
        page.addView(ui.heading("Bridge controls"));ui.space(page,4);
        LinearLayout card=ui.card();page.addView(card);stateLabel=ui.text("Status: Checking...",18);card.addView(stateLabel);
        LinearLayout actions=new LinearLayout(this);card.addView(actions);
        startButton=ui.button("Start",true);stopButton=ui.button("Stop",false);restartButton=ui.button("Restart",false);
        ui.equal(actions,startButton);ui.equal(actions,stopButton);ui.equal(actions,restartButton);
        startButton.setOnClickListener(v->run(AppRuntime::startAndWait));stopButton.setOnClickListener(v->run(AppRuntime::stopAndWait));restartButton.setOnClickListener(v->run(AppRuntime::restartAndWait));
        Switch boot=new Switch(this);boot.setText("Start bridge after reboot");boot.setTextColor(ui.foreground);boot.setTextSize(14);boot.setMinHeight(ui.dp(42));boot.setChecked(BootStarter.isEnabled(this));android.content.res.ColorStateList tint=new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{ui.accentText,ui.secondary});boot.setThumbTintList(tint);boot.setTrackTintList(tint);boot.setOnCheckedChangeListener((v,checked)->BootStarter.setEnabled(this,checked));card.addView(boot);
        status=ui.note("");status.setVisibility(View.GONE);page.addView(status);ui.space(page,10);
        advanced=ui.card();page.addView(advanced);Button expand=ui.button("Advanced  ▾",false);expand.setTextSize(18);expand.setGravity(android.view.Gravity.START|android.view.Gravity.CENTER_VERTICAL);expand.setPadding(0,0,0,0);expand.setContentDescription("Expand Advanced");advanced.addView(expand,new LinearLayout.LayoutParams(-1,ui.dp(44)));
        advancedBody=ui.column();advanced.addView(advancedBody);boolean expanded=saved!=null&&saved.getBoolean("advanced_expanded",false);advancedBody.setVisibility(expanded?View.VISIBLE:View.GONE);expand.setText(expanded?"Advanced  ▴":"Advanced  ▾");expand.setContentDescription(expanded?"Collapse Advanced":"Expand Advanced");expand.setOnClickListener(v->{boolean show=advancedBody.getVisibility()!=View.VISIBLE;advancedBody.setVisibility(show?View.VISIBLE:View.GONE);expand.setText(show?"Advanced  ▴":"Advanced  ▾");expand.setContentDescription(show?"Collapse Advanced":"Expand Advanced");});
        action(advancedBody,"Install/Update Components","Install the bundled bridge or update existing components.",()->AppRuntime.setup(this));
        action(advancedBody,"Refresh bridge status","Read the current bridge status without restarting it.",()->null);
        action(advancedBody,"View technical status","Show component versions and detailed runtime information.",()->{String details=AppRuntime.status();runOnUiThread(()->{TextView text=ui.text(details,12);text.setTextIsSelectable(true);ScrollView scroll=new ScrollView(this);scroll.addView(text);ui.dialog().setTitle("Technical bridge status").setView(scroll).setPositiveButton("Close",null).show();});return null;});
        action(advancedBody,"Test virtual gamepad (2 seconds)","Send a short test input to verify the virtual controller.",AppRuntime::testVirtual);
        Button remove=toolButton(advancedBody,"Uninstall Bridge Components","Stop and remove installed bridge components. Keep saved app choices and mappings.");
        remove.setOnClickListener(v->{if(!busy)new AlertDialog.Builder(this).setTitle("Uninstall Bridge Components?").setMessage("This stops the app bridge and turns off automatic startup. Your saved app selection and mappings are kept.").setNegativeButton("Cancel",null).setPositiveButton("Uninstall",(d,w)->run(()->{String result=AppRuntime.remove();BootStarter.setEnabled(this,false);return result;})).show();});
        ScrollView scroll=new ScrollView(this);scroll.addView(page);setContentView(scroll);updateControls();
    }
    private Button toolButton(LinearLayout page,String label,String description){
        if(toolsRow!=null&&toolsRow.getChildCount()==2&&"tool-spacer".equals(toolsRow.getChildAt(1).getTag()))toolsRow.removeViewAt(1);
        if(toolsRow==null||toolsRow.getChildCount()==2){
            toolsRow=new LinearLayout(this);toolsRow.setBaselineAligned(false);
            LinearLayout.LayoutParams rowParams=new LinearLayout.LayoutParams(-1,-2);rowParams.bottomMargin=ui.dp(8);page.addView(toolsRow,rowParams);
        }
        LinearLayout item=ui.column();item.setPadding(ui.dp(3),0,ui.dp(3),0);
        Button button=ui.button(label,false);button.setBackgroundTintList(null);button.setBackground(ui.shape(ui.surface,24));
        button.setTextSize(13);button.setMinHeight(0);button.setMinimumHeight(0);button.setMinWidth(0);button.setMinimumWidth(0);
        button.setPadding(ui.dp(8),0,ui.dp(8),0);button.setMaxLines(2);button.setGravity(android.view.Gravity.CENTER);
        item.addView(button,new LinearLayout.LayoutParams(-1,ui.dp(40)));TextView note=ui.note(description);note.setTextSize(12);note.setPadding(ui.dp(6),ui.dp(4),ui.dp(6),0);item.addView(note);LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-2,1);toolsRow.addView(item,params);
        if(toolsRow.getChildCount()==1){
            // Reserve the second column even when the final row has only one action.
            View spacer=new View(this);spacer.setTag("tool-spacer");LinearLayout.LayoutParams blank=new LinearLayout.LayoutParams(0,1,1);toolsRow.addView(spacer,blank);
        }
        maintenance.add(button);return button;
    }
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putBoolean("advanced_expanded",advancedBody!=null&&advancedBody.getVisibility()==View.VISIBLE);}
    private void action(LinearLayout page,String label,String description,Work work){Button button=toolButton(page,label,description);button.setOnClickListener(v->run(work));}
    @Override protected void onResume(){super.onResume();if(!busy)run(()->null);}
    private void updateControls(){
        stateLabel.setText("Status: "+(busy?"Working...":state.equals("RUNNING")?"Running":state.equals("STOPPED")?"Stopped":state.equals("STARTING")?"Starting...":state.equals("MISSING")?"Components not installed":state.equals("FIRMWARE")?"Firmware bridge detected":"Unavailable"));
        startButton.setEnabled(!busy&&state.equals("STOPPED"));stopButton.setEnabled(!busy&&(state.equals("RUNNING")||state.equals("STARTING")));restartButton.setEnabled(!busy&&(state.equals("RUNNING")||state.equals("STOPPED")));
        for(Button button:new Button[]{startButton,stopButton,restartButton})button.setAlpha(button.isEnabled()?1f:.35f);
        for(Button button:maintenance)button.setEnabled(!busy&&!state.equals("FIRMWARE"));
    }
    private void run(Work work){
        if(busy)return;busy=true;status.setText("");status.setVisibility(View.GONE);updateControls();
        new Thread(()->{
            String message,newState;
            try{message=work.run();}catch(Exception error){message="Could not complete operation: "+error.getMessage();}
            try{newState=AppRuntime.controlState();}catch(Exception error){newState="UNKNOWN";message=(message==null?"":message+" ")+"Privileged access unavailable; open Diagnostics.";}
            final String result=message,actual=newState;
            runOnUiThread(()->{if(isDestroyed())return;busy=false;state=actual;status.setText(result==null?"":result);status.setVisibility(result==null||result.isEmpty()?View.GONE:View.VISIBLE);updateControls();});
        },"DS4BridgeControls").start();
    }
    @Override protected void navigateAway(Runnable action){if(busy){Toast.makeText(this,"Wait for the bridge operation to finish",Toast.LENGTH_SHORT).show();return;}super.navigateAway(action);}
}
