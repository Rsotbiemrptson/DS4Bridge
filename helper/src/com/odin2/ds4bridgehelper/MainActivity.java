package com.odin2.ds4bridgehelper;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends Activity {
    private static final String TAG = "DS4BridgeHelper";
    static final String OLD_DESCRIPTOR = "com.odin2.touchmapping.sdk.IServerApi";
    static final String NEW_DESCRIPTOR = "com.ro.mapping.sdk.IServerApi";
    static final String NAME = "Odin DS4 Bridge Test";
    private static final ComponentName OLD_SERVICE = new ComponentName(
            "com.odin2.touchmapping", "com.odin2.touchmapping.service.ApiService");
    private static final ComponentName NEW_SERVICE = new ComponentName(
            "com.odin2.touchmapping", "com.ro.mapping.service.ApiService");

    static ComponentName service(Context context) {
        try {
            context.getPackageManager().getServiceInfo(NEW_SERVICE, 0);
            return NEW_SERVICE;
        } catch (PackageManager.NameNotFoundException ignored) {
            return OLD_SERVICE;
        }
    }

    private static boolean newer(IBinder binder) throws RemoteException {
        String descriptor = binder.getInterfaceDescriptor();
        if (NEW_DESCRIPTOR.equals(descriptor)) return true;
        if (OLD_DESCRIPTOR.equals(descriptor)) return false;
        throw new RemoteException("Unexpected mapping interface: " + descriptor);
    }

    private boolean bound;
    private TextView status;
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            String action = getIntent().getStringExtra("action");
            int operation = "remove".equals(action) ? 0 : 1;
            try {
                transactChange(binder, operation);
                ArrayList<String> names = transactList(binder);
                Log.i(TAG, "operation=" + (operation == 1 ? "add" : "remove")
                        + " name=" + NAME + " blacklist=" + names);
                showStatus("AYN exclusion " + (operation == 1 ? "added" : "removed"));
            } catch (Exception e) {
                Log.e(TAG, "AYN controller exclusion failed", e);
                showStatus("AYN exclusion failed: " + e.getMessage());
            }
            new Handler().postDelayed(() -> {
                if (bound) {
                    unbindService(connection);
                    bound = false;
                }
                finish();
            }, 1000);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.w(TAG, "AYN touch-mapping service disconnected");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        status = new TextView(this);
        status.setText("Connecting to AYN touch mapping...");
        status.setTextSize(20);
        status.setPadding(24, 24, 24, 24);
        setContentView(status);
        bound = bindService(new Intent().setComponent(service(this)), connection, BIND_AUTO_CREATE);
        if (!bound) {
            Log.e(TAG, "Could not bind AYN touch-mapping service");
            showStatus("Could not connect to AYN touch mapping");
            new Handler().postDelayed(this::finish, 1500);
        }
    }

    private void showStatus(String message) {
        status.setText(message);
    }

    static void transactChange(IBinder binder, int operation) throws RemoteException {
        ArrayList<String> current = transactList(binder);
        if (operation == 1) {
            if (!current.contains(NAME)) sendChange(binder, 1);
        } else {
            for (String name : current) {
                if (NAME.equals(name)) sendChange(binder, 0);
            }
        }
    }

    private static void sendChange(IBinder binder, int operation) throws RemoteException {
        boolean modern = newer(binder);
        int code = modern ? 35 : 34;
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(modern ? NEW_DESCRIPTOR : OLD_DESCRIPTOR);
            data.writeInt(operation);
            data.writeString(NAME);
            if (!binder.transact(code, data, reply, 0)) {
                throw new RemoteException("transaction " + code + " not supported");
            }
            reply.readException();
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    static ArrayList<String> transactList(IBinder binder) throws RemoteException {
        boolean modern = newer(binder);
        int code = modern ? 34 : 33;
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(modern ? NEW_DESCRIPTOR : OLD_DESCRIPTOR);
            if (!binder.transact(code, data, reply, 0)) {
                throw new RemoteException("transaction " + code + " not supported");
            }
            reply.readException();
            ArrayList<String> names = new ArrayList<>();
            reply.readStringList(names);
            return names;
        } finally {
            data.recycle();
            reply.recycle();
        }
    }
}
