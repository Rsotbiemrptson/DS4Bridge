package com.odin2.ds4bridgehelper;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

/** Real stop/start/restart and mapping-save integration, restoring user files. */
public final class SettingsSmoke extends Instrumentation {
    private Activity screen;
    private Throwable failure;
    private final Map<String,byte[]> profiles=new LinkedHashMap<>();
    private File files;
    private String originalState;
    private SharedPreferences preferences;
    private int theme;
    private boolean system;
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private Object field(Object target,String name)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
    private void main(Runnable task)throws Exception{runOnMainSync(()->{try{task.run();}catch(Throwable error){failure=error;}});if(failure!=null)throw new Exception("UI check",failure);}
    private View find(View root,String label){
        if(label.equals(root.getContentDescription())||root instanceof TextView&&label.equals(((TextView)root).getText().toString()))return root;
        if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){View result=find(group.getChildAt(i),label);if(result!=null)return result;}}return null;
    }
    private void open(Class<?> type)throws Exception{if(screen!=null)main(()->screen.finish());screen=startActivitySync(new Intent(getTargetContext(),type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();}
    private void waitIdle(Object target)throws Exception{
        boolean[] busy={true};long until=SystemClock.uptimeMillis()+25000;
        while(busy[0]&&SystemClock.uptimeMillis()<until){main(()->{try{busy[0]=(Boolean)field(target,"busy");}catch(Exception error){throw new RuntimeException(error);}});if(busy[0])SystemClock.sleep(100);}
        check(!busy[0],"Operation timed out");
    }
    private void clickField(Object target,String name)throws Exception{main(()->{try{((View)field(target,name)).performClick();}catch(Exception error){throw new RuntimeException(error);}});waitIdle(target);}
    private byte[] bytes(File file)throws IOException{try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))>=0)out.write(buffer,0,n);return out.toByteArray();}}
    private boolean isProfile(String name){return name.equals("profile.conf")||name.matches("controller-[0-9]+-[0-9]+\\.conf");}
    private void snapshot()throws Exception{
        files=getTargetContext().createDeviceProtectedStorageContext().getFilesDir();File[] list=files.listFiles();if(list!=null)for(File file:list)if(isProfile(file.getName()))profiles.put(file.getName(),bytes(file));
        originalState=AppRuntime.controlState();check(originalState.equals("RUNNING")||originalState.equals("STOPPED"),"An installed standalone runtime is required");
        preferences=getTargetContext().getSharedPreferences("PickerActivity",0);theme=preferences.getInt("theme",0);system=preferences.getBoolean("show_system",false);
    }
    private void restore()throws Exception{
        if(files!=null){File[] list=files.listFiles();if(list!=null)for(File file:list)if(isProfile(file.getName())&&!profiles.containsKey(file.getName()))check(file.delete(),"Cannot remove test-created profile");
            for(Map.Entry<String,byte[]> entry:profiles.entrySet())try(FileOutputStream out=new FileOutputStream(new File(files,entry.getKey()))){out.write(entry.getValue());out.getFD().sync();}
        }
        if(preferences!=null)preferences.edit().putInt("theme",theme).putBoolean("show_system",system).commit();
        if(originalState!=null){if(originalState.equals("RUNNING"))AppRuntime.restartAndWait();else if(originalState.equals("STOPPED"))AppRuntime.stopAndWait();}
    }
    private void exercise()throws Exception{
        open(PickerActivity.class);main(()->{View more=find(screen.getWindow().getDecorView(),"More options");check(more!=null,"Menu anchor missing");more.performClick();});waitForIdleSync();
        AccessibilityNodeInfo menu=null;for(int attempt=0;attempt<20&&menu==null;attempt++){menu=getUiAutomation().getRootInActiveWindow();if(menu==null)SystemClock.sleep(100);}check(menu!=null,"Menu not accessible");
        check(menu.findAccessibilityNodeInfosByText("Start bridge after reboot").isEmpty(),"Startup remains in main menu");
        check(!menu.findAccessibilityNodeInfosByText("About").isEmpty(),"About menu option missing");
        check(!menu.findAccessibilityNodeInfosByText("Settings").isEmpty(),"Settings menu option missing");
        check(!menu.findAccessibilityNodeInfosByText("Show system apps").isEmpty(),"System apps menu option missing");
        for(String removed:new String[]{"Controller mapping","Diagnostics","Theme","Bridge controls"})check(menu.findAccessibilityNodeInfosByText(removed).isEmpty(),"Extra overflow option: "+removed);
        open(BridgeActivity.class);BridgeActivity bridge=(BridgeActivity)screen;waitIdle(bridge);
        main(()->check(find(screen.getWindow().getDecorView(),"Start bridge after reboot")!=null,"Startup setting missing"));
        if(AppRuntime.controlState().equals("RUNNING"))clickField(bridge,"stopButton");
        check(AppRuntime.controlState().equals("STOPPED"),"Stop did not stop app runtime");
        main(()->{try{check(((Button)field(bridge,"startButton")).isEnabled()&&!((Button)field(bridge,"stopButton")).isEnabled(),"Stopped-state buttons incorrect");}catch(Exception error){throw new RuntimeException(error);}});
        clickField(bridge,"startButton");check(AppRuntime.controlState().equals("RUNNING"),"Start did not start runtime");
        main(()->{try{check(!((Button)field(bridge,"startButton")).isEnabled()&&((Button)field(bridge,"stopButton")).isEnabled(),"Running-state buttons incorrect");}catch(Exception error){throw new RuntimeException(error);}});
        String before=RootHooks.execute("cat "+AppRuntime.STATE+"/bridge.pid");clickField(bridge,"restartButton");
        check(AppRuntime.controlState().equals("RUNNING")&&!before.equals(RootHooks.execute("cat "+AppRuntime.STATE+"/bridge.pid")),"Restart did not replace app process");
        open(MappingActivity.class);MappingActivity mapping=(MappingActivity)screen;
        clickField(mapping,"saveButton");
        check(AppRuntime.controlState().equals("RUNNING"),"Save-and-restart did not leave bridge running");
        main(()->{try{check(!((Boolean)field(mapping,"dirty")),"Saved mapping left dirty");check(((TextView)field(mapping,"status")).getText().toString().startsWith("Bridge started."),"Mapping restart not confirmed");}catch(Exception error){throw new RuntimeException(error);}});
        open(AppearanceActivity.class);
        main(()->{View root=screen.getWindow().getDecorView();for(String option:new String[]{"Follow system","Light","Dark"})check(find(root,option)!=null,"Theme missing: "+option);check(find(root,"Show system apps")==null,"System-app toggle duplicated in Appearance");});
        ActivityMonitor monitor=addMonitor(AppearanceActivity.class.getName(),null,false);
        main(()->find(screen.getWindow().getDecorView(),"Light").performClick());
        Activity changed=monitor.waitForActivityWithTimeout(3000);removeMonitor(monitor);check(changed!=null,"Light theme did not recreate settings");screen=changed;waitForIdleSync();check(!((BridgeScreen)screen).ui.dark,"Light theme did not apply");
        monitor=addMonitor(AppearanceActivity.class.getName(),null,false);
        main(()->find(screen.getWindow().getDecorView(),"Dark").performClick());
        changed=monitor.waitForActivityWithTimeout(3000);removeMonitor(monitor);check(changed!=null,"Dark theme did not recreate settings");screen=changed;waitForIdleSync();check(((BridgeScreen)screen).ui.dark,"Dark theme did not apply");
    }
    @Override public void onStart(){Bundle result=new Bundle();Throwable error=null;
        try{snapshot();exercise();}catch(Throwable problem){error=problem;}
        finally{try{if(screen!=null)main(()->screen.finish());restore();}catch(Throwable problem){if(error==null)error=problem;else error.addSuppressed(problem);}}
        if(error==null){result.putString("stream","PASS: three-item menu, settings sidebar, state-aware Stop/Start/Restart, save-and-restart mapping, Appearance; original profiles/preferences/runtime state restored.\n");finish(0,result);}
        else{result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(error));finish(1,result);}
    }
}
