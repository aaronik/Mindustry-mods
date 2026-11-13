package enemyinfo;

public class HoverPolicyTest{
    static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    public static void main(String[]args){
        check(HoverPolicy.eligible(true,false,false,false),"visible enemy accepted");
        check(!HoverPolicy.eligible(false,false,false,false),"destroyed target excluded");
        check(!HoverPolicy.eligible(true,true,false,false),"friendly excluded");
        check(!HoverPolicy.eligible(true,false,true,false),"derelict excluded");
        check(!HoverPolicy.eligible(true,false,false,true),"fog-hidden enemy excluded");
        check(HoverPolicy.hovered(100f,20f),"edge of large unit accepted");
        check(!HoverPolicy.hovered(101f,20f),"outside unit rejected");
        check(HoverPolicy.hovered(16f,2f),"small unit has minimum hover radius");
        check(!HoverPolicy.hovered(17f,2f),"minimum radius bounded");
        check(HoverPolicy.showRange(240f),"positive turret range drawn");
        check(!HoverPolicy.showRange(0f),"no zero-radius circle");
        check(!HoverPolicy.showRange(-1f),"negative range excluded");
        check(!HoverPolicy.showRange(Float.NaN),"NaN range excluded");
        check(!HoverPolicy.showRange(Float.POSITIVE_INFINITY),"infinite range excluded");
        System.out.println("Enemy hover visibility, hit and range tests passed");
    }
}
