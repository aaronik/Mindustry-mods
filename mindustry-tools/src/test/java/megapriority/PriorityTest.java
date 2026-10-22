package megapriority;
public class PriorityTest{
    static void check(boolean pass){if(!pass)throw new AssertionError("priority selection failed");}
    public static void main(String[] args){
        check(Priority.desired(true,true)==Priority.Mode.rebuild);
        check(Priority.desired(false,true)==Priority.Mode.repair);
        check(Priority.desired(false,false)==Priority.Mode.mine);
        check(Priority.choose(Priority.Mode.mine,Priority.Mode.rebuild,false,false)==Priority.Mode.rebuild);
        check(Priority.choose(Priority.Mode.repair,Priority.Mode.rebuild,false,false)==Priority.Mode.rebuild);
        check(Priority.choose(Priority.Mode.rebuild,Priority.Mode.repair,true,true)==Priority.Mode.rebuild);
        check(Priority.choose(Priority.Mode.rebuild,Priority.Mode.repair,false,false)==Priority.Mode.rebuild);
        check(Priority.choose(Priority.Mode.rebuild,Priority.Mode.repair,false,true)==Priority.Mode.repair);
        check(Priority.choose(Priority.Mode.repair,Priority.Mode.mine,false,false)==Priority.Mode.repair);
        check(Priority.choose(Priority.Mode.repair,Priority.Mode.mine,false,true)==Priority.Mode.mine);
        System.out.println("Mega priority tests passed");
    }
}
