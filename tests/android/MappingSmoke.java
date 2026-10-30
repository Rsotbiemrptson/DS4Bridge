package com.odin2.ds4bridgehelper;

import android.app.*;
import android.content.Intent;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.Field;
import java.util.*;

/** Exercises the inline learning pane without saving or resetting user profiles. */
public final class MappingSmoke extends Instrumentation {
    private MappingActivity activity;
    private Throwable failure;
    private boolean update;
    @Override public void onCreate(Bundle args){super.onCreate(args);update=args!=null&&Boolean.parseBoolean(args.getString("update"));start();}
    private Object field(String name)throws Exception{
        Field f=MappingActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);
    }
    private Button button(View view,String prefix){
        if(view instanceof Button&&((Button)view).getText().toString().startsWith(prefix))return (Button)view;
        if(view instanceof android.view.ViewGroup){
            android.view.ViewGroup group=(android.view.ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++){Button b=button(group.getChildAt(i),prefix);if(b!=null)return b;}
        }
        return null;
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private View row(View rows,int target){return rows.findViewWithTag("key:"+target);}
    private void englishRows(View view){
        if(view instanceof TextView){String text=((TextView)view).getText().toString();check(!text.toLowerCase(Locale.ROOT).contains("raw"),"Raw text remains in row");for(int i=0;i<text.length();i++)check(text.charAt(i)<128,"Encoding artifact in row");}
        if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++)englishRows(group.getChildAt(i));}
    }
    @SuppressWarnings("unchecked") private void exercise()throws Exception{
        InputDevice device=(InputDevice)field("selected");check(device!=null,"No physical controller selected");
        Map<Integer,Integer> keys=(Map<Integer,Integer>)field("keys");
        View rows=(View)field("rows");
        if(((java.util.List<?>)field("devices")).size()==1){
            check(((View)field("chooser")).getVisibility()==View.GONE,"Single-controller dropdown remains visible");
            check(((TextView)field("controllerHeading")).getText().toString().equals(
                MappingActivity.controllerName(device.getVendorId(),device.getProductId(),device.getName())),"Plain controller heading incorrect");
        }
        check(MappingActivity.controllerName(0x2020,0x111,"").equals("Odin Controller Selected"),"Odin heading incorrect");
        check(MappingActivity.controllerName(0x2020,0x112,"").equals("Xbox Controller Selected"),"Xbox heading incorrect");
        String[][] expected={{"B","A","Y","X"},{"A","B","X","Y"}};
        for(int mode=0;mode<2;mode++){
            int product=0x111+mode;int[] defaults=MappingActivity.defaultButtonCodes(product);
            for(int i=0;i<4;i++)check(MappingActivity.inputName(0x2020,product,defaults[i]).equals(expected[mode][i]),"Face labels incorrect for mode "+mode);
        }
        check(MappingActivity.inputName(0x2020,0x111,314).equals("Select"),"Share input should say Select only");
        check(rows.findViewWithTag("axis:16")==null&&rows.findViewWithTag("axis:17")==null,"D-pad axis rows remain");
        View trigger=rows.findViewWithTag("axis:3");check(trigger!=null&&trigger.getParent()==row(rows,308).getParent(),"L2 trigger not grouped with Shoulders");
        View body=(View)trigger.getParent();android.view.ViewGroup card=(android.view.ViewGroup)body.getParent();View header=card.getChildAt(0);
        check(header.getContentDescription().equals("Collapse Shoulders"),"Shoulders header missing");header.performClick();check(body.getVisibility()==View.GONE,"Section did not collapse");
        header.performClick();check(body.getVisibility()==View.VISIBLE,"Section did not expand");
        check(row(rows,314).getParent()==rows.findViewWithTag("axis:0").getParent()&&row(rows,315).getParent()==rows.findViewWithTag("axis:0").getParent(),"Stick clicks not in Analog Sticks");
        for(int target:new int[]{312,313,316})check(row(rows,target).getParent()==row(rows,305).getParent(),"Remaining utility button not in Face buttons");
        View l1=row(rows,308);check(l1!=null,"L1 row missing");
        l1.performLongClick();View pane=(View)field("capturePane");check(pane.getVisibility()==View.VISIBLE,"Hold did not show learning pane");
        check(((TextView)field("learningTitle")).getText().toString().equals("Assign L1"),"Learning target missing");
        check(button(pane,"Cancel")!=null,"Cancel missing");
        check(button(pane,"R1")==null&&button(pane,"Advanced")==null,"Unexpected input choices/editor");
        Map<Integer,Integer> before=new LinkedHashMap<>(keys);
        button(pane,"Cancel").performClick();
        check(keys.equals(before)&&(Integer)field("waiting")==-1&&pane.getVisibility()==View.GONE,"Cancel changed mapping or left learning active");
        row(rows,308).performLongClick();
        long now=SystemClock.uptimeMillis();
        activity.dispatchKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_LEFT,
            0,0,device.getId(),546,0,InputDevice.SOURCE_GAMEPAD));
        check(keys.get(308)==546&&!keys.containsKey(546),"Physical key did not replace L1 and clear D-pad left");
        check(row(rows,308).getContentDescription().toString().equals("L1 - D-pad left"),"Readable input label missing");
        check(row(rows,546).getContentDescription().toString().equals("D-pad left - Unassigned"),"Displaced row is not blank");
        row(rows,308).performLongClick();
        // Android also produces D-pad key events with scan code zero.
        activity.dispatchKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_LEFT,
            0,0,device.getId(),0,0,InputDevice.SOURCE_GAMEPAD));
        check(keys.get(308)==546,"Zero-scan-code D-pad event was not learned");
        row(rows,308).performLongClick();
        MotionEvent.PointerProperties p=new MotionEvent.PointerProperties();p.id=0;p.toolType=MotionEvent.TOOL_TYPE_UNKNOWN;
        MotionEvent.PointerCoords c=new MotionEvent.PointerCoords();c.setAxisValue(MotionEvent.AXIS_HAT_X,1);
        MotionEvent motion=MotionEvent.obtain(now,now,MotionEvent.ACTION_MOVE,1,new MotionEvent.PointerProperties[]{p},
            new MotionEvent.PointerCoords[]{c},0,0,1,1,device.getId(),0,InputDevice.SOURCE_JOYSTICK,0);
        activity.dispatchGenericMotionEvent(motion);motion.recycle();
        check(keys.get(308)==547&&!keys.containsKey(547),"D-pad HAT was not learned exclusively");
        check(pane.getVisibility()==View.GONE,"Learning pane did not close");englishRows(rows);
    }
    @Override public void onStart(){
        Bundle results=new Bundle();
        try{
            activity=(MappingActivity)startActivitySync(new Intent(getTargetContext(),MappingActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();
            runOnMainSync(()->{try{exercise();}catch(Throwable error){failure=error;}});
            if(failure!=null)throw new Exception("UI regression",failure);
            ActivityMonitor monitor=addMonitor(MappingActivity.class.getName(),null,false);
            runOnMainSync(()->activity.recreate());
            MappingActivity recreated=(MappingActivity)monitor.waitForActivityWithTimeout(3000);removeMonitor(monitor);
            check(recreated!=null,"Mapping did not recreate");activity=recreated;waitForIdleSync();
            runOnMainSync(()->{try{
                @SuppressWarnings("unchecked") Map<Integer,Integer> restored=(Map<Integer,Integer>)field("keys");
                check(restored.get(308)==547&&(Boolean)field("dirty"),"Unsaved draft lost during recreation");
            }catch(Throwable error){failure=error;}finally{activity.finish();}});
            if(failure!=null)throw new Exception("Draft state regression",failure);
            if(update){
                AppRuntime.setup(getTargetContext());
                check("17".equals(RootHooks.execute("cat "+AppRuntime.STATE+"/ready")),"Components not updated");
                String started=AppRuntime.start();
                check(started.contains("START_REQUESTED"),"Updated bridge did not start: "+started);
                results.putString("runtime",started);
            }
            results.putString("stream","PASS: long-press, inline learning pane, Cancel, mode headings, Odin/Xbox ABXY labels, zero-scan D-pad, HAT capture, exclusive assignment and unsaved draft recreation; saved profiles unchanged.\n");
            finish(0,results);
        }catch(Throwable error){results.putString("stream","FAIL: "+android.util.Log.getStackTraceString(error));finish(1,results);}
    }
}
