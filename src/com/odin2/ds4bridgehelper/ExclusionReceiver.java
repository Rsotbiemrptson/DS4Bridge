package com.odin2.ds4bridgehelper;

import android.content.*;
import android.util.Log;

public class ExclusionReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        String action=intent.getStringExtra("action");
        if(!"add".equals(action)&&!"remove".equals(action))return;
        PendingResult pending=goAsync();
        MappingIntegration.apply(context,"remove".equals(action)?0:1,(ok,message)->{
            if(ok)Log.i("DS4MappingAdapter",message);else Log.e("DS4MappingAdapter",message);
            pending.setResultCode(ok?-1:0);pending.finish();
        });
    }
}
