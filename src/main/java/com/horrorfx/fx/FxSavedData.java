package com.horrorfx.fx;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.EnumMap;
import java.util.Map;

/** Per-world storage of the effect presets (saved in world/data/horrorfx.dat, travels with the map). */
public class FxSavedData extends SavedData {
    private final Map<FxType, FxSettings> map = new EnumMap<>(FxType.class);

    public static FxSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FxSavedData::load, FxSavedData::new, "horrorfx");
    }

    public static FxSavedData load(CompoundTag tag) {
        FxSavedData d = new FxSavedData();
        for (FxType t : FxType.values()) {
            if (tag.contains(t.id, 10)) {
                d.map.put(t, FxSettings.load(tag.getCompound(t.id)));
            }
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        for (Map.Entry<FxType, FxSettings> e : map.entrySet()) {
            tag.put(e.getKey().id, e.getValue().save());
        }
        return tag;
    }

    public FxSettings get(FxType t) {
        return map.computeIfAbsent(t, FxSettings::defaults);
    }

    public void set(FxType t, FxSettings s) {
        map.put(t, s);
        setDirty();
    }

    public Map<FxType, FxSettings> all() {
        Map<FxType, FxSettings> m = new EnumMap<>(FxType.class);
        for (FxType t : FxType.values()) m.put(t, get(t).copy());
        return m;
    }
}
