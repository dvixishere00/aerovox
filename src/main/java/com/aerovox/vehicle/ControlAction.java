package com.aerovox.vehicle;

/** Actions a key can be bound to. Each key stores one action in 4 bits of a packed int (max 15 actions). */
public enum ControlAction {
    NONE("None"),
    PITCH_UP("Pitch up"),
    PITCH_DOWN("Pitch down"),
    ROLL_LEFT("Roll left"),
    ROLL_RIGHT("Roll right"),
    THROTTLE_UP("Throttle up"),
    THROTTLE_DOWN("Throttle down"),
    BOMB_RELEASE("Release bomb"),
    TOGGLE_MOTORS("Toggle motors"),
    TOGGLE_WORKERS("Toggle workers");

    private final String label;

    ControlAction(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static ControlAction of(int packed, int key) {
        int index = (packed >> (4 * key)) & 0xF;
        return index < values().length ? values()[index] : NONE;
    }

    public static int pack(ControlAction[] keys) {
        int packed = 0;
        for (int i = 0; i < keys.length; i++) {
            packed |= keys[i].ordinal() << (4 * i);
        }
        return packed;
    }

    /** W/S pitch, A/D roll, Space throttle up, Ctrl throttle down. */
    public static final int DEFAULT_BINDINGS = pack(new ControlAction[]{
            PITCH_UP, PITCH_DOWN, ROLL_LEFT, ROLL_RIGHT, THROTTLE_UP, THROTTLE_DOWN});
}
