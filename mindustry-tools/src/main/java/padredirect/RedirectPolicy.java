package padredirect;

/** Campaign-sector destination eligibility, independent of item selection. */
public final class RedirectPolicy{
    private RedirectPolicy(){}
    public static boolean shouldRedirect(boolean hasBase,boolean sameSector,boolean hasExports,
                                         boolean alreadyTargetingLocal){
        return hasBase && !sameSector && hasExports && !alreadyTargetingLocal;
    }
}
