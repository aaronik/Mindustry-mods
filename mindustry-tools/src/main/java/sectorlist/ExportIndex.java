package sectorlist;

import java.util.*;

/** Read-only snapshot of recorded campaign export rates, in items/minute. */
public final class ExportIndex{
    private ExportIndex(){}
    public record Row(int id,String name,Map<Integer,Double> exports){
        public List<Map.Entry<Integer,Double>> breakdown(){
            return exports.entrySet().stream().filter(e->clean(e.getValue())>0d)
                .sorted(Map.Entry.<Integer,Double>comparingByValue().reversed().thenComparingInt(Map.Entry::getKey)).toList();
        }
        public double rate(Integer item){
            if(item!=null)return clean(exports.getOrDefault(item,0d));
            return exports.values().stream().mapToDouble(ExportIndex::clean).sum();
        }
    }
    public static double perMinute(float mean){return clean((double)mean)*60d;}
    private static double clean(double value){return Double.isFinite(value) && value>0d ? value : 0d;}
    public static List<Row> select(List<Row> rows,String search,Integer item,boolean byName){
        String query=search.strip().toLowerCase(Locale.ROOT);
        Comparator<Row> names=Comparator.comparing(r->r.name().toLowerCase(Locale.ROOT));
        Comparator<Row> order=byName ? names : Comparator.comparingDouble((Row r)->r.rate(item)).reversed().thenComparing(names);
        return rows.stream().filter(r->query.isEmpty() || r.name().toLowerCase(Locale.ROOT).contains(query) || Integer.toString(r.id()).contains(query))
            .sorted(order.thenComparingInt(Row::id)).toList();
    }
}
