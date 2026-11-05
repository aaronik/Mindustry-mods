package padredirect;

public class RedirectPolicyTest{
    static void check(boolean yes,String msg){if(!yes)throw new AssertionError(msg);}
    public static void main(String[]args){
        check(RedirectPolicy.shouldRedirect(true,false,true,false),"active exporting sector should redirect");
        check(!RedirectPolicy.shouldRedirect(false,false,true,false),"sector without base is not a source");
        check(!RedirectPolicy.shouldRedirect(true,true,true,false),"local sector cannot export to itself");
        check(!RedirectPolicy.shouldRedirect(true,false,false,false),"non-exporting sector must retain destination");
        check(!RedirectPolicy.shouldRedirect(true,false,true,true),"already redirected sector needs no write");
        System.out.println("Redirect-pad policy tests passed");
    }
}
