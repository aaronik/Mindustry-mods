package wallpack;
import java.util.*;
public class WallPlannerTest{
    static class Grid implements WallPlanner.Grid{
        Set<WallPlanner.Point> blocked=new HashSet<>(), noBattery=new HashSet<>();
        public boolean free(int x,int y){return !blocked.contains(new WallPlanner.Point(x,y));}
        public boolean validPanel(int x,int y){return true;}
        public boolean validBattery(int x,int y){return !noBattery.contains(new WallPlanner.Point(x,y));}
    }
    static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);}
    static void verify(Grid grid,WallPlanner.Layout layout,int left,int bottom,int right,int top,int size,int offset){
        Set<WallPlanner.Point> used=new HashSet<>();
        for(var panel:layout.panels()) for(int dy=0;dy<size;dy++) for(int dx=0;dx<size;dx++){
            var p=new WallPlanner.Point(panel.x()+offset+dx,panel.y()+offset+dy);
            check(p.x()>=left&&p.x()<=right&&p.y()>=bottom&&p.y()<=top,"panel outside rectangle");
            check(grid.free(p.x(),p.y())&&used.add(p),"panel overlap or building replacement");
        }
        for(var battery:layout.batteries()){
            check(battery.x()>=left&&battery.x()<=right&&battery.y()>=bottom&&battery.y()<=top,"small wall outside rectangle");
            check(grid.free(battery.x(),battery.y())&&grid.validBattery(battery.x(),battery.y())&&used.add(battery),"small wall overlap or invalid placement");
        }
        for(int y=bottom;y<=top;y++)for(int x=left;x<=right;x++){
            if(grid.free(x,y)&&grid.validBattery(x,y)) check(used.contains(new WallPlanner.Point(x,y)),"unfilled available small-wall tile");
        }
    }
    public static void main(String[] args){
        Grid g=new Grid();var exact=WallPlanner.plan(g,0,0,5,5,2,0);
        check(exact.panels().size()==9&&exact.batteries().isEmpty(),"6x6 packs nine large walls");verify(g,exact,0,0,5,5,2,0);
        var fringe=WallPlanner.plan(g,0,0,6,6,2,0);
        check(fringe.panels().size()==9&&fringe.batteries().size()==13,"7x7 has 13 small-wall spaces");verify(g,fringe,0,0,6,6,2,0);
        Grid obstacle=new Grid();obstacle.blocked.add(new WallPlanner.Point(3,3));obstacle.blocked.add(new WallPlanner.Point(1,6));
        obstacle.noBattery.add(new WallPlanner.Point(0,0));
        var irregular=WallPlanner.plan(obstacle,0,0,8,8,2,0);
        check(!irregular.panels().isEmpty(),"should fit large walls around obstacles");verify(obstacle,irregular,0,0,8,8,2,0);
        for(int i=0;i<60;i++){
            Grid random=new Grid();Random r=new Random(i);
            for(int y=0;y<12;y++)for(int x=0;x<12;x++)if(r.nextFloat()<0.14)random.blocked.add(new WallPlanner.Point(x,y));
            verify(random,WallPlanner.plan(random,0,0,11,11,2,0),0,0,11,11,2,0);
        }
        System.out.println("Wall-pack layout tests passed");
    }
}
