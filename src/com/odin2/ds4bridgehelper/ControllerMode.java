package com.odin2.ds4bridgehelper;

import android.content.Context;
import android.content.SharedPreferences;

/** Odin 2's verified stock modes plus a global Raven forwarding override. */
final class ControllerMode {
    static final int ODIN=0,XBOX=1,RAVEN=2,DISCONNECTED=3;
    static SharedPreferences prefs(Context c){return c.createDeviceProtectedStorageContext().getSharedPreferences("controller_mode",0);}
    static boolean allApps(Context c){return prefs(c).getBoolean("all_apps",false);}
    static void forwarding(Context c,boolean all){if(!prefs(c).edit().putBoolean("all_apps",all).putInt("override",all?RAVEN:-1).commit())throw new IllegalStateException("Cannot save forwarding mode");}
    static void requireOdin2(Context c)throws Exception{
        MappingIntegration.requireSupported(c);RootHooks.requireRoot();
        if(android.os.Build.VERSION.SDK_INT!=33||!"Odin2".equals(RootHooks.execute("getprop ro.product.vendor.model")))throw new IllegalStateException("Controller-style switching is currently verified only for Odin 2 / Android 13");
    }
    static int stock(String key)throws Exception{String value=RootHooks.execute("settings get system "+key);if(value.equals("null"))return 0;if(!value.equals("0")&&!value.equals("1")&&!value.equals("2"))throw new IllegalStateException("Unexpected stock controller setting");return Integer.parseInt(value);}
    private static void put(String key,int value)throws Exception{
        RootHooks.execute("settings put system "+key+" "+value);
        if(stock(key)!=value)throw new IllegalStateException("Stock controller setting did not apply: "+key);
    }
    static int current(Context c)throws Exception{
        requireOdin2(c);
        if(stock("no_create_gamepad_button_layout")==1)return DISCONNECTED;
        if(allApps(c)&&AppRuntime.controlState().equals("RUNNING"))return RAVEN;
        return stock("flip_button_layout")==1?XBOX:ODIN;
    }
    static String label(int mode){return mode==ODIN?"Odin":mode==XBOX?"Xbox":mode==RAVEN?"Raven Gamepad":"Disconnected";}
    static int next(int mode){return (mode+1)%4;}
    private static void stockMode(int mode)throws Exception{
        // Same three settings and order as stock 318 ControllerStylePreference.
        put("temp_abxy_layout_mode",mode==XBOX?0:mode==ODIN?1:2);
        put("flip_button_layout",mode==XBOX?1:0);
        put("no_create_gamepad_button_layout",mode==DISCONNECTED?1:0);
    }
    static String forwardingValue(Context c){
        if(prefs(c).getBoolean("transition",false)||android.provider.Settings.System.getInt(c.getContentResolver(),"no_create_gamepad_button_layout",0)==1)return "off";
        return allApps(c)?"all":"selected";
    }
    static synchronized String apply(Context c,int mode)throws Exception{
        if(mode<ODIN||mode>DISCONNECTED)throw new IllegalArgumentException("Unknown controller mode");requireOdin2(c);
        if(!"17".equals(RootHooks.execute("cat "+AppRuntime.STATE+"/ready 2>/dev/null")))throw new IllegalStateException("Use Install/Update Components in Bridge controls first");
        String state=AppRuntime.controlState();if(state.equals("FIRMWARE"))throw new IllegalStateException("Remove the firmware bridge before using controller-style switching");
        int oldStyle=stock("temp_abxy_layout_mode"),oldFlip=stock("flip_button_layout"),oldDisabled=stock("no_create_gamepad_button_layout");boolean oldAll=allApps(c);int oldOverride=prefs(c).getInt("override",-1);
        if(!prefs(c).edit().putBoolean("transition",true).commit())throw new IllegalStateException("Cannot pause forwarding for mode change");
        try{
            if(mode==RAVEN){put("no_create_gamepad_button_layout",0);forwarding(c,true);}
            else{stockMode(mode);if(!prefs(c).edit().putBoolean("all_apps",false).putInt("override",mode).commit())throw new IllegalStateException("Cannot save controller mode");}
            AppRuntime.startAndWait();
            if(current(c)!=mode)throw new IllegalStateException("Controller mode could not be confirmed");
        }catch(Exception error){
            try{put("temp_abxy_layout_mode",oldStyle);put("flip_button_layout",oldFlip);put("no_create_gamepad_button_layout",oldDisabled);prefs(c).edit().putBoolean("all_apps",oldAll).putInt("override",oldOverride).commit();}catch(Exception restore){error.addSuppressed(restore);}throw error;
        }finally{prefs(c).edit().putBoolean("transition",false).commit();}
        return label(mode)+(mode==RAVEN?": all apps":mode==DISCONNECTED?": controller disabled; bridge waiting":": selected apps");
    }
    static synchronized String cycle(Context c)throws Exception{return apply(c,next(current(c)));}
    private ControllerMode(){}
}
