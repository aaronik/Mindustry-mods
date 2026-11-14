package powerhud;

import arc.Core;
import arc.Events;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.game.EventType.ResetEvent;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.EventType.Trigger;
import mindustry.ui.Styles;
import java.util.*;

/** Passive team power readout, anchored to the live resource display. */
public class PowerHud{
    private Table panel,lines;
    private ScrollPane scroll;
    private long nextRefresh;
    private final Vec2 anchor=new Vec2();

    public PowerHud(){
        Events.on(ClientLoadEvent.class,event->install());
        Events.on(WorldLoadEvent.class,event->reset());
        Events.on(ResetEvent.class,event->reset());
    }
    private void reset(){nextRefresh=0L;if(panel!=null)panel.visible=false;}
    private void install(){
        if(panel!=null || Vars.headless)return;
        panel=new Table(Styles.black6);
        panel.margin(6f);
        lines=new Table();lines.top().left();
        scroll=new ScrollPane(lines);
        scroll.setScrollingDisabled(true,false);
        scroll.setOverscroll(false,false);
        panel.add(scroll).width(430f).height(30f);
        panel.visible=false;
        Vars.ui.hudGroup.addChild(panel);
        Events.run(Trigger.update,this::update);
    }
    private void update(){
        panel.visible=false;
        if(Vars.state==null || !Vars.state.isGame() || Vars.state.isEditor() || Vars.player==null ||
            !Vars.ui.hudfrag.shown() || !Core.settings.getBool("coreitems") || Core.scene.hasDialog())return;
        var resources=Vars.ui.hudfrag.coreItems;
        if(resources.parent==null || resources.getWidth()<=0f || resources.getHeight()<=0f)return;
        long now=System.nanoTime();
        if(now>=nextRefresh){refresh();nextRefresh=now+500_000_000L;}
        panel.pack();
        anchor.set(0f,resources.getHeight());
        resources.localToStageCoordinates(anchor);
        float gap=Scl.scl(8f),right=anchor.x+resources.getWidth()+gap;
        float x=right,y=anchor.y-panel.getHeight();
        if(right+panel.getWidth()>Core.scene.getWidth()){
            x=anchor.x-panel.getWidth()-gap;
            if(x<0f){x=anchor.x;y=anchor.y-resources.getHeight()-panel.getHeight()-gap;}
        }
        panel.setPosition(Mathf.clamp(x,0f,Math.max(0f,Core.scene.getWidth()-panel.getWidth())),
            Mathf.clamp(y,0f,Math.max(0f,Core.scene.getHeight()-panel.getHeight())));
        panel.visible=true;
    }
    private void refresh(){
        Map<Integer,PowerSnapshot.Network> networks=new HashMap<>();
        for(var building:Vars.player.team().data().buildings){
            if(!building.isValid() || building.team!=Vars.player.team() || building.power==null || building.power.graph==null)continue;
            var graph=building.power.graph;
            if(networks.containsKey(graph.getID()) ||
                !PowerSnapshot.meaningful(graph.producers.size,graph.consumers.size,graph.batteries.size))continue;
            networks.put(graph.getID(),new PowerSnapshot.Network(graph.getID(),graph.all.size,
                graph.getLastScaledPowerIn(),graph.getLastScaledPowerOut(),graph.getLastPowerStored(),graph.getLastCapacity()));
        }
        var snapshot=PowerSnapshot.unique(networks.values());
        lines.clearChildren();
        for(var network:snapshot){
            String sign=network.balance()>=0d?"[green]+":"[scarlet]";
            lines.add("Net "+sign+amount(network.balance())+"[]/s   Battery "+
                amount(network.charge())+" / "+amount(network.batteryCapacity())).left().row();
        }
        if(snapshot.isEmpty())lines.add("No power networks.").left().row();
        panel.getCell(scroll).width(Math.min(430f,Math.max(150f,Core.scene.getWidth()/Scl.scl(1f)-24f)))
            .height(Math.min(230f,Math.max(30f,lines.getPrefHeight())));
    }
    private static String amount(double value){
        double magnitude=Math.abs(value);
        return magnitude>=1_000_000d?Strings.autoFixed((float)(value/1_000_000d),1)+"m":
            magnitude>=1000d?Strings.autoFixed((float)(value/1000d),1)+"k":Strings.autoFixed((float)value,1);
    }
}
