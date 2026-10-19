package autodrill;

import java.util.*;

/** Tile-coordinate planner; game validation is supplied by the caller. */
public final class Planner{
    private Planner(){}

    public record Point(int x, int y){}
    public interface Grid{
        boolean inBounds(int x, int y);
        boolean ore(int x, int y);
        boolean mineable(int x, int y);
        boolean free(int x, int y);
        boolean validDrill(int x, int y);
        boolean validRouter(int x, int y);
    }
    public record Layout(List<Point> drills, List<Point> routers, int oreTiles, int coveredTiles, boolean truncated){}

    public static Layout plan(Grid grid, int startX, int startY, int size, int sizeOffset, int limit){
        if(size < 1 || limit < 1 || !grid.inBounds(startX, startY) || !grid.ore(startX, startY)){
            return new Layout(List.of(), List.of(), 0, 0, false);
        }
        Set<Point> patch = new HashSet<>();
        ArrayDeque<Point> frontier = new ArrayDeque<>();
        Point start = new Point(startX, startY);
        patch.add(start);
        frontier.add(start);
        boolean truncated = false;
        int[] dx = {1, -1, 0, 0}, dy = {0, 0, 1, -1};
        while(!frontier.isEmpty()){
            Point p = frontier.removeFirst();
            for(int d = 0; d < 4; d++){
                Point n = new Point(p.x + dx[d], p.y + dy[d]);
                if(grid.inBounds(n.x, n.y) && grid.ore(n.x, n.y) && !patch.contains(n)){
                    if(patch.size() == limit){ truncated = true; continue; }
                    patch.add(n);
                    frontier.add(n);
                }
            }
        }
        // Each candidate must contain at least one tile of the selected deposit.
        Set<Point> anchors = new HashSet<>();
        for(Point ore : patch){
            for(int sx = 0; sx < size; sx++) for(int sy = 0; sy < size; sy++){
                anchors.add(new Point(ore.x - sizeOffset - sx, ore.y - sizeOffset - sy));
            }
        }
        List<Candidate> choices = new ArrayList<>();
        for(Point a : anchors){
            if(!grid.validDrill(a.x, a.y)) continue;
            List<Point> footprint = footprint(a, size, sizeOffset);
            boolean free = true;
            int target = 0, others = 0;
            for(Point p : footprint){
                if(!grid.inBounds(p.x, p.y) || !grid.free(p.x, p.y)){free = false; break;}
                if(patch.contains(p) && grid.mineable(p.x, p.y)) target++;
                else if(grid.mineable(p.x, p.y)) others++;
            }
            // The game's drill picks a dominant ore, so avoid mixed-ore drills
            // whose primary output might not be the ore that was clicked.
            if(free && target > 0 && target > others) choices.add(new Candidate(a, footprint));
        }
        choices.sort(Comparator.comparingInt((Candidate c) -> c.anchor.y).thenComparingInt(c -> c.anchor.x));
        // Search joint drill/router layouts. Horizontal and vertical router
        // lanes are reserved BEFORE packing, so inner drills keep an output.
        // Include the unrestricted layout for small/irregular deposits.
        int minX = patch.stream().mapToInt(Point::x).min().orElse(startX);
        int minY = patch.stream().mapToInt(Point::y).min().orElse(startY);
        Layout best = new Layout(List.of(), List.of(), patch.size(), 0, truncated);
        int period = size + 1;
        long deadline = System.nanoTime() + 350_000_000L;
        for(int orientation = -1; orientation < 2; orientation++){
            for(int phase = 0; phase < (orientation < 0 ? 1 : period); phase++){
                if(orientation >= 0 && System.nanoTime() > deadline) return best;
                final int axis = orientation, stripe = phase;
                Set<Point> reserved = new HashSet<>();
                if(axis >= 0){
                    for(Candidate c : choices) for(Point p : c.tiles){
                        int coordinate = axis == 0 ? p.y - minY : p.x - minX;
                        if(Math.floorMod(coordinate, period) == stripe && grid.validRouter(p.x, p.y)) reserved.add(p);
                    }
                }
                List<Candidate> selected = pack(choices, patch, grid, reserved);
                Network network = connect(grid, selected, 12);
                Set<Point> covered = new HashSet<>();
                List<Point> drills = new ArrayList<>();
                for(Candidate c : network.connected){
                    drills.add(c.anchor);
                    for(Point p : c.tiles) if(patch.contains(p) && grid.mineable(p.x, p.y)) covered.add(p);
                }
                Set<Point> border = border(grid, network, 12);
                List<Point> routers = new ArrayList<>(border);
                routers.sort(pointOrder());
                Layout result = new Layout(drills, routers, patch.size(), covered.size(), truncated);
                if(result.coveredTiles() > best.coveredTiles() ||
                    result.coveredTiles() == best.coveredTiles() && result.routers().size() < best.routers().size()) best = result;
            }
        }
        return best;
    }

