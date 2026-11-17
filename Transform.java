package com.schembuilder;

import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Applies the GUI's mirror + rotation to schematic-local coordinates and to
 * block states. Mirror is applied first, then rotation. Y is never changed,
 * so your bottom-up layer ordering keeps working.
 */
public final class Transform {
    private Transform() {}

    /**
     * Maps a schematic-local (x, z) to its transformed offset from the origin.
     *
     * @param width  schematic size along X
     * @param length schematic size along Z
     * @return {dx, dz}; always >= 0 so the schematic grows away from the origin
     */
    public static int[] map(int x, int z, int width, int length) {
        if (GuiSettings.mirrorX) x = width - 1 - x;
        if (GuiSettings.mirrorZ) z = length - 1 - z;
        return switch (GuiSettings.rotation & 3) {
            case 1 -> new int[] {length - 1 - z, x};          // 90  clockwise
            case 2 -> new int[] {width - 1 - x, length - 1 - z}; // 180
            case 3 -> new int[] {z, width - 1 - x};           // 270 clockwise
            default -> new int[] {x, z};
        };
    }

    /** Rotates/mirrors a block state so facing, axis, rotation, hinge etc. stay correct. */
    public static BlockState state(BlockState s) {
        if (GuiSettings.mirrorX) s = s.mirror(Mirror.FRONT_BACK); // flips east <-> west
        if (GuiSettings.mirrorZ) s = s.mirror(Mirror.LEFT_RIGHT); // flips north <-> south
        Rotation r = switch (GuiSettings.rotation & 3) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
        return s.rotate(r);
    }
}
