package com.odin2.ds4bridgehelper;

import android.app.*;
import android.content.Intent;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.Field;
import java.util.Map;

/** Tests the dashboard and its real read-only root checks; no mapping writes. */
public final class DiagnosticsSmoke extends Instrumentation {
    private DiagnosticsActivity activity;
    private Throwable failure;
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    private Object field(String name)throws Exception{Field f=DiagnosticsActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private void main(Runnable work){runOnMainSync(()->{try{work.run();}catch(Throwable error){failure=error;}});}
    @SuppressWarnings("unchecked") private void exercise()throws Exception{
        InputDevice selected=(InputDevice)field("selected");check(selected!=null,"No physical controller selected");
        check(((TextView)field("controllerHeading")).getText().toString().contains("Controller Selected"),"Selected controller missing");
        check(((View)field("technical")).getVisibility()==View.GONE,"Technical section not collapsed initially");
        Map<String,TextView> buttons=(Map<String,TextView>)field("buttons");
        check(!buttons.containsKey("Home"),"Reserved Home button remains on dashboard");
        check(!buttons.containsKey("Back"),"Back remains on dashboard");
        long now=SystemClock.uptimeMillis();
        activity.dispatchKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BUTTON_A,0,0,selected.getId(),304,0,InputDevice.SOURCE_GAMEPAD));
        check(buttons.get("A").isActivated(),"Button A not highlighted");
        check(((View)field("controllerView")).getContentDescription().toString().contains("Pressed: [A]"),"Gamepad diagram button not highlighted");
        activity.dispatchKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BUTTON_A,0,0,selected.getId(),304,0,InputDevice.SOURCE_GAMEPAD));
        check(!buttons.get("A").isActivated(),"Released button remains highlighted");
        activity.dispatchKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BUTTON_SELECT,0,0,selected.getId(),314,0,InputDevice.SOURCE_GAMEPAD));
        check(buttons.get("Select").isActivated(),"Select does not highlight");
        activity.dispatchKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BUTTON_SELECT,0,0,selected.getId(),314,0,InputDevice.SOURCE_GAMEPAD));
        check(!buttons.get("Select").isActivated(),"Released Select remains highlighted");
        String last=((TextView)field("lastInput")).getText().toString();
        for(int id:InputDevice.getDeviceIds()){
            InputDevice device=InputDevice.getDevice(id);
            if(device!=null&&device.getVendorId()==0x054c&&device.getProductId()==0x09cc){
                activity.dispatchKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BUTTON_A,0,0,id,305,0,InputDevice.SOURCE_GAMEPAD));
                check(((TextView)field("lastInput")).getText().toString().equals(last),"Virtual controller duplicate changed dashboard");
            }
        }
        MotionEvent.PointerProperties p=new MotionEvent.PointerProperties();p.id=0;
        MotionEvent.PointerCoords c=new MotionEvent.PointerCoords();c.setAxisValue(MotionEvent.AXIS_X,.5f);c.setAxisValue(MotionEvent.AXIS_Y,-.25f);
        c.setAxisValue(MotionEvent.AXIS_Z,-.75f);c.setAxisValue(MotionEvent.AXIS_RZ,1f);c.setAxisValue(MotionEvent.AXIS_BRAKE,.6f);c.setAxisValue(MotionEvent.AXIS_GAS,.8f);c.setAxisValue(MotionEvent.AXIS_HAT_X,-1);
        MotionEvent event=MotionEvent.obtain(now,now,MotionEvent.ACTION_MOVE,1,new MotionEvent.PointerProperties[]{p},new MotionEvent.PointerCoords[]{c},0,0,1,1,selected.getId(),0,InputDevice.SOURCE_JOYSTICK,0);
        activity.dispatchGenericMotionEvent(event);event.recycle();
        check(((View)field("sticks")).getContentDescription().toString().contains("X 0.50 Y -0.25"),"Left stick readings incorrect");
        check(((View)field("sticks")).getContentDescription().toString().contains("X -0.75 Y 1.00"),"Right stick readings incorrect");
        check(((View)field("controllerView")).getContentDescription().toString().contains("Left stick X 0.50 Y -0.25"),"Gamepad diagram stick readings incorrect");
        check(((ProgressBar)field("leftTrigger")).getProgress()==60&&((ProgressBar)field("rightTrigger")).getProgress()==80,"Trigger meters incorrect");
        check(buttons.get("D-pad left").isActivated()&&((TextView)field("dpadLabel")).getText().toString().equals("D-pad: left"),"D-pad indicator incorrect");
        ((Button)field("detailsButton")).performClick();
        check(((View)field("technical")).getVisibility()==View.VISIBLE,"Technical section did not expand");
        check(((TextView)field("output")).getText().toString().contains("Raven Gamepad "+activity.getPackageManager().getPackageInfo(activity.getPackageName(),0).versionName),"Version missing from report");
        Button checkButton=(Button)field("checkButton");
        if((Boolean)field("landscape")){
            android.view.ViewGroup actions=(android.view.ViewGroup)checkButton.getParent();
            int width=actions.getChildAt(0).getWidth(),height=actions.getChildAt(0).getHeight(),top=actions.getChildAt(0).getTop();
            for(int i=0;i<actions.getChildCount();i++){
                View action=actions.getChildAt(i);check(Math.abs(action.getWidth()-width)<=1,"Action widths differ");check(action.getHeight()==height&&action.getTop()==top,"Action heights/alignment differ");
            }
        }
        check(DiagnosticsActivity.checkPopup("Checks passed: long detailed result").equals("Bridge checks passed"),"Success popup not shortened");
        check(DiagnosticsActivity.checkPopup("Check failed: long detailed error").equals("Bridge check failed"),"Failure popup not shortened");
        check(DiagnosticsActivity.checkPopup("Privileged access works, but component inspection failed.").equals("Bridge checks: see results"),"Warning popup not shortened");
        ((Button)field("detailsButton")).performClick();
        check(((View)field("technical")).getVisibility()==View.GONE,"Technical section did not collapse");
        ((Button)field("checkButton")).performClick();
        check((Boolean)field("busy")&&!((Button)field("checkButton")).isEnabled(),"Check did not show running state");
        check(((View)field("checking")).getVisibility()==View.VISIBLE,"Check progress indicator missing");
        check(((TextView)field("checkStatus")).getText().toString().startsWith("Checking"),"Immediate status message missing");
    }
    private View refreshButton(View view){if(view instanceof Button&&((Button)view).getText().toString().equals("Refresh controllers"))return view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View found=refreshButton(group.getChildAt(i));if(found!=null)return found;}}return null;}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            activity=(DiagnosticsActivity)startActivitySync(new Intent(getTargetContext(),DiagnosticsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
            main(()->{View refresh=refreshButton(activity.getWindow().getDecorView());check(refresh!=null,"Refresh button missing");refresh.performClick();});SystemClock.sleep(350);
            try(ParcelFileDescriptor fd=getUiAutomation().executeShellCommand("screencap -p /sdcard/raven-refresh-feedback.png");java.io.InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)){byte[] buffer=new byte[1024];while(in.read(buffer)!=-1){}}
            main(()->{try{exercise();}catch(Exception error){throw new RuntimeException(error);}});
            if(failure!=null)throw new Exception("Dashboard regression",failure);
            final boolean[] busy={true};long until=SystemClock.uptimeMillis()+20000;
            while(busy[0]&&SystemClock.uptimeMillis()<until){SystemClock.sleep(100);main(()->{try{busy[0]=(Boolean)field("busy");}catch(Exception error){throw new RuntimeException(error);}});}
            check(!busy[0],"Root check timed out");
            main(()->{try{
                String summary=((TextView)field("checkStatus")).getText().toString();
                check(summary.startsWith("Checks passed:"),"Unexpected check result: "+summary);
                check(((Button)field("checkButton")).isEnabled()&&((View)field("checking")).getVisibility()==View.GONE,"Check controls did not return to idle");
                check(((String)field("root")).contains("uid=0(root)"),"Root report missing");
            }catch(Exception error){throw new RuntimeException(error);}});
            if(failure!=null)throw new Exception("Check feedback regression",failure);
            result.putString("stream","PASS: selected physical controller, live buttons, virtual-input filtering, stick positions, trigger meters, D-pad, collapsible report and real root-check progress/result.\n");
            main(()->activity.finish());finish(0,result);
        }catch(Throwable error){if(activity!=null)main(()->activity.finish());result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(error));finish(1,result);}
    }
}
