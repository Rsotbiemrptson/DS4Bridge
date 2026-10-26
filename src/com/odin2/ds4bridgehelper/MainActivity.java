package com.odin2.ds4bridgehelper;

import android.app.Activity;
import android.os.*;
import android.widget.TextView;

/** Legacy activity shim shares the broadcast receiver's verified adapter path. */
public class MainActivity extends Activity {
    @Override protected void onCreate(Bundle saved){
        super.onCreate(saved);TextView status=new TextView(this);status.setText("Detecting Touch Mapping integration...");status.setTextSize(20);status.setPadding(24,24,24,24);setContentView(status);
        MappingIntegration.apply(this,"remove".equals(getIntent().getStringExtra("action"))?0:1,(ok,message)->{
            if(isDestroyed())return;status.setText(message);new Handler(Looper.getMainLooper()).postDelayed(this::finish,1500);
        });
    }
}
