package com.odin2.ds4bridgehelper;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class ExclusionReceiver extends BroadcastReceiver {
    private static final String TAG = "DS4BridgeHelper";

    @Override
    public void onReceive(Context context, Intent intent) {
        final int operation = "remove".equals(intent.getStringExtra("action")) ? 0 : 1;
        final PendingResult pending = goAsync();
        final Context appContext = context.getApplicationContext();
        ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                try {
                    MainActivity.transactChange(binder, operation);
                    Log.i(TAG, "broadcast operation=" + (operation == 1 ? "add" : "remove")
                            + " blacklist=" + MainActivity.transactList(binder));
                } catch (Exception e) {
                    Log.e(TAG, "broadcast exclusion failed", e);
                } finally {
                    appContext.unbindService(this);
                    pending.finish();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                pending.finish();
            }
        };
        if (!appContext.bindService(new Intent().setComponent(MainActivity.service(appContext)),
                connection, Context.BIND_AUTO_CREATE)) {
            Log.e(TAG, "broadcast could not bind AYN touch mapping");
            pending.finish();
        }
    }
}
