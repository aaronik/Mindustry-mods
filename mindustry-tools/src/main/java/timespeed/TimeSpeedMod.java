package timespeed;

import arc.ApplicationCore;
import arc.ApplicationListener;
import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.game.EventType.ResetEvent;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.EventType.Trigger;
import mindustry.mod.Mod;
import java.lang.reflect.Field;

/** Repeats real simulation ticks rather than multiplying per-update elapsed time. */
public class TimeSpeedMod extends Mod{
    private static final long frameBudget=12_000_000L;
    private int multiplier=1;
    private boolean installed;
    private long worldEpoch;

    public TimeSpeedMod(){
        Events.on(ClientLoadEvent.class,event->install());
        Events.on(WorldLoadEvent.class,event->reset());
        Events.on(ResetEvent.class,event->reset());
        Events.run(Trigger.update,()->{if(!available())multiplier=1;});
    }
    public boolean enabled(){return multiplier!=1;}
    public int multiplier(){return multiplier;}
    public void select(int requested){
        if(!SpeedPolicy.preset(requested))return;
        if(multiplier==requested){multiplier=1;return;}
        if(!available()){
            toast("Speed controls are available only in local single-player games.");return;
        }
        if(!installed){toast("Speed controls could not attach to this game build.");return;}
        multiplier=requested;
    }
    private void reset(){multiplier=1;worldEpoch++;}
    private boolean available(){
        return SpeedPolicy.available(Vars.state!=null && Vars.state.isGame(),
            Vars.net==null || Vars.net.active(),Vars.state!=null && Vars.state.isEditor(),
            Vars.state!=null && Vars.state.gameOver);
    }
    private void toast(String message){if(Vars.ui!=null)Vars.ui.hudfrag.showToast(message);}

    private void install(){
        if(installed || Vars.headless)return;
        try{
            Field field=ApplicationCore.class.getDeclaredField("modules");
            field.setAccessible(true);
            for(var listener:Core.app.getListeners()){
                if(!(listener instanceof mindustry.ClientLauncher))continue;
                ApplicationListener[] modules=(ApplicationListener[])field.get(listener);
                for(int i=0;i<modules.length;i++)if(modules[i]==Vars.logic){
                    ApplicationListener original=modules[i];
                    modules[i]=new ApplicationListener(){
                        @Override public void update(){updateSimulation(original);}
                        @Override public void resize(int width,int height){original.resize(width,height);}
                        @Override public void pause(){original.pause();}
                        @Override public void resume(){original.resume();}
                        @Override public void dispose(){original.dispose();}
                        @Override public void exit(){original.exit();}
                        @Override public void fileDropped(Fi file){original.fileDropped(file);}
                    };
                    installed=true;
                    Log.info("[Mindustry Tools] Fixed-step speed controls attached.");
                    return;
                }
            }
            Log.err("[Mindustry Tools] Could not find the desktop Logic module; speed controls disabled.");
        }catch(ReflectiveOperationException | RuntimeException error){
            Log.err("[Mindustry Tools] Could not install fixed-step speed controls.",error);
        }
    }

    private void updateSimulation(ApplicationListener original){
        if(!enabled() || !available() || !Vars.state.isPlaying() || Vars.world.isGenerating() || Vars.world.isInvalidMap()){
            original.update();return;
        }
        float frameDelta=Time.delta;
        if(!Float.isFinite(frameDelta) || frameDelta<=0f){original.update();return;}
        // At low FPS, keep individual transport/combat steps no larger than a normal 60 Hz tick.
        float step=Math.min(frameDelta,1f);
        long epoch=worldEpoch;
        var state=Vars.state;
        int requested=multiplier;
        try{
            Time.delta=step;
            TickRunner.run(requested,frameBudget,
                ()->worldEpoch==epoch && Vars.state==state && enabled() && available() && state.isPlaying() &&
                    !Vars.world.isGenerating() && !Vars.world.isInvalidMap(),
                ()->{Time.delta=step;original.update();},
                ()->{
                    // ClientLauncher owns the first begin() and final end().
                    // Complete physics before preparing each additional tick.
                    Vars.asyncCore.end();
                    Vars.asyncCore.begin();
                },System::nanoTime);
        }finally{
            Time.delta=frameDelta;
        }
    }
}
