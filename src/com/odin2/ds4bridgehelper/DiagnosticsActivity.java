package com.odin2.ds4bridgehelper;

import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.input.InputManager;
import android.os.*;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** A physical-controller dashboard, with opt-in technical details. */
public final class DiagnosticsActivity extends BridgeScreen implements InputManager.InputDeviceListener {
    private TextView output, controllerHeading, lastInput, checkStatus, dpadLabel, leftTriggerLabel, rightTriggerLabel;
    private Button checkButton, detailsButton;
    private ProgressBar checking, leftTrigger, rightTrigger;
    private LinearLayout technical;
    private Spinner chooser;
    private StickDisplayView sticks;
    private ControllerDisplayView controllerView;
    private boolean landscape;
    private InputManager inputManager;
    private InputDevice selected;
    private final List<InputDevice> controllers=new ArrayList<>();
    private final Map<String,TextView> buttons=new LinkedHashMap<>();
    private final Set<String> keyDown=new HashSet<>(), hatDown=new HashSet<>();
    private final StringBuilder events=new StringBuilder();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable saveReport=()->{reportQueued=false;persistReport();};
    private String inventory="", root="Checks have not run.";
    private boolean busy, reportQueued, detailsVisible;
    private long lastMotion;
    private int accent, foreground;

    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        landscape=getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        accent=ui.accent;foreground=ui.foreground;
        LinearLayout page=new LinearLayout(this);page.setOrientation(1);page.setPadding(dp(16),dp(12),dp(16),dp(16));
        controllerHeading=text(page,"No Controller Selected",20);
        chooser=new Spinner(this);page.addView(chooser);
        chooser.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> p){}
            public void onItemSelected(AdapterView<?> p,View view,int position,long id){
                if(position<0||position>=controllers.size())return;
                InputDevice next=controllers.get(position);
                if(selected==null||selected.getId()!=next.getId()){selected=next;resetReadings();}
            }
        });
        if(!landscape){text(page,"Press buttons and move the sticks or triggers. These are physical inputs, before your DS4 mapping. System-reserved buttons may not reach this screen.",14);text(page,"Buttons",18);}
        GridLayout grid=new GridLayout(this);grid.setColumnCount(4);if(!landscape)page.addView(grid);
        String[] names={"A","B","X","Y","L1","R1","L2","R2","L3","R3","Select","Start","D-pad up","D-pad down","D-pad left","D-pad right"};
        for(String name:names){
            TextView chip=new TextView(this);chip.setText(name);chip.setGravity(Gravity.CENTER);chip.setTextSize(14);chip.setPadding(dp(4),dp(8),dp(4),dp(8));
            GridLayout.LayoutParams params=new GridLayout.LayoutParams();params.width=0;params.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);params.setMargins(dp(2),dp(2),dp(2),dp(2));
            grid.addView(chip,params);buttons.put(name,chip);
        }
        controllerView=new ControllerDisplayView(this);if(landscape)page.addView(controllerView,new LinearLayout.LayoutParams(-1,dp(180)));
        sticks=new StickDisplayView(this);if(!landscape)page.addView(sticks,new LinearLayout.LayoutParams(-1,dp(165)));
        LinearLayout triggerRow=new LinearLayout(this);triggerRow.setOrientation(0);if(!landscape)page.addView(triggerRow);
        LinearLayout leftBox=new LinearLayout(this);leftBox.setOrientation(1);leftBox.setPadding(0,0,dp(8),0);triggerRow.addView(leftBox,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout rightBox=new LinearLayout(this);rightBox.setOrientation(1);rightBox.setPadding(dp(8),0,0,0);triggerRow.addView(rightBox,new LinearLayout.LayoutParams(0,-2,1));
        leftTriggerLabel=text(leftBox,"L2: 0%",16);leftTrigger=trigger(leftBox);
        rightTriggerLabel=text(rightBox,"R2: 0%",16);rightTrigger=trigger(rightBox);
        LinearLayout dpadBox=landscape?new LinearLayout(this):page;
        dpadLabel=text(dpadBox,"D-pad: centered",16);
        lastInput=text(page,landscape?"Physical inputs before DS4 mapping - press buttons or move sticks":"Waiting for controller input",landscape?13:16);
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(0);page.addView(actions);
        button(actions,"Refresh controllers",v->{refresh();Toast.makeText(this,"Controllers refreshed: "+controllers.size()+" physical controller"+(controllers.size()==1?"":"s")+" found",Toast.LENGTH_SHORT).show();},true);
        button(actions,"Clear readings",v->{events.setLength(0);resetReadings();updateReport();},true);
        checkButton=button(landscape?actions:page,landscape?"Run bridge checks":"Check root and gamepad interfaces",v->rootCheck(),landscape);
        if(landscape)detailsButton=button(actions,"Technical details",v->showDetails(!detailsVisible),true);
        checkStatus=text(page,"Not checked yet. This check does not change your device.",landscape?13:16);
        checking=new ProgressBar(this);checking.setVisibility(View.GONE);page.addView(checking,new LinearLayout.LayoutParams(dp(32),dp(32)));
        if(!landscape)detailsButton=button(page,"Show technical details",v->showDetails(!detailsVisible),false);
        technical=new LinearLayout(this);technical.setOrientation(1);technical.setVisibility(View.GONE);page.addView(technical);
        LinearLayout reportActions=new LinearLayout(this);reportActions.setOrientation(0);technical.addView(reportActions);
        button(reportActions,"Share report",v->{
            Intent share=new Intent(Intent.ACTION_SEND).setType("text/plain");share.putExtra(Intent.EXTRA_TEXT,report());
            startActivity(Intent.createChooser(share,"Share DS4 diagnostics"));
        },true);
        button(reportActions,"Save report",v->startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE).setType("text/plain").putExtra(Intent.EXTRA_TITLE,"DS4-Bridge-diagnostics.txt"),42),true);
        output=text(technical,"",12);output.setTypeface(android.graphics.Typeface.MONOSPACE);output.setTextIsSelectable(true);
        ScrollView scroll=new ScrollView(this);scroll.addView(page);setContentView(scroll);
        inputManager=(InputManager)getSystemService(INPUT_SERVICE);
        resetReadings();
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private TextView text(LinearLayout page,String value,int size){TextView view=new TextView(this);view.setText(value);view.setTextSize(size);view.setPadding(0,dp(4),0,dp(4));page.addView(view);return view;}
    private Button button(LinearLayout page,String label,View.OnClickListener action,boolean weighted){
        Button view=ui.button(label,false);view.setGravity(Gravity.CENTER);view.setMaxLines(2);view.setEllipsize(android.text.TextUtils.TruncateAt.END);view.setMinWidth(0);view.setMinimumWidth(0);view.setOnClickListener(action);
        LinearLayout.LayoutParams params=weighted?new LinearLayout.LayoutParams(0,dp(56),1):new LinearLayout.LayoutParams(-1,dp(56));params.setMargins(dp(3),dp(2),dp(3),dp(2));
        if(weighted){page.setBaselineAligned(false);page.setGravity(Gravity.CENTER_VERTICAL);}
        page.addView(view,params);return view;
    }
    private ProgressBar trigger(LinearLayout page){ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);page.addView(bar,new LinearLayout.LayoutParams(-1,dp(18)));return bar;}
    @Override protected void onResume(){super.onResume();inputManager.registerInputDeviceListener(this,null);refresh();}
    @Override protected void onPause(){inputManager.unregisterInputDeviceListener(this);handler.removeCallbacks(saveReport);reportQueued=false;persistReport();resetReadings();super.onPause();}
    public void onInputDeviceAdded(int id){refresh();}
    public void onInputDeviceRemoved(int id){refresh();}
    public void onInputDeviceChanged(int id){refresh();}
    private boolean isGamepad(InputDevice device){return device!=null&&(device.supportsSource(InputDevice.SOURCE_GAMEPAD)||device.supportsSource(InputDevice.SOURCE_JOYSTICK));}
    private boolean isVirtual(InputDevice device){return device.getVendorId()==0x054c&&device.getProductId()==0x09cc||device.getName().contains("DS4 Bridge");}
    private void refresh(){
        controllers.clear();List<String> names=new ArrayList<>();int position=0;boolean retained=false;
        for(int id:InputDevice.getDeviceIds()){
            InputDevice device=InputDevice.getDevice(id);if(!isGamepad(device)||isVirtual(device))continue;
            if(selected!=null&&selected.getId()==id&&selected.getProductId()==device.getProductId()&&selected.getVendorId()==device.getVendorId()){position=controllers.size();retained=true;}
            controllers.add(device);names.add(MappingActivity.controllerName(device.getVendorId(),device.getProductId(),device.getName()));
        }
        selected=controllers.isEmpty()?null:controllers.get(position);
        controllerView.setMode(selected!=null&&selected.getVendorId()==0x2020&&selected.getProductId()==0x111);
        chooser.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));chooser.setSelection(position);
        chooser.setVisibility(controllers.size()>1?View.VISIBLE:View.GONE);
        controllerHeading.setText(selected==null?"No Controller Selected":names.get(position));
        if(!retained)resetReadings();
        String version;
        try{version=getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(android.content.pm.PackageManager.NameNotFoundException error){version="unknown";}
        StringBuilder text=new StringBuilder("Raven Gamepad "+version+" diagnostics\n");
        text.append("Model: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append("\nDevice: ").append(Build.DEVICE)
            .append("\nAndroid: ").append(Build.VERSION.RELEASE).append(" API ").append(Build.VERSION.SDK_INT)
            .append("\nABI: ").append(Arrays.toString(Build.SUPPORTED_ABIS)).append("\nCandidate services (names only):\n").append(RootHooks.candidates());
        text.append("\nApp-visible interfaces: uinput=").append(new File("/dev/uinput").exists()).append(" uhid=").append(new File("/dev/uhid").exists()).append('\n');
        for(android.content.pm.PackageInfo pkg:getPackageManager().getInstalledPackages(android.content.pm.PackageManager.GET_SERVICES)){
            if(!pkg.packageName.toLowerCase(Locale.ROOT).contains("mapping"))continue;
            text.append("Mapping package: ").append(pkg.packageName).append(" version=").append(pkg.versionName).append('\n');
            if(pkg.services!=null)for(android.content.pm.ServiceInfo service:pkg.services)text.append("  Service: ").append(service.name).append(" exported=").append(service.exported).append(" permission=").append(service.permission).append('\n');
        }
        for(int id:InputDevice.getDeviceIds()){
            InputDevice device=InputDevice.getDevice(id);if(!isGamepad(device))continue;
            text.append("\nController: ").append(device.getName()).append(isVirtual(device)?" [virtual]":" [physical]")
                .append("\nVID/PID: ").append(device.getVendorId()).append('/').append(device.getProductId()).append(" descriptor=").append(device.getDescriptor()).append('\n');
            for(InputDevice.MotionRange range:device.getMotionRanges())text.append(MotionEvent.axisToString(range.getAxis())).append(" min=").append(range.getMin()).append(" max=").append(range.getMax()).append(" flat=").append(range.getFlat()).append('\n');
        }
        inventory=text.toString();updateReport();
    }
    private void showDetails(boolean visible){detailsVisible=visible;technical.setVisibility(visible?View.VISIBLE:View.GONE);detailsButton.setText(visible?"Hide technical details":"Show technical details");if(visible)output.setText(report());}
    private void resetReadings(){
        keyDown.clear();hatDown.clear();paintButtons();sticks.setValues(0,0,0,0);
        controllerView.setAxes(0,0,0,0,0,0);
        leftTrigger.setProgress(0);rightTrigger.setProgress(0);leftTriggerLabel.setText("L2: 0%");rightTriggerLabel.setText("R2: 0%");dpadLabel.setText("D-pad: centered");lastInput.setText(landscape?"Physical input before DS4 mapping - waiting for input":"Waiting for controller input");lastMotion=0;
    }
    private void paintButtons(){
        controllerView.setPressed(keyDown,hatDown);
        for(Map.Entry<String,TextView> entry:buttons.entrySet()){
            boolean down=keyDown.contains(entry.getKey())||hatDown.contains(entry.getKey());
            if(entry.getValue().getBackground()!=null&&entry.getValue().isActivated()==down)continue;
            GradientDrawable background=new GradientDrawable();background.setCornerRadius(dp(8));background.setStroke(dp(1),down?accent:(foreground&0x00ffffff)|0x44000000);
            background.setColor(down?(accent&0x00ffffff)|0x44000000:Color.TRANSPARENT);
            entry.getValue().setBackground(background);entry.getValue().setTextColor(foreground);entry.getValue().setActivated(down);entry.getValue().setAlpha(down?1f:.85f);
        }
    }
    private String keyName(KeyEvent event){
        if(event.getKeyCode()==KeyEvent.KEYCODE_BUTTON_SELECT)return "Select";
        String name=MappingActivity.inputName(selected.getVendorId(),selected.getProductId(),event.getScanCode());
        if(name.equals("Back / Select"))return "Select";
        if(!name.startsWith("Code "))return name;
        switch(event.getKeyCode()){
            case KeyEvent.KEYCODE_BUTTON_A:return "A";case KeyEvent.KEYCODE_BUTTON_B:return "B";
            case KeyEvent.KEYCODE_BUTTON_X:return "X";case KeyEvent.KEYCODE_BUTTON_Y:return "Y";
            case KeyEvent.KEYCODE_BUTTON_L1:return "L1";case KeyEvent.KEYCODE_BUTTON_R1:return "R1";
            case KeyEvent.KEYCODE_BUTTON_L2:return "L2";case KeyEvent.KEYCODE_BUTTON_R2:return "R2";
            case KeyEvent.KEYCODE_BUTTON_THUMBL:return "L3";case KeyEvent.KEYCODE_BUTTON_THUMBR:return "R3";
            case KeyEvent.KEYCODE_BUTTON_START:return "Start";case KeyEvent.KEYCODE_BUTTON_MODE:case KeyEvent.KEYCODE_HOME:return "Home";
            case KeyEvent.KEYCODE_DPAD_LEFT:return "D-pad left";case KeyEvent.KEYCODE_DPAD_RIGHT:return "D-pad right";
            case KeyEvent.KEYCODE_DPAD_UP:return "D-pad up";case KeyEvent.KEYCODE_DPAD_DOWN:return "D-pad down";
            default:return KeyEvent.keyCodeToString(event.getKeyCode()).replace("KEYCODE_","").replace('_',' ');
        }
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event){
        if(event.getKeyCode()==KeyEvent.KEYCODE_BACK)return super.dispatchKeyEvent(event);
        if(selected!=null&&event.getDeviceId()==selected.getId()){
            if(event.getRepeatCount()==0&&(event.getAction()==KeyEvent.ACTION_DOWN||event.getAction()==KeyEvent.ACTION_UP)){
                String name=keyName(event);if(name.equals("Left stick click"))name="L3";if(name.equals("Right stick click"))name="R3";
                boolean down=event.getAction()==KeyEvent.ACTION_DOWN;if(down)keyDown.add(name);else keyDown.remove(name);
                paintButtons();updateDpad();lastInput.setText("Last input: "+name+(down?" pressed":" released"));
                record("KEY "+selected.getVendorId()+"/"+selected.getProductId()+" "+KeyEvent.keyCodeToString(event.getKeyCode())+" raw="+event.getScanCode()+" action="+event.getAction());
            }
            return true;
        }
        if(isGamepad(event.getDevice()))return true;
        return super.dispatchKeyEvent(event);
    }
    private float axis(MotionEvent event,int code){InputDevice.MotionRange range=selected.getMotionRange(code,InputDevice.SOURCE_JOYSTICK);if(range==null)range=selected.getMotionRange(code);return range==null?0:event.getAxisValue(code);}
    private float triggerValue(MotionEvent event,int preferred,int fallback){return Math.max(0,Math.min(1,Math.max(axis(event,preferred),axis(event,fallback))));}
    @Override public boolean dispatchGenericMotionEvent(MotionEvent event){
        if(event.isFromSource(InputDevice.SOURCE_JOYSTICK)){
            if(selected==null||event.getDeviceId()!=selected.getId())return true;
            float lx=axis(event,MotionEvent.AXIS_X),ly=axis(event,MotionEvent.AXIS_Y),rx=axis(event,MotionEvent.AXIS_Z),ry=axis(event,MotionEvent.AXIS_RZ);
            sticks.setValues(lx,ly,rx,ry);
            int l=Math.round(triggerValue(event,MotionEvent.AXIS_LTRIGGER,MotionEvent.AXIS_BRAKE)*100),r=Math.round(triggerValue(event,MotionEvent.AXIS_RTRIGGER,MotionEvent.AXIS_GAS)*100);
            controllerView.setAxes(lx,ly,rx,ry,l/100f,r/100f);
            leftTrigger.setProgress(l);rightTrigger.setProgress(r);leftTriggerLabel.setText("L2: "+l+"%");rightTriggerLabel.setText("R2: "+r+"%");
            hatDown.clear();float x=axis(event,MotionEvent.AXIS_HAT_X),y=axis(event,MotionEvent.AXIS_HAT_Y);
            if(x<-.5f)hatDown.add("D-pad left");if(x>.5f)hatDown.add("D-pad right");if(y<-.5f)hatDown.add("D-pad up");if(y>.5f)hatDown.add("D-pad down");
            paintButtons();updateDpad();lastInput.setText("Live stick and trigger input received");
            if(SystemClock.uptimeMillis()-lastMotion>150){
                lastMotion=SystemClock.uptimeMillis();StringBuilder line=new StringBuilder("MOTION ");
                for(InputDevice.MotionRange range:selected.getMotionRanges())line.append(MotionEvent.axisToString(range.getAxis())).append('=').append(String.format(Locale.ROOT,"%.3f ",event.getAxisValue(range.getAxis())));
                record(line.toString());
            }
            return true;
        }
        return super.dispatchGenericMotionEvent(event);
    }
    private void updateDpad(){List<String> directions=new ArrayList<>();for(String name:buttons.keySet())if(name.startsWith("D-pad ")&&(keyDown.contains(name)||hatDown.contains(name)))directions.add(name.substring(6));dpadLabel.setText("D-pad: "+(directions.isEmpty()?"centered":android.text.TextUtils.join(" + ",directions)));}
    private void rootCheck(){
        if(busy)return;busy=true;checkButton.setEnabled(false);checkButton.setText("Checking...");checking.setVisibility(View.VISIBLE);
        checkStatus.setText("Checking privileged access and virtual-controller interfaces...");root="Checks running...";updateReport();
        revealCheckStatus();
        new Thread(()->{
            String detail="",summary;
            try{
                String mappingCheck=MappingIntegration.probe(this);
                RootHooks.requireRoot();detail="Privileged access: OK\n";detail+=mappingCheck+"\n"+AppRuntime.status();
                if(!MappingIntegration.supported(this)){
                    summary="Diagnostics completed: privileged access works. Bridging is disabled for this unverified mapping integration; see technical details.";
                    detail+="\nDiagnostic-only/unsupported integration: no component setup, startup or exclusion writes enabled.\n";
                }else{
                String inspection=AppRuntime.inspect();detail+="\n"+inspection;
                boolean available=inspection.contains("HOOK /dev/uinput open succeeded");
                boolean running=!RootHooks.execute("pidof odin_ds4_bridge").isEmpty();
                summary=available&&mappingCheck.contains("read-only connection/list check passed")?"Checks passed: mapping integration, privileged access and gamepad interface available. "+(running?"Bridge is running.":"Bridge is stopped.")
                    :available?"Check completed with a warning: Touch Mapping integration did not verify. See technical details."
                    :"Check completed with a warning: the bridge cannot open the virtual-gamepad interface. See technical details.";
                }
            }catch(Exception error){
                detail+="\n"+error;
                summary=detail.startsWith("Privileged access: OK")?"Privileged access works, but component inspection failed. Try Bridge controls > Install/Update Components. See technical details."
                    :"Check failed: privileged access is unavailable. See technical details for the error.";
            }
            final String result=detail,message=summary;
            runOnUiThread(()->{root=result;busy=false;checkButton.setEnabled(true);checkButton.setText(landscape?"Run bridge checks":"Check root and gamepad interfaces");checking.setVisibility(View.GONE);checkStatus.setText(message);updateReport();revealCheckStatus();Toast.makeText(this,checkPopup(message),Toast.LENGTH_SHORT).show();});
        },"DS4Diagnostics").start();
    }
    static String checkPopup(String summary){
        if(summary.startsWith("Checks passed:"))return "Bridge checks passed";
        if(summary.startsWith("Check failed:"))return "Bridge check failed";
        return "Bridge checks: see results";
    }
    private void revealCheckStatus(){checkStatus.post(()->checkStatus.requestRectangleOnScreen(new android.graphics.Rect(0,0,checkStatus.getWidth(),checkStatus.getHeight()),false));}
    private String report(){return inventory+"\n"+MappingIntegration.summary(this)+"\nSelected physical controller: "+(selected==null?"none":selected.getName())+"\nCheck summary: "+checkStatus.getText()+"\nRoot and raw input:\n"+root+"\nLive input:\n"+events;}
    private void updateReport(){if(detailsVisible)output.setText(report());if(!reportQueued){reportQueued=true;handler.postDelayed(saveReport,500);}}
    private void persistReport(){try(FileOutputStream stream=new FileOutputStream(new File(createDeviceProtectedStorageContext().getFilesDir(),"diagnostics.txt"))){stream.write(report().getBytes(StandardCharsets.UTF_8));}catch(IOException ignored){}}
    private void record(String event){if(events.length()>16000)events.delete(0,8000);events.append(event).append('\n');updateReport();}
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==42&&result==RESULT_OK&&data!=null){try(OutputStream stream=getContentResolver().openOutputStream(data.getData())){
            if(stream==null)throw new IOException("Cannot open report destination");stream.write(report().getBytes(StandardCharsets.UTF_8));Toast.makeText(this,"Report saved",Toast.LENGTH_SHORT).show();
        }catch(IOException error){Toast.makeText(this,error.toString(),Toast.LENGTH_LONG).show();}}
    }
}
