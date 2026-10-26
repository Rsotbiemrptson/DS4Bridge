package com.odin2.ds4bridgehelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                || !BootStarter.isEnabled(context)) return;
        PendingResult pending = goAsync();
        new Thread(() -> {
            try { BootStarter.launch(); }
            finally { pending.finish(); }
        }, "ds4-boot-starter").start();
    }
}
