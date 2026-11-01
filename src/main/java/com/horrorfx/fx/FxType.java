package com.horrorfx.fx;

import java.util.ArrayList;
import java.util.List;

/**
 * Effect list. Second argument = which generic GUI controls the effect uses:
 * C = color, O = opacity, S = speed, Z = size. (Intensity is always used.)
 * Draw order on the client = order of this enum.
 */
public enum FxType {
    DARKNESS("darkness", "COZ"),
    BLOOD("blood", "COSZ"),
    ROBOT("robot", "COSZ"),
    WHITEOUT("whiteout", "COZ"),
    SHAKE("shake", "S"),
    BLUR("blur", ""),
    GLOW("glow", "COSZ"),
    VIGNETTE("vignette", "COSZ"),
    GRAIN("grain", "OSZ"),
    FILM("film", "COSZ"),
    FLICKER("flicker", "COS"),
    LETTERBOX("letterbox", "COZ"),
    TINT("tint", "COS"),
    STATIC("static", "OS");

    public final String id;
    private final String uses;

    FxType(String id, String uses) {
        this.id = id;
        this.uses = uses;
    }

    public boolean uses(char c) {
        return uses.indexOf(c) >= 0;
    }

    public String langKey() {
        return "horrorfx.fx." + id;
    }

    public static FxType byId(String s) {
        if (s == null) return null;
        for (FxType t : values()) {
            if (t.id.equalsIgnoreCase(s)) return t;
        }
        return null;
    }

    public static FxType byOrdinal(int i) {
        FxType[] v = values();
        return i >= 0 && i < v.length ? v[i] : null;
    }

    public static List<String> ids() {
        List<String> l = new ArrayList<>();
        for (FxType t : values()) l.add(t.id);
        return l;
    }
}
