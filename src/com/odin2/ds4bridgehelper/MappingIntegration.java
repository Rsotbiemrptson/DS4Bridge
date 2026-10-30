package com.odin2.ds4bridgehelper;

import android.content.*;
import android.content.pm.*;
import android.os.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Known-component detection, descriptor verification and exclusion ownership. */
final class MappingIntegration {
    static final String PAD="Raven Gamepad",LEGACY_PAD="Odin DS4 Bridge Test";
    static boolean isVirtualPad(android.view.InputDevice device){return device!=null&&((device.getVendorId()==0x054c&&device.getProductId()==0x09cc)||PAD.equals(device.getName())||device.getName().contains("DS4 Bridge"));}
    // Accessed only on the main looper: never overlap exclusion actions/probes.
    private static boolean sessionBusy;
    interface Callback { void complete(boolean ok,String message); }
    private static SharedPreferences prefs(Context c){return c.createDeviceProtectedStorageContext().getSharedPreferences("mapping_integration",0);}
    private static ComponentName component(MappingProfiles.Profile p){return new ComponentName(p.pkg,p.service);}
    private static String version(Context c,MappingProfiles.Profile p)throws Exception{return c.getPackageManager().getPackageInfo(p.pkg,0).versionName;}
    private static boolean installed(Context c,MappingProfiles.Profile p){
        try{ServiceInfo info=c.getPackageManager().getServiceInfo(component(p),0);return info.enabled&&info.exported&&info.applicationInfo.enabled&&(info.applicationInfo.flags&(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))!=0;}catch(PackageManager.NameNotFoundException e){return false;}
    }
    private static MappingProfiles.Profile select(Context c)throws Exception{
        return select(c,false);
    }
    private static MappingProfiles.Profile select(Context c,boolean allowDiagnostic)throws Exception{
        String saved=prefs(c).getString("owner","");
        if(!saved.isEmpty()){
            MappingProfiles.Profile p=MappingProfiles.byId(saved);
            if(p==null||!installed(c,p))throw new IllegalStateException("Saved mapping integration is unavailable; no fallback used for cleanup safety.");
            if(!p.supportsVersion(version(c,p)))throw new IllegalStateException("Saved mapping integration has an unverified version.");
            return p;
        }
        for(MappingProfiles.Profile p:MappingProfiles.SUPPORTED)if(installed(c,p)&&p.supportsVersion(version(c,p)))return p;
        if(allowDiagnostic)for(MappingProfiles.Profile p:MappingProfiles.DIAGNOSTIC_ONLY)if(installed(c,p)&&p.supportsVersion(version(c,p)))return p;
        throw new IllegalStateException("No supported mapping integration detected. See Diagnostics; unknown services are not invoked.");
    }
    static void requireSupported(Context c)throws Exception{select(c);}
    static boolean supported(Context c){try{select(c);return true;}catch(Exception unavailable){return false;}}
    static String summary(Context c){
        StringBuilder out=new StringBuilder("Touch Mapping integration:\n");
        try{MappingProfiles.Profile p=select(c);out.append("Detected: ").append(p.label).append("\nComponent: ").append(component(p).flattenToShortString()).append("\nVersion: ").append(version(c,p)).append("\nInterface verification: performed when connected; detection alone is not a successful connection.\n");}
        catch(Exception e){out.append("Unsupported/unavailable: ").append(e.getMessage()).append('\n');}
        for(MappingProfiles.Profile p:MappingProfiles.SUPPORTED)if(installed(c,p))try{if(!p.supportsVersion(version(c,p)))out.append("Unverified version: ").append(p.pkg).append(' ').append(version(c,p)).append(" (diagnostic only)\n");}catch(Exception ignored){}
        try{PackageInfo rp=c.getPackageManager().getPackageInfo("com.rp.mapping",PackageManager.GET_SERVICES);out.append("Retroid mapping candidate: ").append(rp.packageName).append(' ').append(rp.versionName).append(" — diagnostic only; exclusion writes/setup/start disabled. Verified RP6 June 2026 version can use read-only list 37 during checks; change 38 is never invoked.\n");}catch(PackageManager.NameNotFoundException ignored){}
        SharedPreferences state=prefs(c);out.append("Saved exclusion owner: ").append(state.getString("owner","none")).append("\nLast check/action: ").append(state.getString("result","not checked")).append('\n');return out.toString();
    }
    static void apply(Context context,int operation,Callback callback){
        if(operation < -1||operation > 1)throw new IllegalArgumentException("Invalid mapping operation");
        Context app=context.getApplicationContext();new Handler(Looper.getMainLooper()).post(()->{
            if(sessionBusy){callback.complete(false,"Another mapping integration action is in progress; try again after it finishes.");return;}
            sessionBusy=true;new Session(app,operation,callback).begin();
        });
    }
    static String probe(Context context)throws Exception{
        return blocking(context,-1,false);
    }
    static String remove(Context context)throws Exception{
        return blocking(context,0,true);
    }
    private static String blocking(Context context,int operation,boolean requireSuccess)throws Exception{
        if(Looper.myLooper()==Looper.getMainLooper())throw new IllegalStateException("Probe must run off the UI thread");
        CountDownLatch ready=new CountDownLatch(1);String[] result={"Mapping integration operation timed out"};boolean[] success={false};
        apply(context,operation,(ok,message)->{result[0]=message;success[0]=ok;ready.countDown();});
        if(!ready.await(9,TimeUnit.SECONDS)||requireSuccess&&!success[0])throw new IllegalStateException(result[0]);return result[0];
    }
    private static final class Session implements ServiceConnection {
        final Context c;final int operation;final Callback callback;final Handler main=new Handler(Looper.getMainLooper());
        MappingProfiles.Profile profile;boolean bound;volatile boolean finished;
        final Runnable timeout=()->finish(false,"Mapping integration timed out; action outcome may be uncertain. No fallback attempted.");
        Session(Context c,int operation,Callback callback){this.c=c;this.operation=operation;this.callback=callback;}
        void begin(){
            try{profile=select(c,operation==-1);main.postDelayed(timeout,8000);bound=c.bindService(new Intent().setComponent(component(profile)),this,Context.BIND_AUTO_CREATE);if(!bound)finish(false,"Cannot bind "+profile.label+" mapping service");}
            catch(Exception e){finish(false,"Mapping integration unavailable: "+e.getMessage());}
        }
        public void onServiceConnected(ComponentName name,IBinder binder){
            if(finished)return;
            new Thread(()->{
                try{
                    profile.verify(version(c,profile),binder.getInterfaceDescriptor());
                    ArrayList<String> names=list(binder,profile);if(finished)return;
                    if(operation==1){
                        // Persist before sending a write, so a lost reply cannot lose cleanup ownership.
                        if(!prefs(c).edit().putString("owner",profile.id).commit())throw new IllegalStateException("Cannot save exclusion owner");
                        if(!names.contains(PAD)&&!finished)change(binder,profile,1,PAD);
                        if(!list(binder,profile).contains(PAD))throw new IllegalStateException("Exclusion add was not confirmed");
                    }else if(operation==0){
                        for(String pad:new String[]{PAD,LEGACY_PAD}){
                            int count=Collections.frequency(names,pad);if(count>64)throw new IllegalStateException("Unexpected duplicate exclusion count");
                            for(int i=0;i<count&&!finished;i++)change(binder,profile,0,pad);
                        }
                        if(finished)return;
                        ArrayList<String> remaining=list(binder,profile);
                        if(remaining.contains(PAD)||remaining.contains(LEGACY_PAD))throw new IllegalStateException("Exclusion removal was not confirmed");
                        if(!prefs(c).edit().remove("owner").commit())throw new IllegalStateException("Cannot clear exclusion owner");
                    }
                    String message=profile.label+" verified ("+profile.descriptor+"): "+(operation==1?"exclusion added":operation==0?"exclusion removed":"read-only connection/list check passed");
                    main.post(()->finish(true,message));
                }catch(Exception e){main.post(()->finish(false,"Mapping integration failed: "+e.getMessage()));}
            },"DS4MappingAdapter").start();
        }
        public void onServiceDisconnected(ComponentName name){finish(false,"Mapping service disconnected");}
        public void onNullBinding(ComponentName name){finish(false,"Mapping service returned no interface");}
        public void onBindingDied(ComponentName name){finish(false,"Mapping service binding died");}
        void finish(boolean ok,String message){if(finished)return;finished=true;main.removeCallbacks(timeout);if(bound)try{c.unbindService(this);}catch(IllegalArgumentException ignored){}prefs(c).edit().putString("result",message).apply();sessionBusy=false;callback.complete(ok,message);}
    }
    private static ArrayList<String> list(IBinder binder,MappingProfiles.Profile p)throws RemoteException{
        Parcel data=Parcel.obtain(),reply=Parcel.obtain();
        try{data.writeInterfaceToken(p.descriptor);if(!binder.transact(p.listCode,data,reply,0))throw new RemoteException("Unsupported list operation");reply.readException();ArrayList<String> names=reply.createStringArrayList();if(names==null)throw new RemoteException("Invalid exclusion list");return names;}finally{data.recycle();reply.recycle();}
    }
    private static void change(IBinder binder,MappingProfiles.Profile p,int operation,String pad)throws RemoteException{
        Parcel data=Parcel.obtain(),reply=Parcel.obtain();
        try{data.writeInterfaceToken(p.descriptor);data.writeInt(operation);data.writeString(pad);if(!binder.transact(p.changeCode,data,reply,0))throw new RemoteException("Unsupported exclusion operation");reply.readException();}finally{data.recycle();reply.recycle();}
    }
}
