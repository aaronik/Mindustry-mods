package mindustrytools;

import arc.Events;
import arc.scene.event.Touchable;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import autodrill.AutoDrillMod;
import batterypack.BatteryPackMod;
import megapriority.MegaPriorityMod;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.mod.Mod;
import mindustry.ui.Styles;
import solarpack.SolarPackMod;
import wallpack.WallPackMod;

/** One HUD column for all five tools; Pack is the sole submenu. */
public class MindustryToolsMod extends Mod{
    private final AutoDrillMod drill = new AutoDrillMod();
    private final SolarPackMod solar = new SolarPackMod();
    private final BatteryPackMod battery = new BatteryPackMod();
    private final WallPackMod wall = new WallPackMod();
    private final MegaPriorityMod mega = new MegaPriorityMod();
    private boolean packOpen, hudInstalled;

    public MindustryToolsMod(){
        Events.on(ClientLoadEvent.class, event -> {
            if(hudInstalled) return;
            hudInstalled = true;
            Vars.ui.hudGroup.fill(hud -> {
                hud.touchable = Touchable.childrenOnly;
                hud.bottom().right();
                hud.visible(() -> Vars.state != null && Vars.state.isGame() &&
                    Vars.ui.hudfrag.shown() && Vars.control != null && Vars.control.input != null);
                hud.table(options -> {
                    options.right();
                    option(options, "Mega priority", mega::selectedAllManagedNow, mega::toggleSelection);
                    options.row();
                    options.collapser(packs -> {
                        packs.right();
                        option(packs, "Wall-pack", wall::armed, () -> select(Tool.wall));
                        packs.row();
                        option(packs, "Battery-pack", battery::armed, () -> select(Tool.battery));
                        packs.row();
                        option(packs, "Solar-pack", solar::armed, () -> select(Tool.solar));
                    }, true, () -> !packOpen).row();
                    option(options, "Pack", () -> packOpen, () -> packOpen = !packOpen);
                    options.row();
                    option(options, "Auto-drill", drill::armed, () -> select(Tool.drill));
                }).padRight(316f).padBottom(6f);
            });
        });
    }

    private void option(Table table, String label, arc.func.Boolp checked, Runnable action){
        TextButton button = table.button(label, Styles.togglet, null).size(175f,45f).get();
        button.setProgrammaticChangeEvents(false);
        button.clicked(action);
        button.update(() -> button.setChecked(checked.get()));
    }
    private enum Tool{drill,solar,battery,wall}
    private void select(Tool tool){
        if(tool != Tool.drill && drill.armed()) drill.toggle();
        if(tool != Tool.solar && solar.armed()) solar.toggle();
        if(tool != Tool.battery && battery.armed()) battery.toggle();
        if(tool != Tool.wall && wall.armed()) wall.toggle();
        switch(tool){
            case drill -> drill.toggle();
            case solar -> solar.toggle();
            case battery -> battery.toggle();
            case wall -> wall.toggle();
        }
        // Pack stays open until its button is clicked again.
    }
}
