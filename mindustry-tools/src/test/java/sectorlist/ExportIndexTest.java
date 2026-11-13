package sectorlist;

import java.util.*;

public class ExportIndexTest{
    static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    public static void main(String[]args){
        var alpha=new ExportIndex.Row(12,"Alpha",Map.of(1,120d,2,30d));
        var beta=new ExportIndex.Row(95,"Beta",Map.of(1,10d,2,200d));
        var zero=new ExportIndex.Row(7,"Empty",Map.of());
        var rows=List.of(alpha,beta,zero);
        check(ExportIndex.perMinute(2f)==120d,"per-second means converted to per-minute");
        check(ExportIndex.perMinute(Float.NaN)==0d,"NaN sanitized");
        check(ExportIndex.perMinute(-2f)==0d,"negative sanitized");
        check(alpha.rate(null)==150d,"total sums exports");
        check(alpha.breakdown().size()==2,"breakdown includes every exported item");
        check(beta.breakdown().get(0).getKey()==2,"breakdown sorted highest rate first");
        check(zero.breakdown().isEmpty(),"empty sector has no breakdown");
        var invalid=new ExportIndex.Row(8,"Invalid",Map.of(1,-2d,2,Double.NaN,3,0d,4,5d));
        check(invalid.breakdown().size()==1 && invalid.breakdown().get(0).getKey()==4,"exclude nonpositive and invalid rates");
        check(zero.rate(1)==0d,"missing item rate zero");
        check(ExportIndex.select(rows,"",null,false).get(0)==beta,"total exports descending");
        check(ExportIndex.select(rows,"",1,false).get(0)==alpha,"item filter changes ranking");
        check(ExportIndex.select(rows,"",null,true).get(0)==alpha,"name sort");
        check(ExportIndex.select(rows," ALP ",null,false).equals(List.of(alpha)),"case-insensitive search trimmed");
        check(ExportIndex.select(rows,"95",null,false).equals(List.of(beta)),"sector ID search");
        check(ExportIndex.select(rows,"missing",null,false).isEmpty(),"no matching sector");
        check(rows.size()==3,"input unchanged");
        System.out.println("Sector export indexing tests passed");
    }
}
