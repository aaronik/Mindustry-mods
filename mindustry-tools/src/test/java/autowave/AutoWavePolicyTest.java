package autowave;
public class AutoWavePolicyTest{
    static void check(boolean b,String reason){if(!b)throw new AssertionError(reason);}
    public static void main(String[]args){
        check(AutoWavePolicy.available(true,true,true,0,false,true,false),"ready wave should be available");
        check(!AutoWavePolicy.available(true,true,true,1,false,true,false),"remaining enemy blocks next wave");
        check(!AutoWavePolicy.available(true,true,true,0,true,true,false),"spawning blocks next wave");
        check(!AutoWavePolicy.available(true,false,true,0,false,true,false),"waves disabled by map rules");
        check(!AutoWavePolicy.available(false,true,true,0,false,true,false),"no waves enabled");
        check(!AutoWavePolicy.available(true,true,false,0,false,true,false),"unprivileged player cannot skip");
        check(!AutoWavePolicy.available(true,true,true,0,false,false,false),"menu cannot send waves");
        check(!AutoWavePolicy.available(true,true,true,0,false,true,true),"finished game cannot send waves");
        check(AutoWavePolicy.shouldSkip(true,true,12,11,1600,1000),"ready subsequent wave should send");
        check(!AutoWavePolicy.shouldSkip(true,true,12,12,1600,1000),"never send twice for same wave");
        check(!AutoWavePolicy.shouldSkip(true,true,12,11,200,1000),"rate limiting prevents repeated requests");
        check(!AutoWavePolicy.shouldSkip(false,true,12,11,1600,1000),"disabled toggle does nothing");
        System.out.println("Auto-wave policy tests passed");
    }
}
