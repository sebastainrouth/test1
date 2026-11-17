package com.schembuilder;

/** Runtime settings, changed with /schem commands. */
public final class Config {
    public static final double LEGIT_MAX_REACH = 4.5;
    /** Servers accept clicks up to (block interaction range + 1.0) from the eyes, so ~5.5 for the default 4.5. */
    public static final double EXPLOIT_MAX_REACH = 6.0;
    public static final double EXPLOIT_DEFAULT_REACH = 5.4;

    public static volatile double reach = 4.5;
    public static volatile boolean exploit = false;
    public static volatile int blocksPerTick = 2;
    /** Hotbar slot (0-8) used to hold the block being placed. Creative mode overwrites it. */
    public static volatile int hotbarSlot = 8;

    private Config() {}
}
