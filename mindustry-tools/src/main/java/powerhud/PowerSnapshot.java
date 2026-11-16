package powerhud;

import java.util.*;

/** Values use vanilla scaled power rates (per tick), independent of speed presets. */
public final class PowerSnapshot{
    private PowerSnapshot(){}
    public record Network(int id,int buildings,float produced,float needed,float stored,float capacity){
        public double generation(){return positive(produced)*60d;}
        public double demand(){return positive(needed)*60d;}
        public double balance(){return generation()-demand();}
        public double charge(){return positive(stored);}
        public double batteryCapacity(){return positive(capacity);}
    }
    public static int toggleSelection(int current,int clicked){return current==clicked?-1:clicked;}
    public static boolean meaningful(int producers,int consumers,int batteries){
        return producers>0 || consumers>0 || batteries>0;
    }
    private static double positive(float value){return Float.isFinite(value) && value>0f?value:0d;}
    public static List<Network> unique(Collection<Network> networks){
        Map<Integer,Network> unique=new HashMap<>();
        for(Network network:networks)if(network.buildings()>0)unique.putIfAbsent(network.id(),network);
        return unique.values().stream().sorted(Comparator.comparingInt(Network::buildings).reversed().thenComparingInt(Network::id)).toList();
    }
}