    private static List<Candidate> pack(List<Candidate> choices, Set<Point> patch, Grid grid, Set<Point> reserved){
        Set<Point> used = new HashSet<>(), covered = new HashSet<>();
        List<Candidate> selected = new ArrayList<>();
        while(true){
            Candidate best = null;
            int bestNew = 0, bestTotal = 0;
            for(Candidate c : choices){
                if(c.tiles.stream().anyMatch(p -> used.contains(p) || reserved.contains(p))) continue;
                int newly = 0, total = 0;
                for(Point p : c.tiles) if(patch.contains(p) && grid.mineable(p.x, p.y)){
                    total++;
                    if(!covered.contains(p)) newly++;
                }
                if(newly > bestNew || newly == bestNew && newly > 0 && total > bestTotal){
                    best = c; bestNew = newly; bestTotal = total;
                }
            }
            if(best == null) break;
            selected.add(best);
            used.addAll(best.tiles);
            for(Point p : best.tiles) if(patch.contains(p) && grid.mineable(p.x, p.y)) covered.add(p);
        }
        return selected;
    }

    /** Include every reachable free side and corner around the drills, not just
     * a minimal backbone. Corner routers bridge diagonal-only perimeter turns. */
    private static Set<Point> border(Grid grid, Network base, int margin){
        if(base.routers.isEmpty()) return Set.of();
        Set<Point> footprints = new HashSet<>(), perimeter = new HashSet<>();
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for(Candidate c : base.connected) for(Point tile : c.tiles){
            footprints.add(tile);
            minX = Math.min(minX, tile.x); maxX = Math.max(maxX, tile.x);
            minY = Math.min(minY, tile.y); maxY = Math.max(maxY, tile.y);
        }
        minX -= margin; maxX += margin; minY -= margin; maxY += margin;
        for(Point tile : footprints){
            for(int dx = -1; dx <= 1; dx++) for(int dy = -1; dy <= 1; dy++){
                if(dx == 0 && dy == 0) continue;
                Point neighbor = new Point(tile.x + dx, tile.y + dy);
                if(!footprints.contains(neighbor) && grid.inBounds(neighbor.x, neighbor.y) &&
                    grid.free(neighbor.x, neighbor.y) && grid.validRouter(neighbor.x, neighbor.y)) perimeter.add(neighbor);
            }
        }
        Set<Point> result = new HashSet<>(base.routers);
        // Multi-source BFS supplies a path from the backbone to every
        // reachable perimeter tile. Impossible isolated edges are omitted.
        Map<Point, Point> previous = new HashMap<>();
        ArrayDeque<Point> queue = new ArrayDeque<>();
        for(Point router : result){previous.put(router, router); queue.add(router);}
        while(!queue.isEmpty()){
            Point tile = queue.removeFirst();
            for(Point next : neighbors(tile)){
                if(next.x < minX || next.x > maxX || next.y < minY || next.y > maxY ||
                    previous.containsKey(next) || footprints.contains(next) ||
                    !grid.inBounds(next.x, next.y) || !grid.free(next.x, next.y) || !grid.validRouter(next.x, next.y)) continue;
                previous.put(next, tile);
                queue.add(next);
            }
        }
        for(Point edge : perimeter){
            if(!previous.containsKey(edge)) continue;
            for(Point p = edge; result.add(p); p = previous.get(p)){}
        }
        return result;
    }

    private record Network(Set<Point> routers, List<Candidate> connected){}

