package com.odin2.ds4bridgehelper;

import android.app.Instrumentation;
import android.content.Context;
import android.os.*;
import java.io.*;
import java.util.*;

/** Actual installed-app-UID probe. No exclusion writes, runtime changes or mapping edits. */
public final class IntegrationSmoke extends Instrumentation {
    private String action="probe";
    @Override public void onCreate(Bundle args){super.onCreate(args);if(args!=null)action=args.getString("action","probe");start();}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private Map<String,byte[]> profiles(Context c)throws IOException{
        Map<String,byte[]> result=new TreeMap<>();File[] files=c.createDeviceProtectedStorageContext().getFilesDir().listFiles();
        if(files!=null)for(File file:files)if(file.getName().equals("targets.conf")||file.getName().equals("profile.conf")||file.getName().startsWith("controller-")&&file.getName().endsWith(".conf")){
            try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);result.put(file.getName(),out.toByteArray());
            }
        }
        return result;
    }
    @Override public void onStart(){Bundle result=new Bundle();
        try{
            Context c=getTargetContext(),de=c.createDeviceProtectedStorageContext();
            check(android.os.Process.myUid()==c.getApplicationInfo().uid,"Probe is not using installed app UID");
            check("0.16.0".equals(c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName),"Wrong app version");
            if("start".equals(action)){
                Map<String,byte[]> before=profiles(c);
                byte[] targets=before.get("targets.conf");
                check(targets!=null&&Arrays.asList(new String(targets,java.nio.charset.StandardCharsets.UTF_8).split("\\r?\\n")).contains("com.playstation.remoteplay"),"Select and save PS Remote Play in the app list first");
                String state=AppRuntime.controlState();check(!state.equals("MISSING")&&!state.equals("FIRMWARE"),"Standalone runtime required: "+state);
                String started=AppRuntime.startAndWait();
                check(AppRuntime.controlState().equals("RUNNING"),"Bridge did not remain running");
                boolean wasEnabled=BootStarter.isEnabled(c);
                check(de.getSharedPreferences("boot",0).edit().putBoolean("auto_start",true).commit(),"Could not save automatic startup");
                String probe=MappingIntegration.probe(c);check(probe.contains("read-only connection/list check passed"),probe);
                check(de.getSharedPreferences("mapping_integration",0).getString("owner","").startsWith("odin2-"),"Startup did not record the detected Odin 2 exclusion owner");
                Map<String,byte[]> after=profiles(c);check(before.keySet().equals(after.keySet()),"Startup changed profile files");
                for(String name:before.keySet())check(Arrays.equals(before.get(name),after.get(name)),"Startup changed "+name);
                result.putString("stream","PASS: bridge started. "+started+"\nRemote Play already selected; mappings and selections unchanged.\nStart bridge after reboot: enabled (previously "+wasEnabled+").\n"+MappingIntegration.summary(c));finish(0,result);return;
            }
            check("probe".equals(action),"Unknown integration test action");
            Map<String,byte[]> before=profiles(c);boolean boot=BootStarter.isEnabled(c);
            String owner=de.getSharedPreferences("mapping_integration",0).getString("owner","");
            Map<String,?> picker=new TreeMap<>(c.getSharedPreferences("PickerActivity",0).getAll());
            RootHooks.requireRoot();String pidBefore=RootHooks.execute("pidof odin_ds4_bridge");String state=AppRuntime.controlState();
            String detected=MappingIntegration.summary(c);
            check(detected.contains("Detected: Odin 2"),"No supported Odin 2 integration detected: "+detected);
            String probe=MappingIntegration.probe(c);
            check(probe.contains("read-only connection/list check passed"),probe);
            check(probe.contains(MappingProfiles.OLD)||probe.contains(MappingProfiles.MODERN),"Verified interface not recorded");
            check(owner.equals(de.getSharedPreferences("mapping_integration",0).getString("owner","")),"Read-only probe changed exclusion ownership");
            check(boot==BootStarter.isEnabled(c),"Probe changed reboot preference");
            check(picker.equals(c.getSharedPreferences("PickerActivity",0).getAll()),"Probe changed picker preferences");
            Map<String,byte[]> after=profiles(c);check(before.keySet().equals(after.keySet()),"Probe created/removed a profile");
            for(String name:before.keySet())check(Arrays.equals(before.get(name),after.get(name)),"Probe changed "+name);
            check(pidBefore.equals(RootHooks.execute("pidof odin_ds4_bridge")),"Probe changed running bridge PID");
            check(state.equals(AppRuntime.controlState()),"Probe changed bridge state");
            result.putString("stream","PASS: live integration detection from installed app UID.\n"+probe+"\n"+MappingIntegration.summary(c)+"Privileged access: verified root UID 0.\nRuntime state unchanged: "+state+"; selections, mappings, exclusion owner and reboot preference unchanged.\n");finish(0,result);
        }catch(Throwable error){result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(error));finish(1,result);}
    }
}
