package unitbars;

public class BarPolicyTest{
    static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    public static void main(String[]args){
        check(BarPolicy.eligible(true,false),"living visible units accepted regardless of team");
        check(!BarPolicy.eligible(false,false),"dead unit excluded");
        check(!BarPolicy.eligible(true,true),"fog-hidden enemy excluded");
        check(!BarPolicy.eligible(false,true),"hidden dead unit excluded");
        check(BarPolicy.fraction(50f,100f)==0.5f,"half health/shield");
        check(BarPolicy.fraction(-100f,100f)==0f,"broken shield cooldown clamps empty");
        check(BarPolicy.fraction(200f,100f)==1f,"overfill clamps full");
        check(BarPolicy.fraction(10f,0f)==0f,"no capacity safe");
        check(BarPolicy.fraction(Float.NaN,100f)==0f,"invalid value safe");
        System.out.println("Visible-unit health/shield bar tests passed");
    }
}
