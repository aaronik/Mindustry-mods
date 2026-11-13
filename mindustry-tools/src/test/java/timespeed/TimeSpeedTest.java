package timespeed;

import arc.Events;
import arc.util.Time;
import mindustry.Vars;
import mindustry.core.GameState;
import mindustry.game.EventType.ResetEvent;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.EventType.Trigger;
import mindustry.net.Net;

public class TimeSpeedTest{
    static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    public static void main(String[]args){
        check(SpeedPolicy.available(true,false,false,false),"local game is allowed");
        check(!SpeedPolicy.available(false,false,false,false),"menu is blocked");
        check(!SpeedPolicy.available(true,true,false,false),"multiplayer including host is blocked");
        check(!SpeedPolicy.available(true,false,true,false),"editor is blocked");
        check(!SpeedPolicy.available(true,false,false,true),"game over is blocked");
        check(SpeedPolicy.simulationDelta(0.5f)==50f,"scales variable timestep");
        check(SpeedPolicy.simulationDelta(0f)==0f,"zero delta remains zero");
        check(SpeedPolicy.simulationDelta(Float.MAX_VALUE)==Float.MAX_VALUE,"no overflow");

        Vars.state=new GameState();
        Vars.net=new Net(null);
        Vars.state.set(GameState.State.playing);
        TimeSpeedMod speed=new TimeSpeedMod();
        Time.delta=0.75f;
        Events.fire(Trigger.beforeGameUpdate);
        check(Time.delta==0.75f,"disabled speed leaves delta alone");
        speed.toggle();
        check(speed.enabled(),"toggle enables speed");
        Events.fire(Trigger.beforeGameUpdate);
        check(Time.delta==75f,"simulation runs at 100x delta");
        Events.fire(Trigger.afterGameUpdate);
        check(Time.delta==0.75f,"normal UI delta restored");
        speed.toggle();
        check(!speed.enabled(),"toggle disables speed");
        speed.toggle();
        Events.fire(Trigger.beforeGameUpdate);
        Events.fire(new ResetEvent());
        check(!speed.enabled() && Time.delta==0.75f,"reset disables and restores");
        speed.toggle();
        Events.fire(new WorldLoadEvent());
        check(!speed.enabled(),"world load disables speed");
        speed.toggle();
        Vars.state.set(GameState.State.paused);
        Events.fire(Trigger.update);
        check(speed.enabled() && Time.delta==0.75f,"pause preserves toggle without scaling UI");
        Vars.state.set(GameState.State.menu);
        Events.fire(Trigger.update);
        check(!speed.enabled(),"leaving game disables speed");
        Vars.state.set(GameState.State.playing);
        Time.setDeltaProvider(() -> 0.25f);
        speed.toggle();
        Events.fire(Trigger.beforeGameUpdate);
        Events.fire(Trigger.afterGameUpdate);
        check(Time.delta==0.25f,"existing delta restored after another update");
        Vars.net.setClientConnected();
        Events.fire(Trigger.update);
        check(!speed.enabled(),"joining network disables speed");
        speed.toggle();
        check(!speed.enabled(),"cannot enable on network");
        System.out.println("100x speed policy and lifecycle tests passed");
    }
}
