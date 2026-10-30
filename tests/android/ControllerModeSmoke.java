package com.odin2.ds4bridgehelper;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.InputDevice;
import java.io.*;
import java.util.*;

/** Real OEM mode cycle; restores initial stock settings, forwarding preference and saved files. */
public final class ControllerModeSmoke extends Instrumentation {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    private byte[] read(File f)throws Exception{try(InputStream in=new FileInputStream(f);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}}
    private Map<String,byte[]> saved(Context c)throws Exception{
        Map<String,byte[]> files=new TreeMap<>();for(File f:c.createDeviceProtectedStorageContext().getFilesDir().listFiles())if(f.getName().equals("targets.conf")||f.getName().equals("profile.conf")||f.getName().matches("controller-[0-9]+-[0-9]+\\.conf"))files.put(f.getName(),read(f));return files;
    }
    private void restorePrefs(Context c,Map<String,?> old){SharedPreferences.Editor e=ControllerMode.prefs(c).edit().clear();for(Map.Entry<String,?> item:old.entrySet()){if(item.getValue() instanceof Boolean)e.putBoolean(item.getKey(),(Boolean)item.getValue());else if(item.getValue() instanceof Integer)e.putInt(item.getKey(),(Integer)item.getValue());}check(e.commit(),"Cannot restore forwarding preference");}
    private String status()throws Exception{return RootHooks.output("grep 'Forwarding ' "+AppRuntime.STATE+"/runtime.log | tail -n 1");}
    private void waitForward(boolean active)throws Exception{long until=SystemClock.uptimeMillis()+7000;String last="";while(SystemClock.uptimeMillis()<until){last=status();if(last.contains(active?"Forwarding enabled":"Forwarding disabled"))return;SystemClock.sleep(300);}throw new AssertionError("Forwarding mismatch: "+last);}
    private void waitPhysical(int product)throws Exception{long until=SystemClock.uptimeMillis()+7000;while(SystemClock.uptimeMillis()<until){for(int id:InputDevice.getDeviceIds()){InputDevice d=InputDevice.getDevice(id);if(d!=null&&d.getVendorId()==0x2020&&d.getProductId()==product)return;}SystemClock.sleep(200);}throw new AssertionError("Stock controller did not reconnect with product "+product);}
    @Override public void onStart(){Bundle result=new Bundle();Context c=getTargetContext();Map<String,?> oldPrefs=new HashMap<>(ControllerMode.prefs(c).getAll());Activity activity=null;int oldMode=-1;String pid=null;Throwable failure=null;
        try{
            ControllerMode.requireOdin2(c);check(AppRuntime.controlState().equals("RUNNING"),"Bridge required");oldMode=ControllerMode.current(c);pid=RootHooks.execute("pidof odin_ds4_bridge");
            Map<String,byte[]> before=saved(c);boolean boot=BootStarter.isEnabled(c);Map<String,?> picker=new TreeMap<>(c.getSharedPreferences("PickerActivity",0).getAll());
            activity=startActivitySync(new Intent(c,PickerActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
            for(int mode:new int[]{ControllerMode.ODIN,ControllerMode.XBOX,ControllerMode.RAVEN,ControllerMode.DISCONNECTED,ControllerMode.ODIN}){
                ControllerMode.apply(c,mode);check(ControllerMode.current(c)==mode,"Wrong current mode");
                check(AppRuntime.controlState().equals("RUNNING")&&pid.equals(RootHooks.execute("pidof odin_ds4_bridge")),"Mode switch stopped/restarted bridge");
                check(ControllerMode.forwardingValue(c).equals(mode==ControllerMode.RAVEN?"all":mode==ControllerMode.DISCONNECTED?"off":"selected"),"Wrong forwarding policy");
                if(mode==ControllerMode.ODIN)waitPhysical(0x111);if(mode==ControllerMode.XBOX)waitPhysical(0x112);
                if(mode==ControllerMode.RAVEN)waitForward(true);if(mode==ControllerMode.DISCONNECTED||mode==ControllerMode.ODIN&&ControllerMode.next(mode)==ControllerMode.XBOX) {if(status().contains("Forwarding"))waitForward(false);}
                result.putString("mode_"+mode,ControllerMode.label(mode)+" verified; same native PID");
            }
            // Selected-app forwarding must still enable for Remote Play, then disable for Raven's own app.
            try(ParcelFileDescriptor fd=getUiAutomation().executeShellCommand("am start -n com.playstation.remoteplay/.RpActivityMain");InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)){while(in.read()!=-1){}}
            waitForward(true);
            final Activity screen=activity;runOnMainSync(()->c.startActivity(new Intent(c,PickerActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)));waitForIdleSync();waitForward(false);
            check(boot==BootStarter.isEnabled(c)&&picker.equals(c.getSharedPreferences("PickerActivity",0).getAll()),"Saved settings changed");Map<String,byte[]> after=saved(c);check(before.keySet().equals(after.keySet()),"Saved files changed");for(String name:before.keySet())check(Arrays.equals(before.get(name),after.get(name)),"Saved file changed: "+name);
        }catch(Throwable error){failure=error;}
        finally{try{if(oldMode>=0)ControllerMode.apply(c,oldMode);restorePrefs(c,oldPrefs);if(activity!=null){final Activity a=activity;runOnMainSync(a::finish);}}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}}
        if(failure==null){result.putString("stream","PASS: four controller modes, Odin/Xbox physical identity, selected-app and global forwarding, Disconnected pause, same bridge PID throughout, automatic reconnect, original settings/mappings/targets/boot preference preserved.\n");finish(0,result);}else{result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(failure));finish(1,result);}
    }
}
