package batterypack;
import java.util.*;
public class BatteryPlannerTest{
    static class Grid implements BatteryPlanner.Grid{
        Set<BatteryPlanner.Point> blocked=new HashSet<>(), noBattery=new HashSet<>();
        public boolean free(int x,int y){return !blocked.contains(new BatteryPlanner.Point(x,y));}
        public boolean validPanel(int x,int y){return true;}
        public boolean validBattery(int x,int y){return !noBattery.contains(new BatteryPlanner.Point(x,y));}
    }
    static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);}
    static void verify(Grid grid,BatteryPlanner.Layout layout,int left,int bottom,int right,int top,int size,int offset){
        Set<BatteryPlanner.Point> used=new HashSet<>();
        for(var panel:layout.panels()) for(int dy=0;dy<size;dy++) for(int dx=0;dx<size;dx++){
            var p=new BatteryPlanner.Point(panel.x()+offset+dx,panel.y()+offset+dy);
            check(p.x()>=left&&p.x()<=right&&p.y()>=bottom&&p.y()<=top,"panel outside rectangle");
            check(grid.free(p.x(),p.y())&&used.add(p),"panel overlap or building replacement");
        }
        for(var battery:layout.batteries()){
            check(battery.x()>=left&&battery.x()<=right&&battery.y()>=bottom&&battery.y()<=top,"battery outside rectangle");
            check(grid.free(battery.x(),battery.y())&&grid.validBattery(battery.x(),battery.y())&&used.add(battery),"battery overlap or invalid placement");
        }
        for(int y=bottom;y<=top;y++)for(int x=left;x<=right;x++){
            if(grid.free(x,y)&&grid.validBattery(x,y)) check(used.contains(new BatteryPlanner.Point(x,y)),"unfilled available battery tile");
        }
    }
    public static void main(String[] args){
        Grid g=new Grid();var exact=BatteryPlanner.plan(g,0,0,5,5,3,-1);
        check(exact.panels().size()==4&&exact.batteries().isEmpty(),"6x6 packs four panels");verify(g,exact,0,0,5,5,3,-1);
        var fringe=BatteryPlanner.plan(g,0,0,6,6,3,-1);
        check(fringe.panels().size()==4&&fringe.batteries().size()==13,"7x7 has 13 battery spaces");verify(g,fringe,0,0,6,6,3,-1);
        Grid obstacle=new Grid();obstacle.blocked.add(new BatteryPlanner.Point(3,3));obstacle.blocked.add(new BatteryPlanner.Point(1,6));
        obstacle.noBattery.add(new BatteryPlanner.Point(0,0));
        var irregular=BatteryPlanner.plan(obstacle,0,0,8,8,3,-1);
        check(!irregular.panels().isEmpty(),"should fit panels around obstacles");verify(obstacle,irregular,0,0,8,8,3,-1);
        for(int i=0;i<60;i++){
            Grid random=new Grid();Random r=new Random(i);
            for(int y=0;y<12;y++)for(int x=0;x<12;x++)if(r.nextFloat()<0.14)random.blocked.add(new BatteryPlanner.Point(x,y));
            verify(random,BatteryPlanner.plan(random,0,0,11,11,3,-1),0,0,11,11,3,-1);
        }
        var wide=BatteryPlanner.plan(new Grid(),0,0,199,199,3,-1);
        check(wide.panels().size()==4356 && wide.batteries().size()==796,
            "200x200 should pack 4356 panels and 796 batteries: " + wide);
        check(BatteryPackMod.mayPlaceOver(true,false,false),"empty tile is buildable");
        check(BatteryPackMod.mayPlaceOver(false,true,false),"removable terrain must be buildable");
        check(!BatteryPackMod.mayPlaceOver(false,false,false),"unreplaceable terrain must remain blocked");
        check(!BatteryPackMod.mayPlaceOver(false,true,true),"real building on prop remains blocked");
        check(!BatteryPackMod.mayPlaceOver(true,false,true),"real building on empty tile remains blocked");
        System.out.println("Battery-pack layout tests passed");
    }
}
