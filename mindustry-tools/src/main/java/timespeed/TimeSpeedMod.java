package timespeed;

import arc.Events;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType.ResetEvent;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.EventType.Trigger;
import mindustry.mod.Mod;

/** Scales only the simulation's delta; leaves UI timing and the delta provider alone. */
public class TimeSpeedMod extends Mod{
    private int multiplier=1;
    private boolean scaled;
    private float originalDelta;

    public TimeSpeedMod(){
        Events.on(WorldLoadEvent.class,event -> reset());
        Events.on(ResetEvent.class,event -> reset());
        Events.run(Trigger.update,() -> {
            restoreDelta();
            if(!available())multiplier=1;
        });
        Events.run(Trigger.beforeGameUpdate,this::beforeUpdate);
        Events.run(Trigger.afterGameUpdate,this::restoreDelta);
    }

    public boolean enabled(){return multiplier!=1;}
    public int multiplier(){return multiplier;}

    public void select(int requested){
        if(!SpeedPolicy.preset(requested))return;
        if(multiplier==requested){reset();return;}
        if(!available()){
            if(Vars.ui!=null)Vars.ui.hudfrag.showToast("Speed controls are available only in local single-player games.");
            return;
        }
        restoreDelta();
        multiplier=requested;
    }

    private boolean available(){
        return SpeedPolicy.available(Vars.state!=null && Vars.state.isGame(),
            Vars.net==null || Vars.net.active(),Vars.state!=null && Vars.state.isEditor(),
            Vars.state!=null && Vars.state.gameOver);
    }

    private void beforeUpdate(){
        if(!enabled() || !available())return;
        originalDelta=Time.delta;
        scaled=true;
        Time.delta=SpeedPolicy.simulationDelta(originalDelta,multiplier);
    }

    private void restoreDelta(){
        if(!scaled)return;
        Time.delta=originalDelta;
        scaled=false;
    }

    private void reset(){multiplier=1;restoreDelta();}
}
