package powerhud;

import java.util.List;

public class PowerSnapshotTest{
    static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    public static void main(String[]args){
        var large=new PowerSnapshot.Network(7,100,10f,4f,500f,1000f);
        var deficit=new PowerSnapshot.Network(9,2,0f,2f,0f,0f);
        var rows=PowerSnapshot.unique(List.of(deficit,large,large));
        check(rows.size()==2,"deduplicate same graph without combining disconnected groups");
        check(rows.get(0).id()==7,"largest networks first");
        check(large.generation()==600d && large.demand()==240d,"scaled rates converted to per second");
        check(large.balance()==360d && deficit.balance()==-120d,"surplus and deficit separate");
        check(large.charge()==500d && large.batteryCapacity()==1000d,"battery energy not multiplied by time");
        var invalid=new PowerSnapshot.Network(1,1,Float.NaN,-1f,-2f,Float.POSITIVE_INFINITY);
        check(invalid.generation()==0d && invalid.demand()==0d && invalid.charge()==0d && invalid.batteryCapacity()==0d,"invalid metrics safe");
        check(!PowerSnapshot.meaningful(0,0,0),"node-only and inert graphs omitted");
        check(PowerSnapshot.meaningful(0,1,0),"idle disconnected consumer retained even at zero demand");
        check(PowerSnapshot.meaningful(1,0,0),"standalone generator retained");
        check(PowerSnapshot.meaningful(0,0,1),"standalone battery retained");
        System.out.println("Power network snapshot tests passed");
    }
}
