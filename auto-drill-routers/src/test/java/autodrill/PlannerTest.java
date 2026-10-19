package autodrill;
import java.util.*;
public class PlannerTest{
    static class Grid implements Planner.Grid{
        Set<Planner.Point> ore = new HashSet<>(), blocked = new HashSet<>();
        int width=15, height=15;
        public boolean inBounds(int x,int y){return x>=0 && y>=0 && x<width && y<height;}
        public boolean ore(int x,int y){return ore.contains(new Planner.Point(x,y));}
        public boolean mineable(int x,int y){return ore(x,y);}
        public boolean free(int x,int y){return !blocked.contains(new Planner.Point(x,y));}
        public boolean validDrill(int x,int y){return inBounds(x,y);}
        public boolean validRouter(int x,int y){return inBounds(x,y);}
    }
    static void check(boolean ok,String message){if(!ok) throw new AssertionError(message);}
    static boolean touch(Planner.Point a,Planner.Point b){return Math.abs(a.x()-b.x())+Math.abs(a.y()-b.y())==1;}
    static Set<Planner.Point> flood(Set<Planner.Point> tiles){
        Set<Planner.Point> reached=new HashSet<>();
        if(tiles.isEmpty()) return reached;
        ArrayDeque<Planner.Point> q=new ArrayDeque<>();
        q.add(tiles.iterator().next());reached.add(q.peek());
        while(!q.isEmpty()){
            Planner.Point p=q.removeFirst();
            for(Planner.Point n:tiles) if(touch(p,n) && reached.add(n)) q.add(n);
        }
        return reached;
    }
    static void verify(Grid g,Planner.Layout result,int size,int offset){
        Set<Planner.Point> occupied=new HashSet<>();
        for(Planner.Point d:result.drills()) for(Planner.Point t:Planner.footprint(d,size,offset)){
            check(occupied.add(t),"drills overlap");check(g.free(t.x(),t.y()),"drill overwrites building");
        }
        Set<Planner.Point> routers=new HashSet<>(result.routers());
        check(routers.size()==result.routers().size(),"duplicate routers");
        check(flood(routers).size()==routers.size(),"router network not connected");
        for(Planner.Point r:routers) check(!occupied.contains(r) && g.free(r.x(),r.y()),"router overlaps building/drill");
        for(Planner.Point d:result.drills()){
            Set<Planner.Point> footprint=new HashSet<>(Planner.footprint(d,size,offset));
            check(footprint.stream().anyMatch(t->routers.stream().anyMatch(r->touch(t,r))),"drill has no router access");
        }
        // Flood all placeable tiles that could connect to the router network.
        // Every reachable free drill-edge tile belongs to the border.
        Set<Planner.Point> reachable=new HashSet<>(routers);
        ArrayDeque<Planner.Point> walk=new ArrayDeque<>(routers);
        while(!walk.isEmpty()){
            Planner.Point t=walk.removeFirst();
            for(Planner.Point n:List.of(new Planner.Point(t.x()+1,t.y()),new Planner.Point(t.x()-1,t.y()),
                new Planner.Point(t.x(),t.y()+1),new Planner.Point(t.x(),t.y()-1))){
                if(!occupied.contains(n) && g.inBounds(n.x(),n.y()) && g.free(n.x(),n.y()) &&
                    g.validRouter(n.x(),n.y()) && reachable.add(n)) walk.add(n);
            }
        }
        for(Planner.Point t:occupied){
            for(Planner.Point n:List.of(new Planner.Point(t.x()+1,t.y()),new Planner.Point(t.x()-1,t.y()),
                new Planner.Point(t.x(),t.y()+1),new Planner.Point(t.x(),t.y()-1))){
                if(reachable.contains(n)) check(routers.contains(n),"reachable drill border omitted: "+n);
            }
        }
    }
    public static void main(String[] args){
        Grid g=new Grid();
        for(int x=2;x<8;x++)for(int y=2;y<6;y++)g.ore.add(new Planner.Point(x,y));
        g.blocked.add(new Planner.Point(2,2));
        Planner.Layout a=Planner.plan(g,5,4,2,0,4096);
        check(!a.drills().isEmpty() && !a.routers().isEmpty(),"should queue drills and routers");
        check(a.oreTiles()==24 && a.coveredTiles()<24,"occupied ore must remain uncovered");
        verify(g,a,2,0);
        check(Planner.plan(g,0,0,2,0,4096).drills().isEmpty(),"non-ore click");
        Grid tiny=new Grid();tiny.ore.add(new Planner.Point(3,3));
        Planner.Layout one=Planner.plan(tiny,3,3,1,0,4096);
        check(one.routers().size()==8,"single drill requires four side and four corner routers: "+one.routers());verify(tiny,one,1,0);
        // Corner transitions must be bridged by side-connected tiles, never
        // leave diagonal-only router contact as the sole connection.
        Grid stairs=new Grid();
        for(int x=2;x<=3;x++)for(int y=6;y<=7;y++)stairs.ore.add(new Planner.Point(x,y));
        for(int x=4;x<=5;x++)for(int y=4;y<=5;y++)stairs.ore.add(new Planner.Point(x,y));
        stairs.ore.add(new Planner.Point(4,6)); // connects the two ore clusters
        Planner.Layout stepped=Planner.plan(stairs,4,6,2,0,1024);
        verify(stairs,stepped,2,0);
        for(Planner.Point r:stepped.routers()){
            boolean hasDiagonal=stepped.routers().stream().anyMatch(p->Math.abs(p.x()-r.x())==1 && Math.abs(p.y()-r.y())==1);
            if(hasDiagonal) check(stepped.routers().stream().anyMatch(p->touch(p,r)),"diagonal-only router: "+r);
        }
        check(Planner.plan(g,5,4,2,0,2).truncated(),"large patch cap");
        Grid corridor=new Grid();corridor.width=12;corridor.height=7;
        for(int x=2;x<9;x++) corridor.ore.add(new Planner.Point(x,3));
        for(int x=0;x<12;x++)for(int y=0;y<7;y++){
            if(y!=3 && (y!=4 || x<2 || x>8)) corridor.blocked.add(new Planner.Point(x,y));
        }
        Planner.Layout path=Planner.plan(corridor,2,3,1,0,4096);
        check(path.drills().size()==7,"all corridor drills should fit: "+path);verify(corridor,path,1,0);
        Grid cutoff=new Grid();cutoff.width=8;cutoff.height=3;
        for(int x=1;x<=6;x++)cutoff.ore.add(new Planner.Point(x,1));
        // Router path outside ore is blocked at a cut; not all drills can attach.
        for(int x=0;x<8;x++)for(int y=0;y<3;y++)if(y!=1 && x!=2 && x!=5)cutoff.blocked.add(new Planner.Point(x,y));
        Planner.Layout limited=Planner.plan(cutoff,1,1,1,0,4096);
        check(limited.drills().size()<6,"inaccessible drills must be skipped");verify(cutoff,limited,1,0);
        // Two drill groups separated by a solid barrier must not be presented
        // as one output network if there is no router route between them.
        Grid wall=new Grid();wall.width=11;wall.height=7;
        for(int x=1;x<=9;x++) wall.ore.add(new Planner.Point(x,3));
        for(int y=0;y<7;y++){
            wall.blocked.add(new Planner.Point(5,y));
        }
        Planner.Layout split=Planner.plan(wall,2,3,1,0,4096);
        verify(wall,split,1,0);
        check(split.drills().size()<8,"wall-separated drills must be skipped");
        Grid dense=new Grid();dense.width=23;dense.height=23;
        for(int x=2;x<20;x++)for(int y=2;y<20;y++)dense.ore.add(new Planner.Point(x,y));
        Planner.Layout packed=Planner.plan(dense,10,10,3,-1,1024);
        verify(dense,packed,3,-1);
        check(packed.coveredTiles()>=170,"dense deposit needs interior packing: "+packed.coveredTiles());
        check(packed.drills().stream().anyMatch(d->d.x()>=7 && d.x()<=14 && d.y()>=7 && d.y()<=14),"interior drill missing");
        System.out.println("Dense-field coverage: "+packed.coveredTiles()+"/"+packed.oreTiles()+
            ", drills="+packed.drills().size()+", routers="+packed.routers().size());
        Random random=new Random(8213);
        for(int trial=0;trial<60;trial++){
            Grid randomGrid=new Grid();randomGrid.width=10;randomGrid.height=10;
            for(int x=2;x<8;x++)for(int y=2;y<8;y++)randomGrid.ore.add(new Planner.Point(x,y));
            for(int x=0;x<10;x++)for(int y=0;y<10;y++)if(random.nextDouble()<0.22)
                randomGrid.blocked.add(new Planner.Point(x,y));
            Planner.Layout result=Planner.plan(randomGrid,4,4,2,0,1024);
            verify(randomGrid,result,2,0);
        }
        // Boulders/snow props are alwaysReplace; actual structures and
        // existing construction sites are never overwritten.
        check(AutoDrillMod.mayPlaceOver(true,false,false),"air is buildable");
        check(AutoDrillMod.mayPlaceOver(false,true,false),"replaceable terrain is buildable");
        check(!AutoDrillMod.mayPlaceOver(false,false,false),"unreplaceable terrain blocks planning");
        check(!AutoDrillMod.mayPlaceOver(false,true,true),"even replaceable block with a building stays blocked");
        check(!AutoDrillMod.mayPlaceOver(true,false,true),"air tile with building stays blocked");
        System.out.println("Connected-network planner tests passed");
    }
}
