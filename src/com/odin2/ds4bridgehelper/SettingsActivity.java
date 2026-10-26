package com.odin2.ds4bridgehelper;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

/** Raven tools hub, reachable from shared app navigation. */
public final class SettingsActivity extends android.app.Activity {
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);startActivity(new Intent(this,PickerActivity.class).putExtra("open_settings",true));finish();
    }
}
