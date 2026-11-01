package com.horrorfx.fx;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;

/** All adjustable parameters of one effect. Times are in ticks (20 ticks = 1 second). */
public class FxSettings {
    public float intensity = 1f;
    public float opacity = 1f;
    public float speed = 1f;
    public float size = 0.5f;
    public int color = 0x000000;
    public int fadeIn = 20;
    public int fadeOut = 20;
    /** total duration in ticks INCLUDING fades; -1 = until stopped */
    public int duration = -1;
    public String sound = "";
    public float volume = 1f;
    public float pitch = 1f;
    /** ticks between sound repeats; 0 = once; -1 = in sync with effect pulse */
    public int soundInterval = 0;

    public FxSettings copy() {
        FxSettings s = new FxSettings();
        s.intensity = intensity;
        s.opacity = opacity;
        s.speed = speed;
        s.size = size;
        s.color = color;
        s.fadeIn = fadeIn;
        s.fadeOut = fadeOut;
        s.duration = duration;
        s.sound = sound;
        s.volume = volume;
        s.pitch = pitch;
        s.soundInterval = soundInterval;
        return s;
    }

    public FxSettings clamp() {
        intensity = Mth.clamp(intensity, 0f, 1f);
        opacity = Mth.clamp(opacity, 0f, 1f);
        speed = Mth.clamp(speed, 0f, 10f);
        size = Mth.clamp(size, 0f, 1f);
        color &= 0xFFFFFF;
        fadeIn = Mth.clamp(fadeIn, 0, 12000);
        fadeOut = Mth.clamp(fadeOut, 0, 12000);
        if (duration < -1) duration = -1;
        if (duration > 720000) duration = 720000;
        volume = Mth.clamp(volume, 0f, 4f);
        pitch = Mth.clamp(pitch, 0.5f, 2f);
        soundInterval = Mth.clamp(soundInterval, -1, 720000);
        if (sound == null) sound = "";
        if (sound.length() > 256) sound = sound.substring(0, 256);
        return this;
    }

    public void write(FriendlyByteBuf b) {
        b.writeFloat(intensity);
        b.writeFloat(opacity);
        b.writeFloat(speed);
        b.writeFloat(size);
        b.writeInt(color);
        b.writeInt(fadeIn);
        b.writeInt(fadeOut);
        b.writeInt(duration);
        b.writeUtf(sound == null ? "" : (sound.length() > 256 ? sound.substring(0, 256) : sound), 256);
        b.writeFloat(volume);
        b.writeFloat(pitch);
        b.writeInt(soundInterval);
    }

    public static FxSettings read(FriendlyByteBuf b) {
        FxSettings s = new FxSettings();
        s.intensity = b.readFloat();
        s.opacity = b.readFloat();
        s.speed = b.readFloat();
        s.size = b.readFloat();
        s.color = b.readInt();
        s.fadeIn = b.readInt();
        s.fadeOut = b.readInt();
        s.duration = b.readInt();
        s.sound = b.readUtf(256);
        s.volume = b.readFloat();
        s.pitch = b.readFloat();
        s.soundInterval = b.readInt();
        return s.clamp();
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putFloat("intensity", intensity);
        t.putFloat("opacity", opacity);
        t.putFloat("speed", speed);
        t.putFloat("size", size);
        t.putInt("color", color);
        t.putInt("fadeIn", fadeIn);
        t.putInt("fadeOut", fadeOut);
        t.putInt("duration", duration);
        t.putString("sound", sound == null ? "" : sound);
        t.putFloat("volume", volume);
        t.putFloat("pitch", pitch);
        t.putInt("soundInterval", soundInterval);
        return t;
    }

    public static FxSettings load(CompoundTag t) {
        FxSettings s = new FxSettings();
        s.intensity = t.getFloat("intensity");
        s.opacity = t.getFloat("opacity");
        s.speed = t.getFloat("speed");
        s.size = t.getFloat("size");
        s.color = t.getInt("color");
        s.fadeIn = t.getInt("fadeIn");
        s.fadeOut = t.getInt("fadeOut");
        s.duration = t.getInt("duration");
        s.sound = t.getString("sound");
        s.volume = t.getFloat("volume");
        s.pitch = t.getFloat("pitch");
        s.soundInterval = t.getInt("soundInterval");
        return s.clamp();
    }

    /** Sensible starting values for every effect. */
    public static FxSettings defaults(FxType type) {
        FxSettings s = new FxSettings();
        switch (type) {
            case DARKNESS -> {
                s.color = 0x000000; s.intensity = 0.9f; s.size = 0f; s.fadeIn = 40; s.fadeOut = 40;
            }
            case BLOOD -> {
                s.color = 0x8A0303; s.intensity = 0.8f; s.speed = 1.2f; s.size = 0.5f; s.fadeIn = 30; s.fadeOut = 40;
                s.sound = "minecraft:entity.warden.heartbeat"; s.soundInterval = -1;
            }
            case ROBOT -> {
                s.color = 0xFF2020; s.intensity = 0.8f; s.speed = 1f; s.size = 0.5f; s.fadeIn = 20; s.fadeOut = 20;
            }
            case WHITEOUT -> {
                s.color = 0xFFFFFF; s.intensity = 0.85f; s.size = 0f; s.fadeIn = 40; s.fadeOut = 40;
            }
            case SHAKE -> {
                s.intensity = 0.6f; s.speed = 1f; s.fadeIn = 10; s.fadeOut = 20;
            }
            case BLUR -> {
                s.intensity = 0.6f; s.fadeIn = 30; s.fadeOut = 30;
            }
            case GLOW -> {
                s.color = 0xFFF2C0; s.intensity = 0.6f; s.speed = 0.5f; s.size = 0.5f; s.fadeIn = 30; s.fadeOut = 30;
            }
            case VIGNETTE -> {
                s.color = 0x000000; s.intensity = 0.8f; s.speed = 0f; s.size = 0.5f; s.fadeIn = 30; s.fadeOut = 30;
            }
            case GRAIN -> {
                s.intensity = 0.5f; s.speed = 1f; s.size = 0.5f; s.fadeIn = 20; s.fadeOut = 20;
            }
            case FILM -> {
                s.color = 0x5A4630; s.intensity = 0.7f; s.speed = 1f; s.size = 0.5f; s.fadeIn = 30; s.fadeOut = 30;
            }
            case FLICKER -> {
                s.color = 0x000000; s.intensity = 0.5f; s.speed = 1f; s.fadeIn = 10; s.fadeOut = 10;
            }
            case LETTERBOX -> {
                s.color = 0x000000; s.intensity = 1f; s.size = 0.12f; s.fadeIn = 30; s.fadeOut = 30;
            }
            case TINT -> {
                s.color = 0x6A0F2A; s.intensity = 0.5f; s.speed = 0f; s.fadeIn = 30; s.fadeOut = 30;
            }
            case STATIC -> {
                s.intensity = 0.6f; s.speed = 1f; s.fadeIn = 5; s.fadeOut = 10;
            }
        }
        return s;
    }
}
