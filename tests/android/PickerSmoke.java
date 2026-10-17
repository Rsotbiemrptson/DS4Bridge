package com.odin2.ds4bridgehelper;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

/** Exercises actual installed-app selection, restoring preferences and target bytes. */
public final class PickerSmoke extends Instrumentation {
    private PickerActivity activity;
    private Throwable failure;
    private SharedPreferences preferences;
    private int oldTheme;
    private boolean oldSystem,existed;
    private byte[] original;
    private File target;
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private Object field(Object obj,String name)throws Exception{Field f=obj.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(obj);}
    private void main(Runnable work)throws Exception{runOnMainSync(()->{try{work.run();}catch(Throwable error){failure=error;}});if(failure!=null)throw new Exception("Picker regression",failure);}
    private CheckBox checkbox(View root){if(root instanceof CheckBox)return (CheckBox)root;if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){CheckBox result=checkbox(group.getChildAt(i));if(result!=null)return result;}}return null;}
    private byte[] read(File file)throws Exception{try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))>=0)out.write(buffer,0,n);return out.toByteArray();}}
    private String selectedPackage;
    private boolean selectedAfterEdit;
    @SuppressWarnings("unchecked") private void exercise()throws Exception{
        SettingsUi ui=(SettingsUi)field(activity,"ui");check((Integer)field(activity,"background")==ui.background&&(Integer)field(activity,"accent")==ui.accent,"Picker palette differs from Settings");
        List<?> all=(List<?>)field(activity,"choices");check(!all.isEmpty(),"Installed apps missing");Object chosen=all.get(0);
        for(Object choice:all)if(field(choice,"pkg").equals("com.playstation.remoteplay"))chosen=choice;
        selectedPackage=(String)field(chosen,"pkg");String label=(String)field(chosen,"label");
        for(Object choice:all){String pkg=(String)field(choice,"pkg");activity.getPackageManager().getApplicationInfo(pkg,0);check(!pkg.equals(activity.getPackageName()),"Bridge includes itself");}
        EditText search=(EditText)field(activity,"search");
        java.lang.reflect.Method show=PickerActivity.class.getDeclaredMethod("showOptions",View.class);show.setAccessible(true);show.invoke(activity,search);
        PopupMenu popup=(PopupMenu)field(activity,"optionsPopup");Menu menu=popup.getMenu();
        check(menu.size()==3&&menu.getItem(0).getTitle().equals("Show system apps")&&menu.getItem(1).getTitle().equals("Start bridge after reboot")&&menu.getItem(2).getTitle().equals("Settings"),"Main menu options/order incorrect");
        boolean systemBefore=preferences.getBoolean("show_system",false);
        menu.performIdentifierAction(1,0);check(preferences.getBoolean("show_system",false)!=systemBefore&&menu.findItem(1).isChecked()!=systemBefore,"System-app menu toggle did not persist/update");
        menu.performIdentifierAction(1,0);check(preferences.getBoolean("show_system",false)==systemBefore,"System-app menu toggle did not restore");popup.dismiss();
        search.setText(label.toUpperCase(Locale.ROOT));List<?> visible=(List<?>)field(activity,"visible");check(!visible.isEmpty(),"Name search failed");
        search.setText(selectedPackage);visible=(List<?>)field(activity,"visible");check(!visible.isEmpty()&&field(visible.get(0),"pkg").equals(selectedPackage),"Package search failed");
        ListView list=(ListView)field(activity,"list");BaseAdapter adapter=(BaseAdapter)field(activity,"adapter");
        Set<String> selection=(Set<String>)field(activity,"selected");boolean initial=selection.contains(selectedPackage);
        View row=adapter.getView(0,null,list);CheckBox check=checkbox(row);check(check!=null,"App selection checkbox missing");check.performClick();
        check(selection.contains(selectedPackage)!=initial,"Checkbox did not change selection");
        ((Button)field(activity,"saveButton")).performClick();
        Set<String> saved=new HashSet<>();for(String pkg:new String(read(target),java.nio.charset.StandardCharsets.UTF_8).split("\n"))if(!pkg.isEmpty())saved.add(pkg);
        check(saved.equals(selection),"Save did not persist selected packages");
        list.performItemClick(adapter.getView(0,null,list),0,0);check(selection.contains(selectedPackage)==initial,"Row click did not toggle selection");
        search.setText("no-such-ds4-app-893742");check(((List<?>)field(activity,"visible")).isEmpty(),"No-results filter failed");
        preferences.edit().putBoolean("show_system",false).commit();search.setText("");
        for(Object choice:(List<?>)field(activity,"visible"))check(!(Boolean)field(choice,"system"),"System apps not hidden");
        preferences.edit().putBoolean("show_system",true).commit();search.setText(selectedPackage);search.setText("");boolean foundSystem=false;
        for(Object choice:(List<?>)field(activity,"visible"))if((Boolean)field(choice,"system"))foundSystem=true;check(foundSystem,"System apps not shown");
        search.setText(selectedPackage);selectedAfterEdit=selection.contains(selectedPackage);
    }
    private void recreate()throws Exception{
        ActivityMonitor monitor=addMonitor(PickerActivity.class.getName(),null,false);main(()->activity.recreate());PickerActivity next=(PickerActivity)monitor.waitForActivityWithTimeout(3000);removeMonitor(monitor);check(next!=null,"Picker did not recreate");activity=next;waitForIdleSync();
    }
    @Override public void onStart(){Bundle result=new Bundle();Throwable error=null;
        try{
            preferences=getTargetContext().getSharedPreferences("PickerActivity",0);oldTheme=preferences.getInt("theme",0);oldSystem=preferences.getBoolean("show_system",false);
            target=new File(getTargetContext().createDeviceProtectedStorageContext().getFilesDir(),"targets.conf");existed=target.exists();original=existed?read(target):null;
            activity=(PickerActivity)startActivitySync(new Intent(getTargetContext(),PickerActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
            main(()->{try{exercise();}catch(Exception exception){throw new RuntimeException(exception);}});
            preferences.edit().putInt("theme",1).commit();recreate();
            main(()->{try{
                check(!((SettingsUi)field(activity,"ui")).dark,"Light theme did not apply");
                check(((EditText)field(activity,"search")).getText().toString().equals(selectedPackage),"Search lost on recreation");
                check(((Set<?>)field(activity,"selected")).contains(selectedPackage)==selectedAfterEdit,"Unsaved selection lost on recreation");
            }catch(Exception exception){throw new RuntimeException(exception);}});
            preferences.edit().putInt("theme",2).commit();recreate();main(()->{try{check(((SettingsUi)field(activity,"ui")).dark,"Dark theme did not apply");}catch(Exception exception){throw new RuntimeException(exception);}});
        }catch(Throwable problem){error=problem;}
        finally{
            try{if(activity!=null)runOnMainSync(()->activity.finish());if(preferences!=null)preferences.edit().putInt("theme",oldTheme).putBoolean("show_system",oldSystem).commit();
                if(target!=null){if(existed){try(FileOutputStream out=new FileOutputStream(target)){out.write(original);out.getFD().sync();}}else if(target.exists())check(target.delete(),"Cannot remove test-created target list");}
            }catch(Throwable problem){if(error==null)error=problem;else error.addSuppressed(problem);}
        }
        if(error==null){result.putString("stream","PASS: shared picker styling, installed-only rows, name/package search, checkbox/row selection, save, system-app filter, light/dark theme and recreation; original app targets/preferences restored.\n");finish(0,result);}
        else{result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(error));finish(1,result);}
    }
}
