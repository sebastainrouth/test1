package com.schembuilder;

/** Settings used only by the GUI and the rotation/mirror transform. */
public final class GuiSettings {
    private GuiSettings() {}

    /** Rotation in 90 degree steps clockwise: 0, 1, 2, 3. */
    public static volatile int rotation = 0;
    public static volatile boolean mirrorX = false;
    public static volatile boolean mirrorZ = false;

    /** Build origin typed into the GUI. */
    public static volatile int originX = 0;
    public static volatile int originY = 64;
    public static volatile int originZ = 0;

    /** Last schematic loaded from the GUI (highlighted in the list). */
    public static volatile String selectedFile = null;
}
