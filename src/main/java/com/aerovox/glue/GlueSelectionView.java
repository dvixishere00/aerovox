package com.aerovox.glue;

import net.minecraft.core.BlockPos;

/** Client-side mirror of the superglue selection, used only for drawing the outline. */
public final class GlueSelectionView {
    private static BlockPos first;
    private static BlockPos second;

    private GlueSelectionView() {}

    public static BlockPos first() { return first; }
    public static BlockPos second() { return second; }

    /** Same rules as the server: shift + click confirms (the server does the gluing), otherwise sets a corner. */
    public static void click(BlockPos pos, boolean shift) {
        if (shift && first != null && second != null) {
            clear();
            return;
        }
        if (first == null || second != null) {
            first = pos.immutable();
            second = null;
        } else {
            second = pos.immutable();
        }
    }

    public static void clear() {
        first = null;
        second = null;
    }
}
