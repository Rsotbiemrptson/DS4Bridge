package com.odin2.ds4bridgehelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Raven System Control 0.1.21's compact About layout, with Gamepad's own details. */
final class AboutDialog {
    private final Activity activity;
    private final SettingsUi ui;
    private AboutDialog(Activity activity){this.activity=activity;ui=new SettingsUi(activity);}
    static void show(Activity activity){new AboutDialog(activity).show();}
    private TextView text(String value,int size,int color){TextView view=ui.text(value,size);view.setTextColor(color);view.setPadding(0,0,0,0);return view;}
    private AlertDialog.Builder builder(){return new AlertDialog.Builder(activity,activity.getResources().getIdentifier(ui.dark?"RavenAboutDark":"RavenAboutLight","style",activity.getPackageName())){@Override public AlertDialog create(){AlertDialog dialog=super.create();dialog.setOnShowListener(d->{for(int which:new int[]{AlertDialog.BUTTON_POSITIVE,AlertDialog.BUTTON_NEGATIVE,AlertDialog.BUTTON_NEUTRAL}){Button button=dialog.getButton(which);if(button!=null)button.setTextColor(ui.accentText);}});return dialog;}};}
    private void show(){
        LinearLayout content=ui.column();content.setPadding(ui.dp(24),ui.dp(8),ui.dp(24),ui.dp(4));
        ImageView icon=new ImageView(activity);icon.setImageDrawable(activity.getApplicationInfo().loadIcon(activity.getPackageManager()));icon.setContentDescription("Raven Gamepad icon");
        LinearLayout.LayoutParams iconLayout=new LinearLayout.LayoutParams(ui.dp(72),ui.dp(72));iconLayout.gravity=Gravity.CENTER_HORIZONTAL;content.addView(icon,iconLayout);ui.space(content,8);
        TextView name=text("Raven Gamepad",22,ui.foreground);name.setTypeface(null,Typeface.BOLD);name.setGravity(Gravity.CENTER);content.addView(name,new LinearLayout.LayoutParams(-1,-2));
        String version="";try{version=activity.getPackageManager().getPackageInfo(activity.getPackageName(),0).versionName;}catch(Exception ignored){}
        TextView release=text("Version "+version,13,ui.accentText);release.setGravity(Gravity.CENTER);content.addView(release,new LinearLayout.LayoutParams(-1,-2));ui.space(content,8);
        TextView slogan=text("Your controller. Your games.",16,ui.accentText);slogan.setGravity(Gravity.START);content.addView(slogan,new LinearLayout.LayoutParams(-1,-2));ui.space(content,8);
        content.addView(text("Virtual DualShock 4 controls for your handheld, with app selection and button mapping.",14,ui.foreground));ui.space(content,8);
        content.addView(text("Build date: "+BuildInfo.BUILD_DATE+"\nCreator: Rsotbiemrptson",13,ui.secondary));ui.space(content,8);
        content.addView(text("Open source · GNU GPL v3 only",13,ui.secondary));
        TextView license=text("License",14,ui.accentText);license.setPaintFlags(license.getPaintFlags()|Paint.UNDERLINE_TEXT_FLAG);license.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);license.setMinHeight(ui.dp(44));license.setFocusable(true);license.setOnClickListener(v->showLicense());content.addView(license,new LinearLayout.LayoutParams(-2,-2));
        ScrollView scroll=new ScrollView(activity);scroll.addView(content);builder().setTitle("About").setView(scroll).setPositiveButton("Close",null).show();
    }
    private void showLicense(){
        try(InputStream in=activity.getAssets().open("LICENSE.txt")){
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] bytes=new byte[4096];int n;while((n=in.read(bytes))!=-1)out.write(bytes,0,n);
            builder().setTitle("GNU GPL v3 only").setMessage(new String(out.toByteArray(),StandardCharsets.UTF_8)).setPositiveButton("Close",null).show();
        }catch(Exception e){Toast.makeText(activity,"License text could not be opened",Toast.LENGTH_LONG).show();}
    }
}
