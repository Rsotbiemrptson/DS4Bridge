package com.odin2.ds4bridgehelper;

import android.content.Context;
import android.util.Log;
import java.io.*;
import java.security.MessageDigest;
import java.util.Locale;

/** APK assets are copied to root-owned staging and verified before execution. */
final class AppRuntime {
    static final String STATE = "/data/local/ds4-bridge-app";
    static final String[] FILES = {"odin_ds4_bridge", "start.sh", "stop.sh", "supervisor.sh"};
    static String quote(String s) { return "'" + s.replace("'", "'\\''") + "'"; }
    static synchronized String setup(Context context) throws Exception {
        MappingIntegration.requireSupported(context);
        RootHooks.requireRoot();
        Context storage = context.createDeviceProtectedStorageContext();
        File payload = new File(storage.getFilesDir(), "payload-v17");
        if (!payload.isDirectory() && !payload.mkdirs()) throw new IOException("Cannot stage app files");
        checked("[ ! -L " + STATE + " ] && [ ! -L " + STATE + "/incoming ]");
        checked("[ ! -d " + STATE + " ] || [ \"$(stat -c %u " + STATE + ")\" = 0 ]");
        if ("YES".equals(RootHooks.execute("[ -x " + STATE + "/stop.sh ] && echo YES"))) {
            stopAndWait();
            MappingIntegration.remove(context);
        }
        checked("umask 077; mkdir -p " + STATE + "/incoming");
        checked("chown 0:0 " + STATE + " " + STATE + "/incoming");
        checked("chmod 700 " + STATE + " " + STATE + "/incoming");
        checked("rm -f " + STATE + "/ready");
        for (String name : FILES) {
            File target = new File(payload, name);
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            try (InputStream input = context.getAssets().open("runtime/" + name);
                 FileOutputStream output = new FileOutputStream(target)) {
                byte[] block = new byte[16384]; int count;
                while ((count = input.read(block)) != -1) { hash.update(block, 0, count); output.write(block, 0, count); }
                output.getFD().sync();
            }
            StringBuilder hex = new StringBuilder();
            for (byte b : hash.digest()) hex.append(String.format(Locale.ROOT, "%02x", b & 255));
            String staged = STATE + "/incoming/" + name;
            checked("cp " + quote(target.getAbsolutePath()) + " " + staged);
            checked("chown 0:0 " + staged);
            checked("chmod 700 " + staged);
            checked("[ \"$(sha256sum " + staged + " | cut -d ' ' -f 1)\" = " + hex + " ]");
        }
        for (String name : FILES) checked("mv " + STATE + "/incoming/" + name + " " + STATE + "/" + name);
        checked("echo 17 > " + STATE + "/ready");
        Log.i("DS4AppRuntime", "SETUP_OK");
        return "App components installed. Start the bridge when ready.";
    }
    static String start() throws Exception {
        MappingIntegration.requireSupported(BridgeApplication.context());
        RootHooks.requireRoot();
        if ("YES".equals(RootHooks.execute("[ -x /system_ext/bin/odin_ds4_bridge ] && echo YES")))
            return "The firmware bridge is already installed. App mappings apply only to the standalone app runtime.";
        if (!"YES".equals(RootHooks.execute("[ -x " + STATE + "/start.sh ] && echo YES"))) return "Set up app components first.";
        RootHooks.execute("/system/bin/sh " + STATE + "/start.sh");
        String result = RootHooks.output("tail -n 6 " + STATE + "/start.log");
        Log.i("DS4AppRuntime", result);
        return result;
    }
    static String stop() throws Exception {
        RootHooks.requireRoot();
        if (!"YES".equals(RootHooks.execute("[ -x " + STATE + "/stop.sh ] && echo YES"))) return "App runtime is not installed.";
        return RootHooks.execute("/system/bin/sh " + STATE + "/stop.sh");
    }
    private static boolean supervisorAlive()throws Exception{
        String pid=RootHooks.execute("cat "+STATE+"/supervisor.pid 2>/dev/null");
        return pid.matches("[1-9][0-9]*")&&RootHooks.output("tr '\\000' ' ' < /proc/"+pid+"/cmdline").contains(STATE+"/supervisor.sh");
    }
    /** Only the app-owned PID is considered controllable; never stop another bridge. */
    static String controlState()throws Exception{
        RootHooks.requireRoot();
        if("YES".equals(RootHooks.execute("[ -x /system_ext/bin/odin_ds4_bridge ] && echo YES")))return "FIRMWARE";
        if(!"YES".equals(RootHooks.execute("[ -x "+STATE+"/start.sh ] && echo YES")))return "MISSING";
        String pid=RootHooks.execute("cat "+STATE+"/bridge.pid 2>/dev/null");
        if(pid.matches("[1-9][0-9]*")&&"YES".equals(RootHooks.execute("[ \"$(readlink /proc/"+pid+"/exe)\" = "+STATE+"/odin_ds4_bridge ] && kill -0 "+pid+" && echo YES")))return "RUNNING";
        return supervisorAlive()?"STARTING":"STOPPED";
    }
    static String startAndWait()throws Exception{
        String state=controlState();
        if(state.equals("FIRMWARE"))throw new IOException("The firmware bridge cannot be controlled by this app runtime.");
        if(state.equals("MISSING"))throw new IOException("Set up / update components first.");
        if(!state.equals("RUNNING")&&!state.equals("STARTING"))start();
        long until=android.os.SystemClock.uptimeMillis()+15000;
        while(android.os.SystemClock.uptimeMillis()<until){
            if(controlState().equals("RUNNING")){Thread.sleep(300);if(controlState().equals("RUNNING"))return "Bridge started. Saved mapping is active.";}
            Thread.sleep(350);
        }
        throw new IOException("Bridge did not start. Open Diagnostics for details.");
    }
    static String stopAndWait()throws Exception{
        String state=controlState();
        if(state.equals("FIRMWARE"))throw new IOException("The firmware bridge is not controlled by this app.");
        if(state.equals("MISSING"))return "App bridge is not installed.";
        stop();long until=android.os.SystemClock.uptimeMillis()+10000;
        while(android.os.SystemClock.uptimeMillis()<until){
            if(controlState().equals("STOPPED"))return "Bridge stopped.";
            Thread.sleep(250);
        }
        throw new IOException("Bridge did not stop. Open Diagnostics for details.");
    }
    static String restartAndWait()throws Exception{stopAndWait();return startAndWait();}
    static String remove() throws Exception {
        stop(); Thread.sleep(2500);
        // Explicitly clean up even when the supervisor was already stopped/crashed.
        MappingIntegration.remove(BridgeApplication.context());
        checked("[ ! -L " + STATE + " ] && [ ! -L " + STATE + "/incoming ]");
        for (String name : FILES) { checked("rm -f " + STATE + "/" + name); checked("rm -f " + STATE + "/incoming/" + name); }
        for (String name : new String[]{"start.log","runtime.log","profile.conf","supervisor.pid","bridge.pid","ready"}) checked("rm -f " + STATE + "/" + name);
        checked("rmdir " + STATE + "/incoming"); checked("rmdir " + STATE);
        return "App bridge components removed. Your saved app list and mappings remain.";
    }
    static String status() throws Exception {
        return RootHooks.output("id; getprop ro.product.vendor.model; getenforce") + "\n"
                + RootHooks.output("ls -l /dev/uinput /dev/uhid /dev/input/event* 2>/dev/null") + "\n"
                + RootHooks.output("ls -l /system/usr/keylayout/*054c* /vendor/usr/keylayout/*054c* 2>/dev/null") + "\n"
                + RootHooks.output("[ ! -x /system_ext/bin/odin_ds4_bridge ] || echo FIRMWARE_BRIDGE_PRESENT; pidof odin_ds4_bridge") + "\n"
                + RootHooks.output("tail -n 8 " + STATE + "/runtime.log 2>/dev/null");
    }
    static String inspect() throws Exception {
        RootHooks.requireRoot();
        if (!"YES".equals(RootHooks.execute("[ -x " + STATE + "/odin_ds4_bridge ] && echo YES")))
            throw new IOException("Set up app components before raw input inspection");
        return RootHooks.output(STATE + "/odin_ds4_bridge --check-hooks") + "\n"
                + RootHooks.output(STATE + "/odin_ds4_bridge --inspect");
    }
    static String testVirtual() throws Exception {
        RootHooks.requireRoot();
        if (!RootHooks.execute("pidof odin_ds4_bridge").isEmpty()) return "Skipped: an existing bridge is running.";
        return RootHooks.output(STATE + "/odin_ds4_bridge --seconds 2");
    }
    private static void checked(String command) throws Exception {
        String result = RootHooks.execute(command + " && echo DS4_OK");
        if (!result.endsWith("DS4_OK")) throw new IOException("Privileged operation failed: " + result);
    }
}
