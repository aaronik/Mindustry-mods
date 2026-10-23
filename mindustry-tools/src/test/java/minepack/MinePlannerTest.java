package minepack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MinePlannerTest{
    static void check(boolean yes, String message){if(!yes) throw new AssertionError(message);}
    public static void main(String[] args){
        Set<MinePlanner.Point> blocked = Set.of(new MinePlanner.Point(2, 1));
        Set<MinePlanner.Point> invalid = Set.of(new MinePlanner.Point(3, 2));
        MinePlanner.Grid grid = new MinePlanner.Grid(){
            public boolean free(int x, int y){return !blocked.contains(new MinePlanner.Point(x,y));}
            public boolean validMine(int x, int y){return !invalid.contains(new MinePlanner.Point(x,y));}
        };
        List<MinePlanner.Point> result = MinePlanner.plan(grid, 1, 1, 3, 3);
        check(result.size() == 7, "3x3 area minus building and invalid tile must yield seven mines");
        check(!result.contains(new MinePlanner.Point(2,1)) && !result.contains(new MinePlanner.Point(3,2)), "blocked tiles must be skipped");
        check(new HashSet<>(result).size() == result.size(), "no duplicate mines");
        check(MinePlanner.plan(grid, 4, 2, 3, 3).isEmpty(), "reversed rectangle must be empty");
        check(MinePlanner.plan(grid, 0, 0, 199, 199).size() == 39998, "200x200 fill must cover all valid tiles");
        check(MinePackMod.mayPlaceOver(true,false,false), "air must be free");
        check(MinePackMod.mayPlaceOver(false,true,false), "removable props must be free");
        check(!MinePackMod.mayPlaceOver(false,false,false), "unreplaceable terrain must be blocked");
        check(!MinePackMod.mayPlaceOver(false,true,true), "buildings must be protected");
        System.out.println("Mine-pack layout tests passed");
    }
}
