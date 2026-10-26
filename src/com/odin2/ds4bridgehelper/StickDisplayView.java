package com.odin2.ds4bridgehelper;

import android.content.Context;
import android.graphics.*;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import java.util.Locale;

/** Theme-aware live stick positions, normalized to -1..1. */
final class StickDisplayView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int foreground,accent;
    private final float density;
    private float lx,ly,rx,ry;
    StickDisplayView(Context context){
        super(context);density=getResources().getDisplayMetrics().density;
        SettingsUi ui=new SettingsUi((android.app.Activity)context);foreground=ui.foreground;accent=ui.accent;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);setValues(0,0,0,0);
    }
    void setValues(float x,float y,float z,float rz){lx=clamp(x);ly=clamp(y);rx=clamp(z);ry=clamp(rz);setContentDescription(String.format(Locale.ROOT,"Left stick X %.2f Y %.2f. Right stick X %.2f Y %.2f",lx,ly,rx,ry));invalidate();}
    private float clamp(float value){return Math.max(-1,Math.min(1,value));}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);drawStick(canvas,getWidth()*.25f,"Left stick",lx,ly);drawStick(canvas,getWidth()*.75f,"Right stick",rx,ry);}
    private void drawStick(Canvas canvas,float cx,String title,float x,float y){
        float radius=Math.min(getWidth()*.20f,(getHeight()-52*density)*.5f),cy=25*density+radius;
        paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(15*density);paint.setColor(foreground);paint.setAlpha(255);paint.setStyle(Paint.Style.FILL);canvas.drawText(title,cx,18*density,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(density);paint.setAlpha(100);canvas.drawCircle(cx,cy,radius,paint);
        canvas.drawLine(cx-radius,cy,cx+radius,cy,paint);canvas.drawLine(cx,cy-radius,cx,cy+radius,paint);
        paint.setStyle(Paint.Style.FILL);paint.setColor(accent);paint.setAlpha(255);canvas.drawCircle(cx+x*radius,cy+y*radius,7*density,paint);
        paint.setColor(foreground);paint.setTextSize(12*density);canvas.drawText(String.format(Locale.ROOT,"X %+.2f   Y %+.2f",x,y),cx,getHeight()-6*density,paint);
    }
}
