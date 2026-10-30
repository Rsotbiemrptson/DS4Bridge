package com.odin2.ds4bridgehelper;

import android.app.Instrumentation;
import android.content.*;
import android.os.*;
import android.view.InputDevice;
import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

/** Explicit component-upgrade test: touches only the bridge's two exclusion names. */
public final class RenameSmoke extends Instrumentation {
    private IBinder binder;private MappingProfiles.Profile profile;private boolean bound;
    private final CountDownLatch connected=new CountDownLatch(1);
    private final ServiceConnection connection=new ServiceConnection(){
        public void onServiceConnected(ComponentName name,IBinder service){binder=service;connected.countDown();}
        public void onServiceDisconnected(ComponentName name){}
    };
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private Map<String,byte[]> files(Context context)throws Exception{
        Map<String,byte[]> result=new TreeMap<>();File[] files=context.createDeviceProtectedStorageContext().getFilesDir().listFiles();
        if(files!=null)for(File file:files)if(file.getName().equals("profile.conf")||file.getName().equals("targets.conf")||file.getName().matches("controller-[0-9]+-[0-9]+\\.conf")){
            try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] block=new byte[4096];int count;while((count=in.read(block))!=-1)out.write(block,0,count);result.put(file.getName(),out.toByteArray());
            }
        }return result;
    }
    @SuppressWarnings("unchecked") private ArrayList<String> list()throws Exception{
        Method method=MappingIntegration.class.getDeclaredMethod("list",IBinder.class,MappingProfiles.Profile.class);method.setAccessible(true);return (ArrayList<String>)method.invoke(null,binder,profile);
    }
    private void change(int operation,String pad)throws Exception{
        Method method=MappingIntegration.class.getDeclaredMethod("change",IBinder.class,MappingProfiles.Profile.class,int.class,String.class);method.setAccessible(true);method.invoke(null,binder,profile,operation,pad);
    }
    private Map<String,Integer> unrelated()throws Exception{
        Map<String,Integer> result=new TreeMap<>();for(String name:list())if(!name.equals(MappingIntegration.PAD)&&!name.equals(MappingIntegration.LEGACY_PAD))result.put(name,result.getOrDefault(name,0)+1);return result;
    }
    private void noExclusions()throws Exception{ArrayList<String> names=list();check(!names.contains(MappingIntegration.PAD)&&!names.contains(MappingIntegration.LEGACY_PAD),"Current or legacy exclusion remains");}
    private InputDevice renamedPad()throws Exception{
        final InputDevice[] found={null};long until=SystemClock.uptimeMillis()+6000;
        while(found[0]==null&&SystemClock.uptimeMillis()<until){runOnMainSync(()->{for(int id:InputDevice.getDeviceIds()){InputDevice d=InputDevice.getDevice(id);if(d!=null&&d.getName().equals(MappingIntegration.PAD))found[0]=d;}});if(found[0]==null)SystemClock.sleep(200);}return found[0];
    }
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle result=new Bundle();Context context=getTargetContext();boolean upgraded=false;
        try{
            Map<String,byte[]> before=files(context);boolean boot=BootStarter.isEnabled(context);Map<String,?> preferences=new TreeMap<>(context.getSharedPreferences("PickerActivity",0).getAll());
            String initial=AppRuntime.controlState();check(initial.equals("RUNNING")||initial.equals("STOPPED"),"Expected installed standalone bridge");
            Method select=MappingIntegration.class.getDeclaredMethod("select",Context.class);select.setAccessible(true);profile=(MappingProfiles.Profile)select.invoke(null,context);
            String version=context.getPackageManager().getPackageInfo(profile.pkg,0).versionName;
            runOnMainSync(()->bound=context.bindService(new Intent().setComponent(new ComponentName(profile.pkg,profile.service)),connection,Context.BIND_AUTO_CREATE));
            check(bound&&connected.await(5,TimeUnit.SECONDS),"Cannot connect to verified OEM integration");profile.verify(version,binder.getInterfaceDescriptor());
            Map<String,Integer> unrelated=unrelated();
            String setup=AppRuntime.setup(context);upgraded=true;check(setup.contains("installed"),setup);
            check("17".equals(RootHooks.execute("cat "+AppRuntime.STATE+"/ready")),"Wrong component revision");noExclusions();
            // Reproduce old-name leftovers, including duplicates, plus the new name.
            change(1,MappingIntegration.LEGACY_PAD);change(1,MappingIntegration.LEGACY_PAD);change(1,MappingIntegration.PAD);
            MappingIntegration.remove(context);noExclusions();check(unrelated.equals(unrelated()),"Cleanup changed another controller's exclusion");
            AppRuntime.startAndWait();InputDevice device=renamedPad();check(device!=null,"Renamed virtual controller not registered");
            check(device.getVendorId()==0x054c&&device.getProductId()==0x09cc,"Sony IDs changed");check(MappingIntegration.isVirtualPad(device),"Renamed pad not filtered from physical controllers");
            check(list().contains(MappingIntegration.PAD)&&!list().contains(MappingIntegration.LEGACY_PAD),"New-name exclusion not active or old-name exclusion remains");
            AppRuntime.stopAndWait();noExclusions();AppRuntime.startAndWait();check(renamedPad()!=null&&list().contains(MappingIntegration.PAD),"Restart lost renamed controller or exclusion");
            check(unrelated.equals(unrelated()),"Restart changed another controller's exclusion");
            check(boot==BootStarter.isEnabled(context)&&preferences.equals(context.getSharedPreferences("PickerActivity",0).getAll()),"Preferences changed");
            Map<String,byte[]> after=files(context);check(before.keySet().equals(after.keySet()),"Mapping or target file set changed");for(String name:before.keySet())check(Arrays.equals(before.get(name),after.get(name)),"Saved file changed: "+name);
            result.putString("stream","PASS: renamed Raven Gamepad, component revision 17, Sony IDs unchanged, legacy/duplicate exclusion cleanup, new exclusion, stop/start/restart and virtual-input filtering; saved mappings, targets, preferences, boot flag and unrelated exclusions preserved.\n");finish(0,result);
        }catch(Throwable error){if(upgraded)try{AppRuntime.startAndWait();}catch(Exception ignored){}result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(error));finish(1,result);}
        finally{if(bound)runOnMainSync(()->context.unbindService(connection));}
    }
}
