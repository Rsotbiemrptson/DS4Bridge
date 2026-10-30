package com.odin2.ds4bridgehelper;

import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/** Tap cycles verified native modes and Raven's all-app override. Never run root work on the UI thread. */
public final class ControllerStyleTile extends TileService {
    private static final ExecutorService worker=Executors.newSingleThreadExecutor();
    private static final AtomicBoolean busy=new AtomicBoolean();
    private final Handler main=new Handler(Looper.getMainLooper());
    @Override public void onStartListening(){super.onStartListening();refresh();}
    private void refresh(){worker.execute(()->{try{int mode=ControllerMode.current(this);main.post(()->display(mode,null));}catch(Exception error){main.post(()->display(-1,"Open Raven Gamepad"));}});}
    private void display(int mode,String message){Tile tile=getQsTile();if(tile==null)return;tile.setLabel("Raven Gamepad");tile.setSubtitle(message!=null?message:ControllerMode.label(mode)+(mode==ControllerMode.ODIN||mode==ControllerMode.XBOX?" / Apps":mode==ControllerMode.RAVEN?" - All apps":""));tile.setState(mode<0?Tile.STATE_UNAVAILABLE:mode==ControllerMode.DISCONNECTED?Tile.STATE_INACTIVE:Tile.STATE_ACTIVE);tile.setContentDescription("Controller style: "+(message!=null?message:ControllerMode.label(mode))+". Tap to cycle Odin, Xbox, Raven Gamepad and Disconnected.");tile.updateTile();}
    @Override public void onClick(){super.onClick();if(isLocked()){unlockAndRun(this::change);return;}change();}
    private void change(){
        if(!busy.compareAndSet(false,true))return;display(-1,"Switching...");
        worker.execute(()->{String message;try{message=ControllerMode.cycle(this);}catch(Exception error){message="Could not switch: "+error.getMessage();}finally{busy.set(false);}final String result=message;main.post(()->{Toast.makeText(this,result,Toast.LENGTH_LONG).show();refresh();});});
    }
}
