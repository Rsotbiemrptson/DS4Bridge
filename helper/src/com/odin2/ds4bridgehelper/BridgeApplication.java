package com.odin2.ds4bridgehelper;

import android.app.Application;
import android.content.Context;

public final class BridgeApplication extends Application {
    private static Context app;
    @Override public void onCreate(){super.onCreate();app=getApplicationContext();}
    static Context context(){if(app==null)throw new IllegalStateException("Application context unavailable");return app;}
}
