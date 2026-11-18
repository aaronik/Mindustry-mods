package timespeed;

import arc.*;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.ClientLauncher;
import mindustry.core.*;
import mindustry.async.AsyncCore;
import mindustry.game.EventType.*;
import mindustry.net.Net;
import java.lang.reflect.Proxy;

public class TimeSpeedTest{
    static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    static class Launcher extends ClientLauncher{
        void tick(){for(var module:modules)module.update();}
    }
    public static void main(String[]args){
        check(SpeedPolicy.available(true,false,false,false),"local game allowed");
        check(!SpeedPolicy.available(false,false,false,false),"menu blocked");
        check(!SpeedPolicy.available(true,true,false,false),"network blocked");
        check(!SpeedPolicy.available(true,false,true,false),"editor blocked");
        check(!SpeedPolicy.available(true,false,false,true),"game over blocked");
        int[] ticks={0},handoffs={0};
        check(TickRunner.run(100,Long.MAX_VALUE,()->true,()->ticks[0]++,()->handoffs[0]++,()->0L)==100,"100 real ticks");
        check(ticks[0]==100 && handoffs[0]==99,"handoff between every additional tick");
        ticks[0]=0;
        TickRunner.run(100,10,()->true,()->ticks[0]++,()->{},()->ticks[0]*10L);
        check(ticks[0]==1,"CPU budget stops additional work");
        ticks[0]=0;
        TickRunner.run(100,Long.MAX_VALUE,()->ticks[0]<3,()->ticks[0]++,()->{},()->0L);
        check(ticks[0]==3,"stop immediately on pause/world change predicate");

        Vars.headless=false;
        Core.settings=new Settings(){@Override public void manualSave(){}};
        mindustry.gen.Groups.init();
        Vars.state=new GameState();Vars.net=new Net(null);Vars.world=new World();
        Vars.state.set(GameState.State.playing);
        int[] logicTicks={0},renderTicks={0},begin={0},end={0};
        boolean[] pauseOnTick={false},throwOnTick={false};
        Vars.logic=new Logic(){
            @Override public void update(){
                logicTicks[0]++;
                check(Time.delta<=1f,"never oversized delta");
                Events.fire(Trigger.update);
                if(pauseOnTick[0])Vars.state.set(GameState.State.paused);
                if(throwOnTick[0])throw new IllegalStateException("test failure");
            }
        };
        Vars.asyncCore=new AsyncCore(){
            @Override public void begin(){begin[0]++;}
            @Override public void end(){end[0]++;}
        };
        Launcher launcher=new Launcher();
        launcher.add(Vars.logic);
        launcher.add(new ApplicationListener(){@Override public void update(){renderTicks[0]++;}});
        Core.app=(Application)Proxy.newProxyInstance(Application.class.getClassLoader(),new Class[]{Application.class},
            (proxy,method,arguments)->method.getName().equals("getListeners")?Seq.with(launcher):null);
        TimeSpeedMod speed=new TimeSpeedMod();
        Events.fire(new ClientLoadEvent());
        for(int preset:new int[]{2,5,100}){
            logicTicks[0]=begin[0]=end[0]=renderTicks[0]=0;
            speed.select(preset);Time.delta=0.75f;
            launcher.tick();
            check(logicTicks[0]>=1 && logicTicks[0]<=preset,"bounded repeated ticks");
            check(begin[0]==logicTicks[0]-1 && end[0]==begin[0],"async handoffs balanced");
            check(renderTicks[0]==1 && Time.delta==0.75f,"UI/render once, original delta restored");
            speed.select(preset);check(!speed.enabled(),"same preset returns to normal");
        }
        logicTicks[0]=0;Time.delta=0.75f;launcher.tick();
        check(logicTicks[0]==1,"normal speed runs exactly one tick");
        speed.select(100);Time.delta=3f;launcher.tick();
        check(Time.delta==3f,"large frame delta capped only during simulation and restored");
        speed.select(100);Time.delta=0.75f;
        speed.select(100);pauseOnTick[0]=true;logicTicks[0]=0;launcher.tick();
        check(logicTicks[0]==1,"pause stops batch");
        pauseOnTick[0]=false;Vars.state.set(GameState.State.playing);
        throwOnTick[0]=true;Time.delta=0.5f;
        try{launcher.tick();throw new AssertionError("expected test failure");}catch(IllegalStateException expected){}
        check(Time.delta==0.5f,"delta restored on failure");throwOnTick[0]=false;
        Events.fire(new ResetEvent());check(!speed.enabled(),"reset disables speed");
        speed.select(5);Events.fire(new WorldLoadEvent());check(!speed.enabled(),"world load disables speed");
        speed.select(2);Vars.net.setClientConnected();Events.fire(Trigger.update);check(!speed.enabled(),"network entry disables speed");
        System.out.println("Fixed-step speed scheduling, listener integration and lifecycle tests passed");
    }
}
