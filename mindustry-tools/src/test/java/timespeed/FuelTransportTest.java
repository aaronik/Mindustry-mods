package timespeed;

import arc.Core;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.core.ContentLoader;
import mindustry.core.GameState;
import mindustry.core.World;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.Tiles;
import mindustry.world.blocks.power.ConsumeGenerator.ConsumeGeneratorBuild;

/** Headless Mindustry 160.7 building updates, not a model of item transport. */
public class FuelTransportTest{
    static void check(boolean condition, String message){
        if(!condition) throw new AssertionError(message);
    }

    static class Chain{
        final Building vault, unloader, first, second, router;
        final ConsumeGeneratorBuild east, south;
        final Building[] updates;
        int producingTicks;

        Chain(int fuel){
            // Recreate the world and entity groups for each independent run. Content is loaded once.
            Groups.init();
            Vars.world = new World();
            Vars.state = new GameState();
            Vars.world.tiles = new Tiles(24, 16);
            for(int x = 0; x < 24; x++) for(int y = 0; y < 16; y++){
                Vars.world.tiles.set(x, y, new Tile(x, y, Blocks.air, Blocks.air, Blocks.stone));
            }
            vault = place(3, 7, Blocks.vault);
            unloader = place(5, 7, Blocks.unloader);
            first = place(6, 7, Blocks.titaniumConveyor);
            second = place(7, 7, Blocks.titaniumConveyor);
            router = place(8, 7, Blocks.router);
            east = (ConsumeGeneratorBuild)place(9, 7, Blocks.rtgGenerator);
            south = (ConsumeGeneratorBuild)place(8, 5, Blocks.rtgGenerator);
            updates = new Building[]{vault, unloader, first, second, router, east, south};
            vault.items.add(Items.thorium, fuel);
            for(Building building : updates) building.updateProximity();
            check(unloader.proximity.contains(vault, true) && router.proximity.contains(east, true)
                && router.proximity.contains(south, true), "real tile adjacency must be wired");
        }

        Building place(int x, int y, Block block){
            Tile tile = Vars.world.tile(x, y);
            tile.setBlock(block, Team.sharded, 0);
            check(tile.build != null, "expected actual " + block.name + " building");
            return tile.build;
        }

        void tick(float delta){
            Time.delta = delta;
            // Same Building.update() path used by the game: consumption followed by updateTile().
            for(Building building : updates) building.update();
            if(east.productionEfficiency > 0 && south.productionEfficiency > 0) producingTicks++;
        }

        int inTransit(){
            return first.items.get(Items.thorium) + second.items.get(Items.thorium)
                + router.items.get(Items.thorium) + unloader.items.get(Items.thorium);
        }

        void sameAs(Chain other){
            for(int i = 0; i < updates.length; i++){
                check(updates[i].items.get(Items.thorium) == other.updates[i].items.get(Items.thorium),
                    "thorium inventory differs at " + updates[i].block.name);
            }
            check(Float.compare(east.generateTime, other.east.generateTime) == 0
                && Float.compare(south.generateTime, other.south.generateTime) == 0
                && Float.compare(east.totalTime, other.east.totalTime) == 0
                && Float.compare(south.totalTime, other.south.totalTime) == 0
                && Float.compare(east.productionEfficiency, other.east.productionEfficiency) == 0
                && Float.compare(south.productionEfficiency, other.south.productionEfficiency) == 0
                && producingTicks == other.producingTicks, "RTG generation history differs");
        }
    }

    public static void main(String[] args){
        Vars.headless = true;
        Core.settings = new arc.Settings();
        Vars.content = new ContentLoader();
        Vars.content.createBaseContent();
        Vars.content.init();

        Chain normal = new Chain(2);
        for(int i = 0; i < 1000; i++) normal.tick(1f);
        check(normal.vault.items.get(Items.thorium) == 0 && normal.inTransit() == 0,
            "ordinary ticks must transport all finite fuel");
        check(normal.east.items.get(Items.thorium) == 0 && normal.south.items.get(Items.thorium) == 0,
            "both RTGs must consume their only item and starve");
        check(normal.producingTicks > 700 && normal.producingTicks < 900,
            "both RTGs must produce power before starvation, not indefinitely");
        check(normal.east.productionEfficiency == 0f && normal.south.productionEfficiency == 0f,
            "starved RTGs must stop generating");

        Chain repeated = new Chain(2);
        int completed = TickRunner.run(1000, Long.MAX_VALUE, () -> true,
            () -> repeated.tick(1f), () -> {}, () -> 0L);
        check(completed == 1000, "substep runner must execute every requested tick");
        repeated.sameAs(normal);

        Chain stockedNormal = new Chain(100);
        for(int i=0;i<10_000;i++)stockedNormal.tick(1f);
        Chain stockedFast = new Chain(100);
        for(int frame=0;frame<100;frame++)TickRunner.run(100,Long.MAX_VALUE,()->true,
            ()->stockedFast.tick(1f),()->{},()->0L);
        stockedFast.sameAs(stockedNormal);
        check(stockedFast.east.productionEfficiency>0f && stockedFast.south.productionEfficiency>0f,
            "stocked 100-tick batches must keep both RTGs fueled just like normal speed");

        Chain oversized = new Chain(2);
        oversized.tick(1000f);
        check(oversized.east.items.get(Items.thorium) == 0 && oversized.south.items.get(Items.thorium) == 0
            && oversized.producingTicks == 0 && oversized.inTransit() > 0,
            "one oversized delta must NOT be mistaken for 1000 transport/generation ticks");
        System.out.println("Real vault/unloader/titanium conveyors/router/two RTGs: repeated ticks match normal; oversized delta does not feed RTGs");
    }
}