    private static Network connect(Grid grid, List<Candidate> selected, int margin){
        if(selected.isEmpty()) return new Network(Set.of(), List.of());
        Set<Point> footprints = new HashSet<>();
        for(Candidate c : selected) footprints.addAll(c.tiles);
        List<Set<Point>> access = new ArrayList<>();
        for(Candidate c : selected){
            Set<Point> edge = new HashSet<>();
            for(Point t : c.tiles) for(Point n : neighbors(t)){
                if(!footprints.contains(n) && grid.inBounds(n.x, n.y) && grid.free(n.x, n.y) && grid.validRouter(n.x, n.y)) edge.add(n);
            }
            access.add(edge);
        }
        int first = -1;
        for(int i = 0; i < selected.size(); i++) if(!access.get(i).isEmpty()){first = i; break;}
        if(first < 0) return new Network(Set.of(), List.of());
        Set<Point> network = new HashSet<>();
        network.add(access.get(first).stream().max(Comparator.comparingInt((Point p) -> {
            int open = 0;
            for(Point n : neighbors(p)) if(!footprints.contains(n) && grid.inBounds(n.x, n.y) && grid.free(n.x, n.y) && grid.validRouter(n.x, n.y)) open++;
            return open;
        }).thenComparingInt(Point::y).thenComparingInt(Point::x)).orElseThrow());
        boolean[] connected = new boolean[selected.size()];
        connected[first] = true;
        // Additional drills may already touch the backbone.
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for(Candidate c : selected) for(Point t : c.tiles){
            minX = Math.min(minX, t.x); maxX = Math.max(maxX, t.x);
            minY = Math.min(minY, t.y); maxY = Math.max(maxY, t.y);
        }
        minX -= margin; maxX += margin; minY -= margin; maxY += margin;
        while(true){
            for(int i = 0; i < selected.size(); i++) if(!connected[i] && !Collections.disjoint(access.get(i), network)) connected[i] = true;
            Set<Point> goals = new HashSet<>();
            for(int i = 0; i < selected.size(); i++) if(!connected[i]) goals.addAll(access.get(i));
            if(goals.isEmpty()) break;
            // Multi-source BFS finds the shortest cardinal router path to an
            // as-yet-unconnected drill, through unoccupied, placeable tiles.
            ArrayDeque<Point> queue = new ArrayDeque<>();
            Map<Point, Point> previous = new HashMap<>();
            for(Point p : network){queue.add(p); previous.put(p, p);}
            Point reached = null;
            while(!queue.isEmpty() && reached == null){
                Point p = queue.removeFirst();
                if(goals.contains(p)){reached = p; break;}
                for(Point n : neighbors(p)){
                    if(n.x < minX || n.x > maxX || n.y < minY || n.y > maxY || previous.containsKey(n) || footprints.contains(n) ||
                        !grid.inBounds(n.x, n.y) || !grid.free(n.x, n.y) || !grid.validRouter(n.x, n.y)) continue;
                    previous.put(n, p);
                    queue.add(n);
                }
            }
            if(reached == null) break;
            for(Point p = reached; !network.contains(p); p = previous.get(p)) network.add(p);
        }
        // Inclusion-minimal: remove each surplus router if the remaining
        // network is still connected and every retained drill touches it.
        boolean removed;
        do{
            removed = false;
            List<Point> removeOrder = new ArrayList<>(network);
            removeOrder.sort(pointOrder());
            for(Point p : removeOrder){
                if(network.size() <= 1) break;
                network.remove(p);
                if(validNetwork(network, access, connected)) removed = true;
                else network.add(p);
            }
        }while(removed);
        List<Candidate> accepted = new ArrayList<>();
        for(int i = 0; i < selected.size(); i++) if(connected[i]) accepted.add(selected.get(i));
        return new Network(network, accepted);
    }

    private static boolean validNetwork(Set<Point> routers, List<Set<Point>> access, boolean[] attached){
        if(routers.isEmpty()) return false;
        for(int i = 0; i < attached.length; i++) if(attached[i] && Collections.disjoint(access.get(i), routers)) return false;
        Set<Point> seen = new HashSet<>();
        ArrayDeque<Point> queue = new ArrayDeque<>();
        Point start = routers.iterator().next(); seen.add(start); queue.add(start);
        while(!queue.isEmpty()) for(Point n : neighbors(queue.removeFirst())){
            if(routers.contains(n) && seen.add(n)) queue.add(n);
        }
        return seen.size() == routers.size();
    }

    private static List<Point> neighbors(Point p){
        return List.of(new Point(p.x + 1, p.y), new Point(p.x - 1, p.y), new Point(p.x, p.y + 1), new Point(p.x, p.y - 1));
    }
    private static Comparator<Point> pointOrder(){
        return Comparator.comparingInt(Point::y).thenComparingInt(Point::x);
    }

    public static List<Point> footprint(Point anchor, int size, int offset){
        List<Point> result = new ArrayList<>(size * size);
        for(int x = 0; x < size; x++) for(int y = 0; y < size; y++){
            result.add(new Point(anchor.x + offset + x, anchor.y + offset + y));
        }
        return result;
    }
    private record Candidate(Point anchor, List<Point> tiles){}
}
