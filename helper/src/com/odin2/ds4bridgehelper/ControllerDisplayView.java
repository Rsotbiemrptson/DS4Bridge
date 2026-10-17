package com.odin2.ds4bridgehelper;

import android.content.Context;
import android.graphics.*;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import java.util.*;

/** Original, mode-aware gamepad diagram inspired by the Odin Key Test layout. */
final class ControllerDisplayView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Set<String> pressed=new HashSet<>();
    private final int foreground,accent;
    private boolean odin=true;
    private float lx,ly,rx,ry,l2,r2;
    ControllerDisplayView(Context context){super(context);foreground=new TextView(context).getCurrentTextColor();TypedValue value=new TypedValue();context.getTheme().resolveAttribute(android.R.attr.colorAccent,value,true);accent=value.data;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
    void setMode(boolean value){odin=value;describe();invalidate();}
    void setPressed(Set<String> keys,Set<String> hats){pressed.clear();pressed.addAll(keys);pressed.addAll(hats);describe();invalidate();}
    void setAxes(float x,float y,float z,float rz,float left,float right){lx=x;ly=y;rx=z;ry=rz;l2=left;r2=right;describe();invalidate();}
    private void describe(){setContentDescription(String.format(Locale.ROOT,"%s controller. Pressed: %s. Left stick X %.2f Y %.2f. Right stick X %.2f Y %.2f. L2 %.0f percent. R2 %.0f percent.",odin?"Odin":"Xbox",pressed,lx,ly,rx,ry,l2*100,r2*100));}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);float scale=Math.min(getWidth()/1000f,getHeight()/330f);
        canvas.save();canvas.translate((getWidth()-1000*scale)/2,(getHeight()-330*scale)/2);canvas.scale(scale,scale);
        key(canvas,62,40,"L1","L1",false);key(canvas,142,40,"L2","L2",l2>.05f);
        key(canvas,858,40,"R1","R1",false);key(canvas,938,40,"R2","R2",r2>.05f);
        key(canvas,460,48,"Select","Select",false);key(canvas,540,48,"Start","Start",false);
        key(canvas,130,132,"Up","D-pad up",false);key(canvas,74,188,"Left","D-pad left",false);key(canvas,186,188,"Right","D-pad right",false);key(canvas,130,244,"Down","D-pad down",false);
        key(canvas,870,132,odin?"X":"Y",odin?"X":"Y",false);key(canvas,814,188,odin?"Y":"X",odin?"Y":"X",false);
        key(canvas,926,188,odin?"A":"B",odin?"A":"B",false);key(canvas,870,244,odin?"B":"A",odin?"B":"A",false);
        stick(canvas,355,185,lx,ly,"L3","Left stick");stick(canvas,645,185,rx,ry,"R3","Right stick");
        meter(canvas,250,298,l2,"L2");meter(canvas,590,298,r2,"R2");canvas.restore();
    }
    private void key(Canvas canvas,float x,float y,String label,String input,boolean analog){
        boolean down=pressed.contains(input)||analog;
        paint.setStyle(Paint.Style.FILL);paint.setColor(accent);paint.setAlpha(down?120:15);canvas.drawCircle(x,y,30,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(down?3:1.6f);paint.setColor(down?accent:foreground);paint.setAlpha(down?255:150);canvas.drawCircle(x,y,30,paint);
        paint.setStyle(Paint.Style.FILL);paint.setColor(foreground);paint.setAlpha(255);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(label.length()>3?17:22);canvas.drawText(label,x,y-(paint.ascent()+paint.descent())/2,paint);
    }
    private void stick(Canvas canvas,float x,float y,float axisX,float axisY,String click,String title){
        paint.setColor(foreground);paint.setAlpha(140);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);canvas.drawCircle(x,y,77,paint);canvas.drawLine(x-77,y,x+77,y,paint);canvas.drawLine(x,y-77,x,y+77,paint);
        paint.setStyle(Paint.Style.FILL);paint.setColor(accent);paint.setAlpha(pressed.contains(click)?255:160);canvas.drawCircle(x+Math.max(-1,Math.min(1,axisX))*77,y+Math.max(-1,Math.min(1,axisY))*77,20,paint);
        paint.setColor(foreground);paint.setAlpha(255);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(20);canvas.drawText(title+" / "+click,x,y-90,paint);
        paint.setTextSize(18);canvas.drawText(String.format(Locale.ROOT,"X %+.2f  Y %+.2f",axisX,axisY),x,y+100,paint);
    }
    private void meter(Canvas canvas,float x,float y,float value,String title){
        paint.setColor(foreground);paint.setAlpha(80);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);canvas.drawRect(x,y,x+160,y+10,paint);
        paint.setStyle(Paint.Style.FILL);paint.setColor(accent);paint.setAlpha(255);canvas.drawRect(x,y,x+160*Math.max(0,Math.min(1,value)),y+10,paint);
        paint.setColor(foreground);paint.setTextSize(18);paint.setTextAlign(Paint.Align.LEFT);canvas.drawText(title+" "+Math.round(value*100)+"%",x+172,y+12,paint);
    }
}
