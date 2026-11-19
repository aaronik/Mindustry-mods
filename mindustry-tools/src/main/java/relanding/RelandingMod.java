package relanding;

import arc.Events;
import arc.math.geom.Point2;
import arc.struct.IntSet;
import arc.util.Log;
import mindustry.Vars;
import mindustry.content.Planets;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.maps.generators.FileMapGenerator;
import mindustry.type.SectorPreset;
import mindustry.world.Tiles;
import mindustry.world.WorldParams;
import mindustry.world.blocks.storage.CoreBlock;
import java.util.ArrayList;

/** Runs only during fresh map generation in vanilla's lost-sector restore path. */
public class RelandingMod{
    public RelandingMod(){
        Events.on(ClientLoadEvent.class,event->{
            for(SectorPreset preset:Vars.content.sectors()){
                // Do not wrap custom/mod generators or Erekir's scripted starting bases.
                if(preset.planet!=Planets.serpulo || preset.generator==null ||
                    preset.generator.getClass()!=FileMapGenerator.class)continue;
                FileMapGenerator original=preset.generator;
                preset.generator=new FileMapGenerator(original.map,preset){
                    @Override public void generate(Tiles tiles,WorldParams params){
                        var sector=Vars.state.rules.sector;
                        boolean suppress=sector!=null && RelandingPolicy.suppress(
                            sector.planet==Planets.serpulo,Vars.net!=null && !Vars.net.active(),
                            params.corePositionOverride,sector.save!=null && sector.hasSave(),sector.info.attempts);
                        original.generate(tiles,params);
                        if(!suppress)return;
                        IntSet protectedTiles=new IntSet();
                        if(sector.allowLaunchLoadout()){
                            var loadout=Vars.universe.getLastLoadout();
                            var core=loadout==null?null:loadout.tiles.find(tile->tile.block instanceof CoreBlock);
                            // If launch contents cannot be identified, leave the map untouched.
                            if(core==null){Log.warn("[Mindustry Tools] Skipped repeat-landing cleanup: unknown launch loadout.");return;}
                            int ox=Point2.x(params.corePositionOverride)-core.x;
                            int oy=Point2.y(params.corePositionOverride)-core.y;
                            for(var tile:loadout.tiles){
                                for(int dx=0;dx<tile.block.size;dx++)for(int dy=0;dy<tile.block.size;dy++)
                                    protectedTiles.add(Point2.pack(tile.x+ox+tile.block.sizeOffset+dx,
                                        tile.y+oy+tile.block.sizeOffset+dy));
                            }
                        }
                        int removed=removeStarters(Vars.world.tiles,protectedTiles);
                        Log.info("[Mindustry Tools] Repeat landing: removed @ map starter buildings before saved-base restoration.",removed);
                    }
                };
            }
        });
    }

    /** Called before any saved player buildings/plans have been restored. */
    public static int removeStarters(Tiles tiles,IntSet protectedTiles){
        var remove=new ArrayList<mindustry.gen.Building>();
        for(var tile:tiles){
            var building=tile.build;
            if(building==null || building.tile!=tile)continue;
            boolean overlaps=false;
            for(int dx=0;dx<building.block.size;dx++)for(int dy=0;dy<building.block.size;dy++)
                if(protectedTiles.contains(Point2.pack(tile.x+building.block.sizeOffset+dx,
                    tile.y+building.block.sizeOffset+dy)))overlaps=true;
            if(shouldRemove(building,overlaps))remove.add(building);
        }
        for(var building:remove)building.tile.remove();
        return remove.size();
    }
    private static boolean shouldRemove(mindustry.gen.Building building,boolean overlaps){
        return RelandingPolicy.remove(building.team==Vars.state.rules.defaultTeam,
            building.block instanceof CoreBlock,overlaps);
    }
}
