package com.odin2.ds4bridgehelper;

import android.app.*;
import android.content.DialogInterface;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Foreground scan-code learning plus root-assisted, single-axis capture. */
public final class MappingActivity extends BridgeScreen implements InputManager.InputDeviceListener {
    private final List<InputDevice> devices = new ArrayList<>();
    private final Map<Integer,Integer> keys = new LinkedHashMap<>(), axes = new LinkedHashMap<>();
    private final Set<Integer> inverted = new HashSet<>();
    private InputDevice selected;
    private LinearLayout rows;
    private TextView status;
    private int waiting = -1;
    private boolean busy;
    private boolean dirty;
    private Bundle pendingState;
    private LinearLayout capturePane;
    private TextView learningTitle;
    private Button saveButton,resetButton;
    private Spinner chooser;
    private TextView controllerHeading;
    private InputManager inputManager;
    private static final int[] KEY_TARGETS={305,306,304,307,308,309,310,311,312,313,314,315,316,544,545,546,547};
    private static final String[] KEY_NAMES={"Cross","Circle","Square","Triangle",
        "L1","R1","L2 button","R2 button","Share","Options","L3","R3","PS","D-pad up","D-pad down","D-pad left","D-pad right"};
    private static final int[] AXIS_TARGETS={0,1,2,5,3,4,16,17};
    private static final String[] AXIS_NAMES={"Left stick horizontal","Left stick vertical","Right stick horizontal",
        "Right stick vertical","L2 trigger","R2 trigger","D-pad horizontal","D-pad vertical"};

    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        pendingState=saved;
        boolean wide=getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        LinearLayout page=ui.column();page.setPadding(ui.dp(16),ui.dp(6),ui.dp(16),ui.dp(6));
        TextView title=ui.heading("Controller mapping");controllerHeading=ui.note("No Controller Selected");
        if(wide){LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);ui.equal(header,title);ui.equal(header,controllerHeading);page.addView(header);}else{page.addView(title);page.addView(controllerHeading);}
        chooser=new Spinner(this);page.addView(chooser);
        chooser.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(android.widget.AdapterView<?> p){}
            public void onItemSelected(android.widget.AdapterView<?> p,View v,int position,long id){
                if(position<0||position>=devices.size())return;
                InputDevice next=devices.get(position);
                if(selected!=null&&next.getId()==selected.getId())return;
                navigateAway(()->{cancelLearning();selected=next;load();dirty=false;render();});
            }
        });
        page.addView(ui.note("Hold a button row to learn; tap an axis to record, hold to invert."));
        LinearLayout editor=new LinearLayout(this);editor.setOrientation(wide?0:1);
        ScrollView scroll=new ScrollView(this);rows=ui.column();scroll.addView(rows);
        editor.addView(scroll,wide?new LinearLayout.LayoutParams(0,-1,2):new LinearLayout.LayoutParams(-1,0,1));
        capturePane=ui.card();capturePane.setVisibility(View.GONE);learningTitle=ui.text("Assign button",18);capturePane.addView(learningTitle);
        capturePane.addView(ui.text("Press the desired controller button",16));TextView captureNote=ui.note("Its previous assignment becomes Unassigned.");captureNote.setTextSize(12);capturePane.addView(captureNote);
        Button cancel=ui.button("Cancel",false);cancel.setOnClickListener(v->cancelLearning());capturePane.addView(cancel);
        LinearLayout.LayoutParams captureParams=wide?new LinearLayout.LayoutParams(0,-1,1):new LinearLayout.LayoutParams(-1,-2);captureParams.setMargins(wide?ui.dp(10):0,wide?0:ui.dp(8),0,0);editor.addView(capturePane,captureParams);
        page.addView(editor,new LinearLayout.LayoutParams(-1,0,1));
        status=ui.note("Changes are applied when you save and restart the bridge.");page.addView(status);
        LinearLayout footer=new LinearLayout(this);resetButton=ui.button("Reset mapping",false);saveButton=ui.button("Save and restart bridge",true);ui.equal(footer,resetButton);ui.equal(footer,saveButton);page.addView(footer);
        resetButton.getLayoutParams().height=ui.dp(44);saveButton.getLayoutParams().height=ui.dp(44);
        saveButton.setOnClickListener(v->saveAndRestart());
        resetButton.setOnClickListener(v->{if(selected==null||busy)return;cancelLearning();keys.clear();axes.clear();inverted.clear();defaults();dirty=true;render();status.setText("Defaults restored in the editor. Save and restart to apply them.");});
        setContentView(page);
        inputManager=(InputManager)getSystemService(INPUT_SERVICE);
    }
    @Override protected void onResume(){super.onResume();inputManager.registerInputDeviceListener(this,null);refreshControllers();}
    @Override protected void onPause(){inputManager.unregisterInputDeviceListener(this);cancelLearning();super.onPause();}
    @Override protected void onSaveInstanceState(Bundle out){
        super.onSaveInstanceState(out);if(selected==null)return;
        out.putInt("mapping_vendor",selected.getVendorId());out.putInt("mapping_product",selected.getProductId());out.putSerializable("mapping_keys",new LinkedHashMap<>(keys));out.putSerializable("mapping_axes",new LinkedHashMap<>(axes));out.putSerializable("mapping_inverted",new HashSet<>(inverted));out.putBoolean("mapping_dirty",dirty);
    }
    @SuppressWarnings("unchecked") private void restoreDraft(){
        if(pendingState==null||selected==null)return;
        if(pendingState.getInt("mapping_vendor",-1)==selected.getVendorId()&&pendingState.getInt("mapping_product",-1)==selected.getProductId()&&pendingState.containsKey("mapping_keys")){
            keys.clear();keys.putAll((Map<Integer,Integer>)pendingState.getSerializable("mapping_keys"));axes.clear();axes.putAll((Map<Integer,Integer>)pendingState.getSerializable("mapping_axes"));inverted.clear();inverted.addAll((Set<Integer>)pendingState.getSerializable("mapping_inverted"));dirty=pendingState.getBoolean("mapping_dirty");
        }pendingState=null;
    }
    public void onInputDeviceAdded(int id){refreshControllers();}
    public void onInputDeviceRemoved(int id){refreshControllers();}
    public void onInputDeviceChanged(int id){refreshControllers();}
    private void refreshControllers(){
        List<InputDevice> found=new ArrayList<>();List<String> names=new ArrayList<>();
        int position=0;boolean retained=false;
        for(int id:InputDevice.getDeviceIds()){
            InputDevice d=InputDevice.getDevice(id);
            if(d==null||(!d.supportsSource(InputDevice.SOURCE_GAMEPAD)&&!d.supportsSource(InputDevice.SOURCE_JOYSTICK))
                ||(d.getVendorId()==0x054c&&d.getProductId()==0x09cc)||d.getName().contains("DS4 Bridge"))continue;
            if(selected!=null&&selected.getId()==id&&selected.getVendorId()==d.getVendorId()&&selected.getProductId()==d.getProductId()){
                position=found.size();retained=true;
            }
            found.add(d);names.add(controllerName(d.getVendorId(),d.getProductId(),d.getName()));
        }
        devices.clear();devices.addAll(found);
        if(!retained){cancelLearning();selected=null;keys.clear();axes.clear();inverted.clear();}
        chooser.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));
        chooser.setVisibility(devices.size()>1?View.VISIBLE:View.GONE);
        controllerHeading.setVisibility(devices.size()>1?View.GONE:View.VISIBLE);
        controllerHeading.setText(names.isEmpty()?"No Controller Selected":names.get(0));
        if(!devices.isEmpty()){
            selected=devices.get(position);if(!retained){load();dirty=false;}restoreDraft();render();chooser.setSelection(position);
        }else{render();status.setText("No physical gamepad detected. Connect a controller to continue.");}
    }
    static String controllerName(int vendor,int product,String fallback){
        if(vendor==0x2020&&product==0x111)return "Odin Controller Selected";
        if(vendor==0x2020&&product==0x112)return "Xbox Controller Selected";
        return fallback+" Selected";
    }
    private void cancelLearning(){waiting=-1;if(capturePane!=null)capturePane.setVisibility(View.GONE);renderSelection();}
    private File profileFile(){return new File(createDeviceProtectedStorageContext().getFilesDir(),"controller-"+selected.getVendorId()+"-"+selected.getProductId()+".conf");}
    private void defaults(){
        if(selected==null || selected.getVendorId()!=0x2020 || (selected.getProductId()!=0x111&&selected.getProductId()!=0x112))return;
        int[] raw=defaultButtonCodes(selected.getProductId());
        for(int i=0;i<raw.length;i++)keys.put(KEY_TARGETS[i],raw[i]);
        int[] axis={0,1,2,5,10,9,16,17};
        for(int i=0;i<axis.length;i++)axes.put(AXIS_TARGETS[i],axis[i]);
    }
    private void load(){
        keys.clear();axes.clear();inverted.clear();defaults();
        File file=profileFile();if(!file.exists())return;
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8))){
            keys.clear();axes.clear();String line;
            while((line=reader.readLine())!=null){String[] p=line.split(" ");
                if(p.length==3&&p[0].equals("key"))keys.put(Integer.parseInt(p[2]),Integer.parseInt(p[1]));
                if(p.length==4&&p[0].equals("axis")){int target=Integer.parseInt(p[2]);axes.put(target,Integer.parseInt(p[1]));if(p[3].equals("1"))inverted.add(target);}
            }
        }catch(Exception error){status.setText("Could not read saved mapping: "+error);}
    }
    private void render(){
        rows.removeAllViews();
        LinearLayout titles=new LinearLayout(this);TextView output=ui.note("DS4 output"),input=ui.note("Controller input");output.setPadding(ui.dp(8),ui.dp(2),0,ui.dp(2));input.setPadding(ui.dp(8),ui.dp(2),0,ui.dp(2));ui.equal(titles,output);ui.equal(titles,input);rows.addView(titles);
        for(int i=0;i<KEY_TARGETS.length;i++){
            if(i==0||i==4||i==8||i==13)group(i==0?"Face buttons":i==4?"Shoulders":i==8?"Other buttons":"D-pad buttons");
            final int target=KEY_TARGETS[i];View row=assignmentRow("key:"+target,KEY_NAMES[i],keys.containsKey(target)?inputName(keys.get(target)):"Unassigned");
            row.setOnClickListener(v->learn(target));row.setOnLongClickListener(v->{learn(target);return true;});
        }
        for(int i=0;i<AXIS_TARGETS.length;i++){
            if(i==0||i==4||i==6)group(i==0?"Stick axes":i==4?"Analog triggers":"D-pad axes");
            final int target=AXIS_TARGETS[i];final String name=AXIS_NAMES[i];View row=assignmentRow("axis:"+target,name,(axes.containsKey(target)?axisInputName(axes.get(target)):"Unassigned")+(inverted.contains(target)?" (inverted)":""));
            row.setOnClickListener(v->captureAxis(target,name));row.setOnLongClickListener(v->{axisOptions(target);return true;});
        }
        renderSelection();
    }
    private void group(String label){TextView title=ui.text(label,13);title.setPadding(ui.dp(8),ui.dp(3),ui.dp(8),ui.dp(3));title.setTypeface(null,android.graphics.Typeface.BOLD);title.setBackgroundColor(ui.panel);rows.addView(title);}
    private View assignmentRow(String tag,String name,String input){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(ui.dp(10),ui.dp(2),ui.dp(10),ui.dp(2));row.setMinimumHeight(ui.dp(40));row.setTag(tag);row.setContentDescription(name+" - "+input);row.setFocusable(true);row.setClickable(true);
        TextView output=ui.text(name,14),value=ui.text(input,14);if(input.equals("Unassigned"))value.setTextColor(ui.secondary);ui.equal(row,output);ui.equal(row,value);rows.addView(row);rows.addView(ui.divider());return row;
    }
    private void renderSelection(){if(rows==null)return;for(int i=0;i<rows.getChildCount();i++){View row=rows.getChildAt(i);if(row.getTag()!=null){boolean selected=row.getTag().equals("key:"+waiting);row.setActivated(selected);if(selected)row.setBackground(ui.background(true));else row.setBackgroundColor(ui.background);}}}
    static int[] defaultButtonCodes(int product){
        boolean odin=product==0x111;
        return new int[]{odin?305:304,odin?304:305,odin?308:307,odin?307:308,
            310,311,312,313,314,315,317,318,316,544,545,546,547};
    }
    private String inputName(int code){
        return inputName(selected.getVendorId(),selected.getProductId(),code);
    }
    static String inputName(int vendor,int product,int code){
        if(vendor==0x2020&&(product==0x111||product==0x112)){
            boolean odin=product==0x111;
            int[] codes=defaultButtonCodes(product);
            String[] names={odin?"B":"A",odin?"A":"B",odin?"Y":"X",odin?"X":"Y","L1","R1",
                "L2","R2","Back / Select","Start","Left stick click","Right stick click","Home",
                "D-pad up","D-pad down","D-pad left","D-pad right"};
            for(int i=0;i<codes.length;i++)if(codes[i]==code)return names[i];
        }
        return "Code "+code;
    }
    private String axisInputName(int code){
        if(selected!=null&&selected.getVendorId()==0x2020){
            int[] codes={0,1,2,5,10,9,16,17};
            for(int i=0;i<codes.length;i++)if(codes[i]==code)return AXIS_NAMES[i];
        }
        return "Axis "+code;
    }
    private void learn(int target){
        if(selected==null||busy)return;
        waiting=target;
        learningTitle.setText("Assign "+KEY_NAMES[indexOf(KEY_TARGETS,target)]);capturePane.setVisibility(View.VISIBLE);renderSelection();
    }
    private static int indexOf(int[] values,int value){for(int i=0;i<values.length;i++)if(values[i]==value)return i;return 0;}
    private void finishLearning(int source){
        if(waiting<0)return;
        int target=waiting;waiting=-1;assign(keys,target,source);
        cancelLearning();
        status.setText("Button assigned. Any previous assignment of that input is now Unassigned. Save when finished.");
    }
    private boolean captureButton(KeyEvent event){
        if(waiting<0||selected==null||event.getDeviceId()!=selected.getId())return false;
        if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0){
            int raw=event.getScanCode();
            if(raw<=0)switch(event.getKeyCode()){
                case KeyEvent.KEYCODE_DPAD_UP:raw=544;break;
                case KeyEvent.KEYCODE_DPAD_DOWN:raw=545;break;
                case KeyEvent.KEYCODE_DPAD_LEFT:raw=546;break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:raw=547;break;
            }
            if(raw>0)finishLearning(raw);
        }
        return true;
    }
    private boolean captureHat(MotionEvent event){
        if(waiting<0||selected==null||event.getDeviceId()!=selected.getId())return false;
        float x=event.getAxisValue(MotionEvent.AXIS_HAT_X),y=event.getAxisValue(MotionEvent.AXIS_HAT_Y);
        if(Math.abs(x)>.5f)finishLearning(x<0?546:547);
        else if(Math.abs(y)>.5f)finishLearning(y<0?544:545);
        return true;
    }
    private void assign(Map<Integer,Integer> map,int target,int source){
        List<Integer> remove=new ArrayList<>();for(Map.Entry<Integer,Integer> row:map.entrySet())if(row.getValue()==source)remove.add(row.getKey());
        for(Integer old:remove)map.remove(old);map.put(target,source);dirty=true;render();
    }
    private void axisOptions(int target){
        if(selected==null||busy)return;
        CheckBox invert=new CheckBox(this);invert.setText("Invert axis");invert.setChecked(inverted.contains(target));
        new AlertDialog.Builder(this).setTitle("Axis settings").setView(invert).setPositiveButton("Save",(d,w)->{
            if(invert.isChecked())inverted.add(target);else inverted.remove(target);dirty=true;render();
        }).setNeutralButton("Clear",(d,w)->{axes.remove(target);inverted.remove(target);dirty=true;render();}).setNegativeButton("Cancel",null).show();
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event){
        return captureButton(event)||super.dispatchKeyEvent(event);
    }
    @Override public boolean dispatchGenericMotionEvent(MotionEvent event){return captureHat(event)||super.dispatchGenericMotionEvent(event);}
    private void captureAxis(int target,String name){
        if(selected==null||busy)return;busy=true;
        final int vendor=selected.getVendorId(),product=selected.getProductId();
        status.setText("For five seconds, move only "+name+" through its full range.");
        new Thread(() -> {
            String message;
            try{RootHooks.requireRoot();message=RootHooks.execute(AppRuntime.STATE+"/odin_ds4_bridge --capture-axis "+vendor+" "+product);}
            catch(Exception error){message="Set up app components and run root diagnostics first: "+error;}
            final String result=message;
            runOnUiThread(()->{busy=false;
                if(selected!=null&&selected.getVendorId()==vendor&&selected.getProductId()==product&&result.startsWith("CAPTURE_AXIS ")){
                    try{assign(axes,target,Integer.parseInt(result.substring(13).trim()));status.setText("Axis learned. Save when finished.");}
                    catch(Exception error){status.setText(result);}
                }else status.setText(result);
            });
        },"DS4AxisCapture").start();
    }
    private boolean save(){
        if(selected==null||busy)return false;
        StringBuilder profile=new StringBuilder("device "+selected.getVendorId()+" "+selected.getProductId()+"\n");
        for(Map.Entry<Integer,Integer> row:keys.entrySet())profile.append("key ").append(row.getValue()).append(' ').append(row.getKey()).append('\n');
        for(Map.Entry<Integer,Integer> row:axes.entrySet())profile.append("axis ").append(row.getValue()).append(' ').append(row.getKey()).append(' ').append(inverted.contains(row.getKey())?1:0).append('\n');
        try{write(profileFile(),profile.toString());write(new File(createDeviceProtectedStorageContext().getFilesDir(),"profile.conf"),profile.toString());
            dirty=false;status.setText("Mapping saved.");return true;
        }catch(IOException error){status.setText(error.toString());return false;}
    }
    private void saveAndRestart(){
        if(!save())return;cancelLearning();busy=true;saveButton.setEnabled(false);resetButton.setEnabled(false);status.setText("Mapping saved. Restarting bridge...");
        new Thread(()->{String message;try{message=AppRuntime.restartAndWait();}catch(Exception error){message="Mapping saved, but restart failed: "+error.getMessage();}final String result=message;runOnUiThread(()->{if(isDestroyed())return;busy=false;saveButton.setEnabled(true);resetButton.setEnabled(true);status.setText(result);});},"DS4SaveMapping").start();
    }
    @Override protected void navigateAway(Runnable action){
        if(busy){Toast.makeText(this,"Wait for the operation to finish",Toast.LENGTH_SHORT).show();return;}
        if(!dirty){super.navigateAway(action);return;}
        new AlertDialog.Builder(this).setTitle("Unsaved mapping").setMessage("Discard these edits and leave this page?").setNegativeButton("Keep editing",null).setPositiveButton("Discard",(d,w)->{dirty=false;action.run();}).show();
    }
    private void write(File target,String text)throws IOException{
        File temp=new File(target.getParentFile(),target.getName()+".tmp");
        try(FileOutputStream stream=new FileOutputStream(temp)){stream.write(text.getBytes(StandardCharsets.UTF_8));stream.getFD().sync();}
        if(!temp.renameTo(target))throw new IOException("Cannot save mapping");
    }
}
