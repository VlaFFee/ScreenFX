package com.horrorfx.client;

import com.horrorfx.fx.FxSettings;
import com.horrorfx.fx.FxType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;

/** One running effect on this client. */
public class ActiveFx {
    public final FxType type;
    public final FxSettings settings;
    public final boolean preview;
    public int age;
    public boolean stopping;
    public int stopAge;
    private SoundInstance lastSound;

    public ActiveFx(FxType type, FxSettings settings, boolean preview) {
        this.type = type;
        this.settings = settings;
        this.preview = preview;
    }

    /** 0..1 strength including fade in / fade out. */
    public float level(float pt) {
        float t = age + pt;
        float in = settings.fadeIn <= 0 ? 1f : Math.min(1f, t / settings.fadeIn);
        float out = 1f;
        if (!preview && settings.duration >= 0) {
            float remain = settings.duration - t;
            out = settings.fadeOut <= 0 ? (remain > 0 ? 1f : 0f) : Mth.clamp(remain / settings.fadeOut, 0f, 1f);
        }
        if (stopping) {
            float st = (age - stopAge) + pt;
            float o2 = settings.fadeOut <= 0 ? 0f : Mth.clamp(1f - st / settings.fadeOut, 0f, 1f);
            out = Math.min(out, o2);
        }
        float x = Mth.clamp(Math.min(in, out), 0f, 1f);
        return x * x * (3f - 2f * x);
    }

    public boolean isFinished() {
        if (stopping && (settings.fadeOut <= 0 || age - stopAge >= settings.fadeOut)) return true;
        return !preview && settings.duration >= 0 && age >= settings.duration;
    }

    /** Heartbeat period in ticks (also used for "sync" sound repeat). */
    public int beatPeriod() {
        return Math.max(4, Math.round(20f / Math.max(0.1f, settings.speed)));
    }

    private int soundPeriod() {
        return settings.soundInterval < 0 ? beatPeriod() : settings.soundInterval;
    }

    public void tick(Minecraft mc) {
        age++;
        if (stopping || settings.sound == null || settings.sound.isBlank()) return;
        int iv = soundPeriod();
        if (iv > 0 && age % iv == 0) playSound(mc);
    }

    public void playSound(Minecraft mc) {
        if (settings.sound == null || settings.sound.isBlank()) return;
        ResourceLocation rl = ResourceLocation.tryParse(settings.sound.trim());
        if (rl == null) return;
        SoundEvent ev = SoundEvent.createVariableRangeEvent(rl);
        SimpleSoundInstance inst = SimpleSoundInstance.forUI(ev, settings.pitch, settings.volume);
        mc.getSoundManager().play(inst);
        lastSound = inst;
    }

    public void cleanup(Minecraft mc) {
        if (lastSound != null) {
            mc.getSoundManager().stop(lastSound);
            lastSound = null;
        }
    }
}
