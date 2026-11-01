package com.horrorfx.client;

import com.horrorfx.fx.FxSettings;
import com.horrorfx.fx.FxType;
import net.minecraft.client.Minecraft;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;

public final class ClientFxManager {
    public static final Map<FxType, ActiveFx> ACTIVE = new EnumMap<>(FxType.class);

    private ClientFxManager() {}

    /** preview = true: uses the given settings object live (sliders update it) and ignores duration. */
    public static void start(FxType type, FxSettings settings, boolean preview) {
        Minecraft mc = Minecraft.getInstance();
        ActiveFx old = ACTIVE.remove(type);
        if (old != null) old.cleanup(mc);
        ActiveFx a = new ActiveFx(type, preview ? settings : settings.copy(), preview);
        ACTIVE.put(type, a);
        a.playSound(mc);
    }

    public static void stop(FxType type) {
        ActiveFx a = ACTIVE.get(type);
        if (a != null && !a.stopping) {
            a.stopping = true;
            a.stopAge = a.age;
        }
    }

    public static void stopAll() {
        for (FxType t : FxType.values()) stop(t);
    }

    public static void clear(FxType type) {
        ActiveFx a = ACTIVE.remove(type);
        if (a != null) a.cleanup(Minecraft.getInstance());
    }

    public static void clearAll() {
        Minecraft mc = Minecraft.getInstance();
        for (ActiveFx a : ACTIVE.values()) a.cleanup(mc);
        ACTIVE.clear();
    }

    public static void clearPreviews() {
        Minecraft mc = Minecraft.getInstance();
        Iterator<ActiveFx> it = ACTIVE.values().iterator();
        while (it.hasNext()) {
            ActiveFx a = it.next();
            if (a.preview) {
                a.cleanup(mc);
                it.remove();
            }
        }
    }

    public static boolean isPreview(FxType type) {
        ActiveFx a = ACTIVE.get(type);
        return a != null && a.preview;
    }

    public static void tick() {
        if (ACTIVE.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) return;
        Iterator<ActiveFx> it = ACTIVE.values().iterator();
        while (it.hasNext()) {
            ActiveFx a = it.next();
            a.tick(mc);
            if (a.isFinished()) {
                a.cleanup(mc);
                it.remove();
            }
        }
    }
}
