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
    private LinearLayout toolsRow;
    interface Work { String run() throws Exception; }
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);LinearLayout page=ui.column();page.setPadding(ui.dp(18),ui.dp(4),ui.dp(18),ui.dp(6));
        page.addView(ui.heading("Bridge controls"));ui.space(page,4);
        LinearLayout card=ui.card();page.addView(card);stateLabel=ui.text("Checking bridge status...",18);card.addView(stateLabel);
        LinearLayout actions=new LinearLayout(this);card.addView(actions);
        startButton=ui.button("Start",true);stopButton=ui.button("Stop",false);restartButton=ui.button("Restart",false);
        ui.equal(actions,startButton);ui.equal(actions,stopButton);ui.equal(actions,restartButton);
        startButton.setOnClickListener(v->run(AppRuntime::startAndWait));stopButton.setOnClickListener(v->run(AppRuntime::stopAndWait));restartButton.setOnClickListener(v->run(AppRuntime::restartAndWait));
        status=ui.note("Restart applies the saved mapping. Automatic startup stays in the app-list menu.");page.addView(status);ui.space(page,4);
        page.addView(ui.note("Advanced"));advanced=ui.column();page.addView(advanced);
        action(advanced,"Install/Update Components",()->AppRuntime.setup(this));
        action(advanced,"Refresh bridge status",()->"Bridge status refreshed.");
        action(advanced,"View technical status",()->{String details=AppRuntime.status();runOnUiThread(()->{TextView text=ui.text(details,12);text.setTextIsSelectable(true);ScrollView scroll=new ScrollView(this);scroll.addView(text);new AlertDialog.Builder(this).setTitle("Technical bridge status").setView(scroll).setPositiveButton("Close",null).show();});return "Technical status opened.";});
        action(advanced,"Test virtual gamepad (2 seconds)",AppRuntime::testVirtual);
        Button remove=toolButton(advanced,"Uninstall Bridge Components");
        remove.setOnClickListener(v->{if(!busy)new AlertDialog.Builder(this).setTitle("Uninstall Bridge Components?").setMessage("This stops the app bridge and turns off automatic startup. Your saved app selection and mappings are kept.").setNegativeButton("Cancel",null).setPositiveButton("Uninstall",(d,w)->run(()->{String result=AppRuntime.remove();BootStarter.setEnabled(this,false);return result;})).show();});
        ScrollView scroll=new ScrollView(this);scroll.addView(page);setContentView(scroll);updateControls();
    }
    private Button toolButton(LinearLayout page,String label){
        if(toolsRow!=null&&toolsRow.getChildCount()==2&&"tool-spacer".equals(toolsRow.getChildAt(1).getTag()))toolsRow.removeViewAt(1);
        if(toolsRow==null||toolsRow.getChildCount()==2){
            toolsRow=new LinearLayout(this);toolsRow.setBaselineAligned(false);
            LinearLayout.LayoutParams rowParams=new LinearLayout.LayoutParams(-1,ui.dp(48));rowParams.bottomMargin=ui.dp(2);page.addView(toolsRow,rowParams);
        }
        Button button=ui.button(label,false);button.setBackgroundTintList(null);button.setBackground(ui.background(false));
        button.setTextSize(13);button.setMinHeight(0);button.setMinimumHeight(0);button.setMinWidth(0);button.setMinimumWidth(0);
        button.setPadding(ui.dp(8),0,ui.dp(8),0);button.setMaxLines(2);button.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-1,1);params.setMargins(ui.dp(3),0,ui.dp(3),0);toolsRow.addView(button,params);
        if(toolsRow.getChildCount()==1){
            // Reserve the second column even when the final row has only one action.
            View spacer=new View(this);spacer.setTag("tool-spacer");LinearLayout.LayoutParams blank=new LinearLayout.LayoutParams(0,1,1);blank.setMargins(ui.dp(3),0,ui.dp(3),0);toolsRow.addView(spacer,blank);
        }
        maintenance.add(button);return button;
    }
    private void action(LinearLayout page,String label,Work work){Button button=toolButton(page,label);button.setOnClickListener(v->run(work));}
    @Override protected void onResume(){super.onResume();if(!busy)run(()->"Bridge status checked.");}
    private void updateControls(){
        stateLabel.setText(busy?"Working...":state.equals("RUNNING")?"Bridge running":state.equals("STOPPED")?"Bridge stopped":state.equals("STARTING")?"Bridge is starting...":state.equals("MISSING")?"Components not installed":state.equals("FIRMWARE")?"Firmware bridge detected":"Status unavailable");
        startButton.setEnabled(!busy&&state.equals("STOPPED"));stopButton.setEnabled(!busy&&(state.equals("RUNNING")||state.equals("STARTING")));restartButton.setEnabled(!busy&&(state.equals("RUNNING")||state.equals("STOPPED")));
        for(Button button:new Button[]{startButton,stopButton,restartButton})button.setAlpha(button.isEnabled()?1f:.35f);
        for(Button button:maintenance)button.setEnabled(!busy&&!state.equals("FIRMWARE"));
    }
    private void run(Work work){
        if(busy)return;busy=true;status.setText("Working...");updateControls();
        new Thread(()->{
            String message,newState;
            try{message=work.run();}catch(Exception error){message="Could not complete operation: "+error.getMessage();}
            try{newState=AppRuntime.controlState();}catch(Exception error){newState="UNKNOWN";message+=" Privileged access unavailable; open Diagnostics.";}
            final String result=message,actual=newState;
            runOnUiThread(()->{if(isDestroyed())return;busy=false;state=actual;status.setText(result);updateControls();});
        },"DS4BridgeControls").start();
    }
    @Override protected void navigateAway(Runnable action){if(busy){Toast.makeText(this,"Wait for the bridge operation to finish",Toast.LENGTH_SHORT).show();return;}super.navigateAway(action);}
}
