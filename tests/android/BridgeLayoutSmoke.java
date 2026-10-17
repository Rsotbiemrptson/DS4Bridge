package com.odin2.ds4bridgehelper;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.Field;

/** Read-only layout regression: does not start, stop, or reconfigure the bridge. */
public final class BridgeLayoutSmoke extends Instrumentation {
    private Activity screen;
    private Throwable failure;
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle result=new Bundle();
        try{
            screen=startActivitySync(new Intent(getTargetContext(),BridgeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();SystemClock.sleep(1500);
            runOnMainSync(()->{try{
                Field field=BridgeActivity.class.getDeclaredField("advanced");field.setAccessible(true);
                LinearLayout tools=(LinearLayout)field.get(screen);
                check(tools.getVisibility()==View.VISIBLE,"Advanced tools are hidden");
                check(tools.getChildCount()==3,"Expected three rows");
                int count=0,width=0,height=0;
                for(int i=0;i<3;i++){
                    LinearLayout row=(LinearLayout)tools.getChildAt(i);check(row.getChildCount()==2,"Expected two columns");
                    for(int j=0;j<2;j++)if(row.getChildAt(j) instanceof Button){
                        Button button=(Button)row.getChildAt(j);count++;
                        check(button.isShown(),"Hidden action");
                        check(button.getBackground() instanceof android.graphics.drawable.GradientDrawable,"Rounded background missing");
                        if(width==0){width=button.getWidth();height=button.getHeight();}
                        check(Math.abs(width-button.getWidth())<=1&&height==button.getHeight(),"Unequal tool sizes");
                        check(button.getLayout()!=null&&button.getLayout().getLineCount()<=2,"Tool label overflow");
                        check(button.getLayout().getHeight()<=button.getHeight()-button.getCompoundPaddingTop()-button.getCompoundPaddingBottom(),"Tool label clipped");
                    }
                }
                check(count==5,"Expected five existing actions");
                check(height==((BridgeScreen)screen).ui.dp(48),"Unexpected tool height");
                android.graphics.Rect visible=new android.graphics.Rect();
                check(tools.getGlobalVisibleRect(visible)&&visible.height()==tools.getHeight(),"Advanced tools require scrolling");
            }catch(Throwable error){failure=error;}});
            try(ParcelFileDescriptor capture=getUiAutomation().executeShellCommand("screencap -p /sdcard/ds4-controls-0144.png");java.io.InputStream output=new ParcelFileDescriptor.AutoCloseInputStream(capture)){
                byte[] buffer=new byte[1024];while(output.read(buffer)!=-1){}
            }
            if(failure!=null)throw new Exception(failure);
            result.putString("stream","PASS: compact advanced tools, three rows, two columns, equal rounded buttons, visible labels.\n");finish(0,result);
        }catch(Throwable error){result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(error));finish(1,result);}
    }
}
