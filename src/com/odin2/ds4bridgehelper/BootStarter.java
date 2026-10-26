package com.odin2.ds4bridgehelper;

import android.content.Context;
import android.os.IBinder;
import android.os.Parcel;
import android.util.Log;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

/** Uses only a fixed, root-owned launcher path; no user text is passed to the shell. */
final class BootStarter {
    private static final String TAG = "DS4BootStarter";

    private BootStarter() {}

    static boolean isEnabled(Context context) {
        return context.createDeviceProtectedStorageContext()
                .getSharedPreferences("boot", Context.MODE_PRIVATE)
                .getBoolean("auto_start", false);
    }

    static void setEnabled(Context context, boolean enabled) {
        context.createDeviceProtectedStorageContext()
                .getSharedPreferences("boot", Context.MODE_PRIVATE)
                .edit().putBoolean("auto_start", enabled).apply();
    }

    static void launch() {
        try {
            MappingIntegration.requireSupported(BridgeApplication.context());
            String exists = RootHooks.execute("[ -x " + AppRuntime.STATE + "/start.sh ] && echo YES");
            String command = "YES".equals(exists) ? "/system/bin/sh " + AppRuntime.STATE + "/start.sh"
                    : "/system/bin/sh /data/local/odin2-ds4-bridge/boot-start.sh";
            Log.i(TAG, "Boot launcher reply: " + RootHooks.execute(command));
        } catch (Throwable error) {
            Log.e(TAG, "Boot startup failed", error);
        }
    }
}
