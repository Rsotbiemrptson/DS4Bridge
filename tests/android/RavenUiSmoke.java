package com.odin2.ds4bridgehelper;

import android.app.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

/** UI-only acceptance and screenshots; no component setup or runtime controls. */
public final class RavenUiSmoke extends Instrumentation {
    private Activity screen;private Throwable failure;
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private void main(Runnable work)throws Exception{runOnMainSync(()->{try{work.run();}catch(Throwable t){failure=t;}});Throwable problem=failure;failure=null;if(problem!=null)throw new Exception(problem);}
    private View find(View root,String name){if(name.equals(root.getContentDescription())||root instanceof TextView&&name.equals(((TextView)root).getText().toString()))return root;if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){View found=find(((ViewGroup)root).getChildAt(i),name);if(found!=null)return found;}return null;}
    private void open(Class<?> type,int orientation)throws Exception{
        if(screen!=null)main(()->screen.finish());screen=startActivitySync(new Intent(getTargetContext(),type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
        ActivityMonitor monitor=addMonitor(type.getName(),null,false);
        int desired=orientation==ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE?Configuration.ORIENTATION_LANDSCAPE:Configuration.ORIENTATION_PORTRAIT;
        boolean rotate=screen.getResources().getConfiguration().orientation!=desired;
        main(()->screen.setRequestedOrientation(orientation));
        if(rotate){Activity next=monitor.waitForActivityWithTimeout(5000);check(next!=null,"Rotation did not recreate "+type.getSimpleName());screen=next;}removeMonitor(monitor);waitForIdleSync();SystemClock.sleep(600);
    }
    private void capture(String label)throws Exception{
        try(ParcelFileDescriptor fd=getUiAutomation().executeShellCommand("screencap -p /sdcard/raven-gamepad-"+label+".png");InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)){byte[] buffer=new byte[1024];while(in.read(buffer)!=-1){}}
    }
    private Activity child()throws Exception{java.lang.reflect.Field field=PickerActivity.class.getDeclaredField("activeChild");field.setAccessible(true);return (Activity)field.get(screen);}
    private Object childField(String name)throws Exception{Activity child=child();java.lang.reflect.Field field=child.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(child);}
    private void mappingGuard(boolean recreate)throws Exception{
        main(()->{try{
            MappingActivity mapping=(MappingActivity)child();java.lang.reflect.Method learn=MappingActivity.class.getDeclaredMethod("learn",int.class);learn.setAccessible(true);learn.invoke(mapping,308);
            View capture=(View)childField("capturePane");Button cancel=(Button)find(capture,"Cancel");check(cancel!=null,"Learning Cancel missing");SettingsUi ui=new SettingsUi(mapping);check(((android.graphics.drawable.GradientDrawable)cancel.getBackground()).getColor().getDefaultColor()==ui.accent,"Cancel background not accented");cancel.performClick();check((Integer)childField("waiting")==-1,"Cancel left capture active");
            learn.invoke(mapping,308);InputDevice device=(InputDevice)childField("selected");long now=SystemClock.uptimeMillis();screen.dispatchKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_LEFT,0,0,device.getId(),546,0,InputDevice.SOURCE_GAMEPAD));check(((Map<Integer,Integer>)childField("keys")).get(308)==546,"Embedded mapping did not receive controller button");
        }catch(Exception e){throw new RuntimeException(e);}});waitForIdleSync();
        if(recreate){ActivityMonitor monitor=addMonitor(PickerActivity.class.getName(),null,false);main(()->screen.recreate());Activity next=monitor.waitForActivityWithTimeout(5000);removeMonitor(monitor);check(next!=null,"Host did not recreate");screen=next;waitForIdleSync();main(()->{try{check((Boolean)childField("dirty")&&((Map<Integer,Integer>)childField("keys")).get(308)==546,"Embedded mapping draft lost on recreation");}catch(Exception e){throw new RuntimeException(e);}});}
        main(()->{try{Activity mapping=child();((PickerActivity)screen).openPane(DiagnosticsActivity.class);check(child()==mapping,"Unsaved guard did not hold the mapping pane");}catch(Exception e){throw new RuntimeException(e);}});waitForIdleSync();
        android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();boolean clicked=false;
        for(android.view.accessibility.AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText("Discard"))if(node.getText()!=null&&node.getText().toString().equalsIgnoreCase("Discard")){clicked=node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK);break;}
        check(clicked,"Discard prompt missing");waitForIdleSync();SystemClock.sleep(400);
    }
    private void pane(Class<?> type)throws Exception{main(()->((PickerActivity)screen).openPane(type));waitForIdleSync();SystemClock.sleep(900);main(()->{try{java.lang.reflect.Field field=PickerActivity.class.getDeclaredField("activeType");field.setAccessible(true);check(field.get(screen)==type,"Pane navigation did not complete: "+type);}catch(Exception e){throw new RuntimeException(e);}});}
    private Map<String,byte[]> files()throws Exception{
        Map<String,byte[]> result=new TreeMap<>();File[] list=getTargetContext().createDeviceProtectedStorageContext().getFilesDir().listFiles();
        if(list!=null)for(File file:list)if(file.getName().equals("targets.conf")||file.getName().equals("profile.conf")||file.getName().startsWith("controller-")&&file.getName().endsWith(".conf"))try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);result.put(file.getName(),out.toByteArray());}return result;
    }
    @Override public void onStart(){Bundle result=new Bundle();Throwable error=null;Context c=getTargetContext();SharedPreferences prefs=c.getSharedPreferences("PickerActivity",0);Map<String,?> oldPrefs=new HashMap<>(prefs.getAll());Map<String,byte[]> before=null;boolean boot=BootStarter.isEnabled(c);String pid=null;
        try{
            before=files();RootHooks.requireRoot();pid=RootHooks.execute("pidof odin_ds4_bridge");
            check("Raven Gamepad".contentEquals(c.getPackageManager().getApplicationLabel(c.getApplicationInfo())),"App label not updated");
            for(int theme:new int[]{2,1}){
                prefs.edit().putInt("theme",theme).commit();
                for(int orientation:new int[]{ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}){
                    String tag=(orientation==ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE?"landscape":"portrait")+"-"+(theme==2?"dark":"light");
                    open(PickerActivity.class,orientation);
                    View nav=screen.getWindow().getDecorView().findViewWithTag("raven-navigation");
                    main(()->{check(find(screen.getWindow().getDecorView(),"Navigate: Settings")==null,"Settings tab remains");check(find(screen.getWindow().getDecorView(),"Navigate: Apps").isSelected(),"Apps inactive");check(find(screen.getWindow().getDecorView(),"Navigate: Appearance").getVisibility()==View.GONE,"Settings tabs visible on Apps");SettingsUi ui=new SettingsUi(screen);check(ui.surface==(ui.dark?0xff1f2228:0xffe9eaf0)&&ui.panel==(ui.dark?0xff272b32:0xffffffff),"Backup colors differ");check(screen.getResources().getColor(screen.getResources().getIdentifier("icon_background","color",screen.getPackageName()))==0xff1f2228,"Icon background differs from Backup");});capture("apps-"+tag);
                    pane(AppearanceActivity.class);
                    main(()->{check(nav==screen.getWindow().getDecorView().findViewWithTag("raven-navigation"),"Sidebar rebuilt");for(String title:new String[]{"Apps","Appearance","Bridge controls","Controller mapping","Diagnostics"})check(find(screen.getWindow().getDecorView(),"Navigate: "+title).getVisibility()==View.VISIBLE,"Tab hidden: "+title);check(find(screen.getWindow().getDecorView(),"Back")==null&&find(screen.getWindow().getDecorView(),"Return to Apps")==null,"Back arrows remain");check(find(screen.getWindow().getDecorView(),"Accent color")!=null,"Appearance missing");});capture("appearance-"+tag);
                    pane(BridgeActivity.class);SystemClock.sleep(600);
                    main(()->{check(nav==screen.getWindow().getDecorView().findViewWithTag("raven-navigation"),"Sidebar rebuilt at Bridge");View expand=find(screen.getWindow().getDecorView(),"Expand Advanced");check(expand!=null,"Advanced not initially collapsed");expand.performClick();check(find(screen.getWindow().getDecorView(),"Collapse Advanced")!=null,"Advanced did not expand");});capture("bridge-"+tag);
                    main(()->find(screen.getWindow().getDecorView(),"Collapse Advanced").performClick());
                    pane(MappingActivity.class);capture("mapping-"+tag);if(theme==2&&orientation==ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)mappingGuard(false);
                    pane(DiagnosticsActivity.class);
                    main(()->{try{check(nav==screen.getWindow().getDecorView().findViewWithTag("raven-navigation"),"Sidebar rebuilt at Diagnostics");Activity child=child();java.lang.reflect.Field selected=DiagnosticsActivity.class.getDeclaredField("selected");selected.setAccessible(true);InputDevice device=(InputDevice)selected.get(child);check(device!=null,"No embedded controller selected");long now=SystemClock.uptimeMillis();screen.dispatchKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BUTTON_A,0,0,device.getId(),304,0,InputDevice.SOURCE_GAMEPAD));java.lang.reflect.Field buttons=DiagnosticsActivity.class.getDeclaredField("buttons");buttons.setAccessible(true);check(((Map<String,TextView>)buttons.get(child)).get("A").isActivated(),"Controller events not forwarded to embedded pane");screen.dispatchKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BUTTON_A,0,0,device.getId(),304,0,InputDevice.SOURCE_GAMEPAD));}catch(Exception e){throw new RuntimeException(e);}});capture("diagnostics-"+tag);
                    pane(PickerActivity.class);
                    main(()->{check(nav==screen.getWindow().getDecorView().findViewWithTag("raven-navigation"),"Sidebar rebuilt on Apps return");check(find(screen.getWindow().getDecorView(),"Navigate: Appearance").getVisibility()==View.GONE,"Apps did not hide settings tabs");});
                }
            }
            open(PickerActivity.class,ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);pane(MappingActivity.class);mappingGuard(true);
        }catch(Throwable t){error=t;}
        finally{
            try{
                if(screen!=null)main(()->{screen.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);screen.finish();});
                SharedPreferences.Editor edit=prefs.edit();if(oldPrefs.containsKey("theme"))edit.putInt("theme",(Integer)oldPrefs.get("theme"));else edit.remove("theme");edit.commit();
                check(oldPrefs.equals(prefs.getAll()),"UI check changed preferences");check(boot==BootStarter.isEnabled(c),"UI check changed reboot startup");
                if(before!=null){Map<String,byte[]> after=files();check(before.keySet().equals(after.keySet()),"Profile files changed");for(String name:before.keySet())check(Arrays.equals(before.get(name),after.get(name)),"Profile changed: "+name);}
                if(pid!=null)check(pid.equals(RootHooks.execute("pidof odin_ds4_bridge")),"Runtime process changed");
            }catch(Throwable t){if(error==null)error=t;else error.addSuppressed(t);}
        }
        if(error==null){result.putString("stream","PASS: Raven navigation with persistent sidebar, Apps-only main navigation, Appearance-first Settings, collapsible Advanced, embedded controller events, Backup colors/icon background and dark/light landscape/portrait screenshots; preferences, selections, mapping files, boot setting and runtime PID unchanged.\n");finish(0,result);}
        else{result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(error));finish(1,result);}
    }
}
