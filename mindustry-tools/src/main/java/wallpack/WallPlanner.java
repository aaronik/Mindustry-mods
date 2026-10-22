package wallpack;

import java.util.*;

/** Tile-space rectangle packer; actual Mindustry placement checks are delegated to Grid. */
public final class WallPlanner{
    private WallPlanner(){}
    public record Point(int x, int y){}
    public record Layout(List<Point> panels, List<Point> batteries){}
    public interface Grid{
        boolean free(int x, int y);
        boolean validPanel(int x, int y);
        boolean validBattery(int x, int y);
    }

    /** Rect bounds are inclusive; offset is the game's Block.sizeOffset. */
    public static Layout plan(Grid grid, int left, int bottom, int right, int top, int size, int offset){
        if(left > right || bottom > top || size < 1) return new Layout(List.of(), List.of());
        List<Point> candidates = new ArrayList<>();
        for(int y = bottom - offset; y + offset + size - 1 <= top; y++){
            for(int x = left - offset; x + offset + size - 1 <= right; x++){
                if(!grid.validPanel(x, y)) continue;
                boolean valid = true;
                for(int dy = 0; dy < size && valid; dy++) for(int dx = 0; dx < size; dx++){
                    if(!grid.free(x + offset + dx, y + offset + dy)){valid = false; break;}
                }
                if(valid) candidates.add(new Point(x, y));
            }
        }
        // Try each grid phase and greedily use remaining free candidates.
        // Count of large walls dominates any preference for small walls.
        Layout best = new Layout(List.of(), List.of());
        for(int phaseY = 0; phaseY < size; phaseY++) for(int phaseX = 0; phaseX < size; phaseX++){
            Set<Point> used = new HashSet<>();
            List<Point> panels = new ArrayList<>();
            for(int pass = 0; pass < 2; pass++) for(Point candidate : candidates){
                int fx = candidate.x + offset, fy = candidate.y + offset;
                boolean aligned = Math.floorMod(fx - left, size) == phaseX && Math.floorMod(fy - bottom, size) == phaseY;
                if((pass == 0) != aligned) continue;
                boolean clear = true;
                for(int dy = 0; dy < size && clear; dy++) for(int dx = 0; dx < size; dx++){
                    if(used.contains(new Point(fx + dx, fy + dy))){clear = false; break;}
                }
                if(!clear) continue;
                panels.add(candidate);
                for(int dy = 0; dy < size; dy++) for(int dx = 0; dx < size; dx++) used.add(new Point(fx + dx, fy + dy));
            }
            List<Point> batteries = new ArrayList<>();
            for(int y = bottom; y <= top; y++) for(int x = left; x <= right; x++){
                Point p = new Point(x, y);
                if(!used.contains(p) && grid.free(x, y) && grid.validBattery(x, y)) batteries.add(p);
            }
            if(panels.size() > best.panels().size() || panels.size() == best.panels().size() && batteries.size() > best.batteries().size()){
                best = new Layout(panels, batteries);
            }
        }
        return best;
    }
}
