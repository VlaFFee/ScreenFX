package com.horrorfx.client;

import com.horrorfx.HorrorFX;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.Random;

/** Random-noise texture used for grain, film dust and TV static. */
public final class NoiseTexture {
    private static final int SIZE = 128;
    private static final Random RND = new Random();
    private static DynamicTexture tex;
    private static ResourceLocation loc;
    private static int lastStep = Integer.MIN_VALUE;

    private NoiseTexture() {}

    private static void ensure() {
        if (tex != null) return;
        tex = new DynamicTexture(SIZE, SIZE, false);
        loc = new ResourceLocation(HorrorFX.MODID, "dynamic/noise");
        Minecraft.getInstance().getTextureManager().register(loc, tex);
    }

    private static void update(int step) {
        ensure();
        if (step == lastStep) return;
        lastStep = step;
        NativeImage p = tex.getPixels();
        if (p == null) return;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int v = RND.nextInt(256);
                int a = 40 + RND.nextInt(216);
                p.setPixelRGBA(x, y, (a << 24) | (v << 16) | (v << 8) | v);
            }
        }
        tex.upload();
    }

    /** scale: size of one 128px noise tile relative to 128 gui units. step: changes -> new noise pattern. */
    public static void draw(GuiGraphics g, int w, int h, float alpha, float scale, int step) {
        if (alpha <= 0.003f) return;
        update(step);
        int tile = Math.max(16, (int) (SIZE * scale));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, Math.min(1f, alpha));
        Random r = new Random(step * 2654435761L);
        for (int y = 0; y < h; y += tile) {
            for (int x = 0; x < w; x += tile) {
                g.blit(loc, x, y, tile, tile, (float) r.nextInt(SIZE), (float) r.nextInt(SIZE), SIZE, SIZE, SIZE, SIZE);
            }
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }
}
