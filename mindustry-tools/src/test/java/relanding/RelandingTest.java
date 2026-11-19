package relanding;

import arc.Core;
import arc.math.geom.Point2;
import arc.struct.IntSet;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.core.ContentLoader;
import mindustry.core.GameState;
import mindustry.core.World;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.world.Tile;
import mindustry.world.Tiles;

public class RelandingTest{
    static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    public static void main(String[]args){
        check(repeatEligible(),"repeat restore eligible");
        check(!RelandingPolicy.suppress(true,true,0,true,1),"normal load/first landing excluded");
        check(!RelandingPolicy.suppress(true,true,1,false,1),"no prior save excluded");
        check(!RelandingPolicy.suppress(true,true,1,true,0),"first attempt excluded");
        check(!RelandingPolicy.suppress(false,true,1,true,1),"Erekir excluded");
        check(!RelandingPolicy.suppress(true,false,1,true,1),"multiplayer excluded");
        Vars.headless=true;Core.settings=new arc.Settings();
        Vars.content=new ContentLoader();Vars.content.createBaseContent();Vars.content.init();
        Groups.init();Vars.state=new GameState();Vars.world=new World();
        Vars.world.tiles=new Tiles(24,24);
        for(int x=0;x<24;x++)for(int y=0;y<24;y++)Vars.world.tiles.set(x,y,new Tile(x,y,Blocks.air,Blocks.air,Blocks.stone));
        Vars.world.tile(4,4).setBlock(Blocks.coreShard,Team.sharded);
        Vars.world.tile(8,8).setBlock(Blocks.conveyor,Team.sharded);
        Vars.world.tile(10,8).setBlock(Blocks.titaniumConveyor,Team.sharded);
        Vars.world.tile(14,8).setBlock(Blocks.duo,Team.crux);
        Vars.world.tile(17,8).setBlock(Blocks.conveyor,Team.derelict);
        IntSet protect=new IntSet();protect.add(Point2.pack(10,8));
        int count=RelandingMod.removeStarters(Vars.world.tiles,protect);
        check(count==1,"only generated friendly starter removed");
        check(Vars.world.tile(8,8).build==null,"conflicting starter tile free for previous design");
        check(Vars.world.tile(4,4).block()==Blocks.coreShard,"core preserved");
        check(Vars.world.tile(10,8).block()==Blocks.titaniumConveyor,"launch loadout preserved");
        check(Vars.world.tile(14,8).team()==Team.crux,"enemy preserved");
        check(Vars.world.tile(17,8).team()==Team.derelict,"derelict handling left to vanilla");
        Vars.world.tile(8,8).setBlock(Blocks.router,Team.derelict);
        check(Vars.world.tile(8,8).block()==Blocks.router,"old design can occupy cleared tile");
        System.out.println("Repeat landing starter preservation tests passed");
    }
    static boolean repeatEligible(){return RelandingPolicy.suppress(true,true,1,true,1);}
}
