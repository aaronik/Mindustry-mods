package powerhud;

import arc.scene.Element;

/** Release only this panel's focus; never interfere with another UI's scrolling. */
public final class ScrollFocusPolicy{
    private ScrollFocusPolicy(){}
    public static boolean release(Element panel,Element focus,Element hovered,boolean visible){
        return focus!=null && focus.isDescendantOf(panel) &&
            (!visible || hovered==null || !hovered.isDescendantOf(panel));
    }
}
