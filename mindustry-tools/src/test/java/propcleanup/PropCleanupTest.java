package propcleanup;

public class PropCleanupTest{
    static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);}
    public static void main(String[] args){
        check(PropCleanup.eligible(true,true,false,true,false),"natural removable boulder should queue");
        check(!PropCleanup.eligible(true,false,false,true,false),"unreplaceable map cliff must remain");
        check(!PropCleanup.eligible(false,true,false,true,false),"replaceable synthetic block must remain");
        check(!PropCleanup.eligible(true,true,true,true,false),"tile containing building must remain");
        check(!PropCleanup.eligible(true,true,false,false,false),"server-disallowed breaking must not queue");
        check(!PropCleanup.eligible(true,true,false,true,true),"existing break plan must not duplicate");
        System.out.println("Prop cleanup selection tests passed");
    }
}
