package minepack;

import java.util.ArrayList;
import java.util.List;

/** Fill every individually valid tile of an inclusive rectangle with a 1x1 mine. */
public final class MinePlanner{
    private MinePlanner(){}
    public record Point(int x, int y){}
    public interface Grid{
        boolean free(int x, int y);
        boolean validMine(int x, int y);
    }
    public static List<Point> plan(Grid grid, int left, int bottom, int right, int top){
        List<Point> result = new ArrayList<>();
        if(left > right || bottom > top) return result;
        for(int y = bottom; y <= top; y++){
            for(int x = left; x <= right; x++){
                if(grid.free(x, y) && grid.validMine(x, y)) result.add(new Point(x, y));
            }
        }
        return result;
    }
}
