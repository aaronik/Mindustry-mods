package autowave;

import arc.Core;
import arc.Events;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.EventType.Trigger;
import mindustry.mod.Mod;
import mindustry.net.Packets.AdminAction;
import mindustry.gen.Call;

/** Opt-in wave button automation; uses the same action and permission rules. */
public class AutoWaveMod extends Mod{
    private static final long cooldownMillis=1000L;
    private boolean enabled;
    private int lastRequestedWave=Integer.MIN_VALUE;
    private long lastRequest;

    public AutoWaveMod(){
        Events.on(WorldLoadEvent.class,event -> reset());
        Events.run(Trigger.update,this::update);
    }
    public boolean enabled(){return enabled;}
    public void toggle(){
        if(!inGame())return;
        enabled=!enabled;
        if(enabled){
            // Allow the first available wave immediately while retaining the
            // one-wave guard for client/server round-trip latency.
            lastRequestedWave=Integer.MIN_VALUE;
            lastRequest=0;
        }
    }
    private void reset(){enabled=false;lastRequestedWave=Integer.MIN_VALUE;lastRequest=0;}
    private boolean inGame(){return Vars.state!=null && Vars.state.isGame() && Vars.player!=null;}
    private void update(){
        if(!enabled)return;
        if(!inGame()){reset();return;}
        if(Vars.state.isPaused())return;
        boolean permitted=(Vars.net.server() || Vars.player.admin) || !Vars.net.active();
        boolean available=AutoWavePolicy.available(Vars.state.rules.waves,Vars.state.rules.waveSending,
            permitted,Vars.state.enemies,Vars.spawner.isSpawning(),Vars.state.isGame(),Vars.state.gameOver);
        if(Core.scene.hasDialog() || Core.scene.hasField() ||
            !AutoWavePolicy.shouldSkip(enabled,available,Vars.state.wave,lastRequestedWave,
                Time.timeSinceMillis(lastRequest),cooldownMillis))return;
        lastRequestedWave=Vars.state.wave;
        lastRequest=Time.millis();
        if(Vars.net.client() && Vars.player.admin){
            Call.adminRequest(Vars.player,AdminAction.wave,null);
        }else{
            Vars.logic.skipWave();
        }
    }
}
