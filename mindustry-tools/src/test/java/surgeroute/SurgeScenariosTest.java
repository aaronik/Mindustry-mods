package surgeroute;

import java.util.*;

/** Deterministic map-like fixtures: 2x2 footprints, cliffs, props, builds, insulated lines. */
public class SurgeScenariosTest{
    private static final int OPEN=0, CLIFF=1, BUILDING=2, PROP=3, INSULATED=4;
    static class MapGrid implements SurgePlanner.Grid{
        final int width,height;
        final byte[] cells;
        MapGrid(int width,int height){this.width=width;this.height=height;cells=new byte[width*height];}
        public int width(){return width;}
        public int height(){return height;}
        boolean in(int x,int y){return x>=0&&y>=0&&x<width&&y<height;}
        int cell(int x,int y){return in(x,y)?cells[y*width+x]:CLIFF;}
        void set(int x,int y,int value){if(in(x,y))cells[y*width+x]=(byte)value;}
        void rect(int x0,int y0,int x1,int y1,int value){for(int y=y0;y<=y1;y++)for(int x=x0;x<=x1;x++)set(x,y,value);}
        public boolean validTower(int x,int y){
            return in(x,y)&&in(x+1,y+1)&&passable(cell(x,y))&&passable(cell(x+1,y))&&
                passable(cell(x,y+1))&&passable(cell(x+1,y+1));
        }
        boolean passable(int v){return v==OPEN||v==PROP;}
        public int wallEdges(int x,int y){
            int count=0;
            for(int k=0;k<2;k++){
                if(cell(x-1,y+k)==CLIFF)count++;
                if(cell(x+2,y+k)==CLIFF)count++;
                if(cell(x+k,y-1)==CLIFF)count++;
                if(cell(x+k,y+2)==CLIFF)count++;
            }
            return count;
        }
        public boolean linkClear(int x,int y,int tx,int ty){
            // Discrete world-raycast approximation: walls don't insulate;
            // only insulated buildings on the segment block a laser.
            int n=Math.max(Math.abs(tx-x),Math.abs(ty-y))*2+1;
            for(int i=0;i<=n;i++){
                int px=Math.round(x+(tx-x)*i/(float)n),py=Math.round(y+(ty-y)*i/(float)n);
                if(cell(px,py)==INSULATED)return false;
            }
            return true;
        }
    }
    static SurgePlanner.Route route(MapGrid grid,SurgePlanner.Point click,List<SurgePlanner.Source> surge,List<SurgePlanner.Source> fallback,int batch){
        var search=new SurgePlanner.Search(grid,click,surge,fallback);
        int iterations=0;
        while(!search.advance(batch))check(++iterations<50000,"search timed out");
        return search.result();
    }
    static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);}
    static void validate(MapGrid grid,SurgePlanner.Route route,SurgePlanner.Point click){
        check(route!=null&&!route.towers().isEmpty(),"route expected near "+click);
        List<SurgePlanner.Point> towers=route.towers();
        var source=route.source();
        var first=towers.get(0);
        check(route.usedExistingTower()?SurgePlanner.touches(first,2,source.center(),source.size()):
            SurgePlanner.inRange(first,source.center()),"first tower not connected to power source");
        check(grid.linkClear(first.x(),first.y(),source.center().x(),source.center().y()),"source raycast blocked");
        Set<SurgePlanner.Point> occupied=new HashSet<>();
        for(int i=0;i<towers.size();i++){
            var p=towers.get(i);
            check(grid.validTower(p.x(),p.y()),"illegal 2x2 tower at "+p);
            for(int dy=0;dy<2;dy++)for(int dx=0;dx<2;dx++){
                check(occupied.add(new SurgePlanner.Point(p.x()+dx,p.y()+dy)),"overlapping towers at "+p);
            }
            if(i>0){var prev=towers.get(i-1);
                check(SurgePlanner.inRange(prev,p),"hop exceeds range");
                check(grid.linkClear(prev.x(),prev.y(),p.x(),p.y()),"link crosses insulated block");
            }
        }
        var end=towers.get(towers.size()-1);
        check(Math.abs(end.x()-click.x())<=2&&Math.abs(end.y()-click.y())<=2,
            "route ended far from click: "+end+" vs "+click);
    }
    public static void main(String[] args){
        // Realistic open/cliff map edge: running along a black wall is favored
        // despite a direct route through buildable terrain.
        MapGrid edge=new MapGrid(300,180);
        edge.rect(0,0,299,35,CLIFF);
        var power=new SurgePlanner.Source(new SurgePlanner.Point(15,85),1);
        var along=route(edge,new SurgePlanner.Point(260,85),List.of(),List.of(power),43);
        validate(edge,along,new SurgePlanner.Point(260,85));
        check(along.towers().stream().anyMatch(p->p.y()<45),"must favor black map edge");

        // A built factory and removable boulders in the route: use props, avoid
        // the factory footprint, keep all towers valid.
        MapGrid occupied=new MapGrid(220,100);
        occupied.rect(67,34,105,65,BUILDING);
        occupied.rect(24,65,56,79,PROP);
        var around=route(occupied,new SurgePlanner.Point(195,50),
            List.of(new SurgePlanner.Source(new SurgePlanner.Point(8,50),2)),List.of(),23);
        validate(occupied,around,new SurgePlanner.Point(195,50));

        // Full-width static wall with a gap still permits laser crossings,
        // while actual insulated structures force the route around them.
        MapGrid insulation=new MapGrid(150,110);
        insulation.rect(60,0,64,109,CLIFF);
        insulation.rect(60,48,64,55,OPEN);
        insulation.rect(70,40,72,65,INSULATED);
        var detour=route(insulation,new SurgePlanner.Point(125,50),
            List.of(),List.of(new SurgePlanner.Source(new SurgePlanner.Point(8,50),1)),31);
        validate(insulation,detour,new SurgePlanner.Point(125,50));

        // Clicking on a building or cliff should yield a nearby placement if
        // one exists; an entirely unbuildable destination region should not.
        MapGrid click=new MapGrid(120,80);
        click.rect(89,30,90,31,BUILDING);
        var onBuilding=route(click,new SurgePlanner.Point(89,30),List.of(),
            List.of(new SurgePlanner.Source(new SurgePlanner.Point(4,30),1)),9);
        validate(click,onBuilding,new SurgePlanner.Point(89,30));
        click.rect(86,26,94,36,CLIFF);
        check(route(click,new SurgePlanner.Point(90,30),List.of(),
            List.of(new SurgePlanner.Source(new SurgePlanner.Point(4,30),1)),9)==null,
            "no valid placement near click must fail without plans");

        // The Surge source is blocked; a power node nearer the destination
        // should terminate the route before it extends toward a far tower.
        MapGrid multiple=new MapGrid(200,120);
        multiple.rect(80,35,100,56,INSULATED);
        var unreachable=new SurgePlanner.Source(new SurgePlanner.Point(90,45),2);
        var reachable=new SurgePlanner.Source(new SurgePlanner.Point(8,45),2);
        var secondary=route(multiple,new SurgePlanner.Point(165,45),
            List.of(unreachable,reachable),List.of(new SurgePlanner.Source(new SurgePlanner.Point(140,45),1)),27);
        validate(multiple,secondary,new SurgePlanner.Point(165,45));
        check(!secondary.usedExistingTower() && secondary.source().center().equals(new SurgePlanner.Point(140,45)),
            "must stop at the nearer connected power node rather than continue past it");

        // Click adjacent to the world boundary: no path can extend outside
        // the map, but the remaining legal side can still connect.
        MapGrid boundary=new MapGrid(145,80);
        var boundaryRoute=route(boundary,new SurgePlanner.Point(143,1),List.of(),
            List.of(new SurgePlanner.Source(new SurgePlanner.Point(4,2),1)),11);
        validate(boundary,boundaryRoute,new SurgePlanner.Point(143,1));
        check(boundaryRoute.towers().stream().allMatch(p->p.x()>=0&&p.x()+1<145&&p.y()>=0&&p.y()+1<80),
            "no tower footprint may extend outside map");

        // If all rays to a power source are insulated, do not return an
        // apparently connected path to that source.
        MapGrid denied=new MapGrid(130,70){
            @Override public boolean linkClear(int x,int y,int x2,int y2){
                return !(x2==5 && y2==30);
            }
        };
        check(route(denied,new SurgePlanner.Point(115,30),List.of(),
            List.of(new SurgePlanner.Source(new SurgePlanner.Point(5,30),1)),13)==null,
            "insulated source must not be falsely connected");

        // Perpendicular branch into a pre-existing horizontal tower chain:
        // new first tower must be on the destination-facing side rather
        // than behind any selected origin tower.
        MapGrid branchWall=new MapGrid(250,250){
            @Override public int wallEdges(int x,int y){return y>=74 && y<=80 ? 4 : 0;}
        };
        List<SurgePlanner.Source> horizontal=List.of(
            new SurgePlanner.Source(new SurgePlanner.Point(48,90),2),
            new SurgePlanner.Source(new SurgePlanner.Point(88,90),2),
            new SurgePlanner.Source(new SurgePlanner.Point(128,90),2));
        var branch=route(branchWall,new SurgePlanner.Point(90,210),horizontal,List.of(),19);
        validate(branchWall,branch,new SurgePlanner.Point(90,210));
        check(branch.source().equals(horizontal.get(1)),"must join nearest reachable chain tower");
        check(branch.towers().get(0).y()>branch.source().center().y(),
            "first new tower must face destination, not appear behind chain");
        // Cheap wall tiles can produce a dense near-source chain. If a tower
        // can directly reach the next-but-one, it is unnecessary and should
        // not be queued merely for its wall-edge bonus.
        MapGrid cheapWall=new MapGrid(140,90){
            @Override public int wallEdges(int x,int y){return x<80 && y>=34 && y<=35 ? 4 : 0;}
        };
        var streamlined=route(cheapWall,new SurgePlanner.Point(125,49),
            List.of(new SurgePlanner.Source(new SurgePlanner.Point(7,49),2)),List.of(),25);
        validate(cheapWall,streamlined,new SurgePlanner.Point(125,49));
        for(int i=0;i+2<streamlined.towers().size();i++){
            var from=streamlined.towers().get(i);
            var skip=streamlined.towers().get(i+2);
            check(!SurgePlanner.inRange(from,skip) || !cheapWall.linkClear(from.x(),from.y(),skip.x(),skip.y()),
                "redundant near-source tower despite direct laser hop");
        }
        // Simplification must retain a middle tower if an insulated block
        // prevents a direct link across it.
        MapGrid blockedShortcut=new MapGrid(110,60){
            @Override public boolean linkClear(int x,int y,int tx,int ty){
                return !(x==20&&tx==50) && !(x==50&&tx==20);
            }
        };
        var example=new SurgePlanner.Route(List.of(new SurgePlanner.Point(20,20),
            new SurgePlanner.Point(34,20),new SurgePlanner.Point(50,20)),
            new SurgePlanner.Source(new SurgePlanner.Point(18,20),2),true);
        check(SurgePlanner.simplify(example,blockedShortcut).towers().size()==3,
            "cannot prune tower across blocked power link");
        // Deterministic field fuzz: irregular cliffs, props, buildings, and
        // insulated obstacles. Every returned path must satisfy the invariants.
        for(int seed=0;seed<12;seed++){
            MapGrid random=new MapGrid(110,95);Random rng=new Random(731+seed);
            for(int i=0;i<70;i++){
                int x=20+rng.nextInt(75),y=4+rng.nextInt(85);
                random.rect(x,y,Math.min(109,x+rng.nextInt(4)),Math.min(94,y+rng.nextInt(4)),
                    switch(rng.nextInt(4)){case 0->CLIFF;case 1->BUILDING;case 2->PROP;default->INSULATED;});
            }
            var result=route(random,new SurgePlanner.Point(100,45),List.of(),
                List.of(new SurgePlanner.Source(new SurgePlanner.Point(5,45),1)),17);
            if(result!=null)validate(random,result,new SurgePlanner.Point(100,45));
        }
        System.out.println("Surge map scenarios passed (edge, obstacles, sources, click, seeded maps)");
    }
}
