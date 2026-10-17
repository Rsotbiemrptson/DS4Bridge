package com.odin2.ds4bridgehelper;

import android.app.Instrumentation;
import android.os.Bundle;

/** Runs without screen interaction and only with a firmware bridge present. */
public final class RuntimeSmoke extends Instrumentation {
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    @Override public void onStart() {
        Bundle results = new Bundle();
        try {
            RootHooks.requireRoot();
            if (!"YES".equals(RootHooks.execute("[ -x /system_ext/bin/odin_ds4_bridge ] && echo YES")))
                throw new IllegalStateException("This smoke test requires the current firmware bridge");
            String original = RootHooks.execute("pidof odin_ds4_bridge");
            if (original.isEmpty()) throw new IllegalStateException("Firmware bridge is not running");
            AppRuntime.setup(getTargetContext());
            String inspection = AppRuntime.inspect();
            if (!inspection.contains("HOOK /dev/uinput open succeeded") || !inspection.contains("DEVICE 8224"))
                throw new IllegalStateException("Diagnostic input inspection failed");
            if (!AppRuntime.start().contains("firmware bridge is already installed"))
                throw new IllegalStateException("Duplicate bridge guard failed");
            AppRuntime.setup(getTargetContext()); // Exercise update/idempotence.
            AppRuntime.remove();
            if (!"YES".equals(RootHooks.execute("[ ! -e " + AppRuntime.STATE + " ] && echo YES")))
                throw new IllegalStateException("App runtime was not fully removed");
            AppRuntime.setup(getTargetContext()); // Leave the final prototype ready.
            if (!original.equals(RootHooks.execute("pidof odin_ds4_bridge")))
                throw new IllegalStateException("Existing firmware bridge changed during testing");
            results.putString("stream", "PASS: APK setup, update, diagnostics, duplicate guard, removal and re-setup; firmware bridge unchanged.\n");
            results.putString("result", "PASS");
            finish(0, results);
        } catch (Exception error) {
            results.putString("stream", "FAIL: " + error + "\n");
            results.putString("result", "FAIL");
            finish(1, results);
        }
    }
}
