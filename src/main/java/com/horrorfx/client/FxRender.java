package com.horrorfx.client;

import com.horrorfx.fx.FxSettings;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.Random;

/** Drawing code of every 2D effect. w/h = scaled gui size, s = fade level 0..1. */
public final class FxRender {
    private FxRender() {}

    // ------------------------------------------------------------------ helpers
    static int argb(int rgb, float a) {
        return (Mth.clamp((int) (a * 255f + 0.5f), 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    static float smooth(float x) {
        x = Mth.clamp(x, 0f, 1f);
        return x * x * (3f - 2f * x);
    }

    private static void vtx(BufferBuilder bb, Matrix4f m, float x, float y, float r, float g, float b, float a) {
        bb.vertex(m, x, y, 0f).color(r, g, b, a).endVertex();
    }

    private static float cr(int rgb) { return ((rgb >> 16) & 255) / 255f; }
    private static float cg(int rgb) { return ((rgb >> 8) & 255) / 255f; }
    private static float cb(int rgb) { return (rgb & 255) / 255f; }

    /** Additive (light adding) rectangle. */
    static void addRect(GuiGraphics g, int x1, int y1, int x2, int y2, int rgb, float a) {
        if (a <= 0.002f) return;
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f m = g.pose().last().pose();
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float r = cr(rgb), gg = cg(rgb), b = cb(rgb);
        vtx(bb, m, x1, y2, r, gg, b, a);
        vtx(bb, m, x2, y2, r, gg, b, a);
        vtx(bb, m, x2, y1, r, gg, b, a);
        vtx(bb, m, x1, y1, r, gg, b, a);
        BufferUploader.drawWithShader(bb.end());
        RenderSystem.defaultBlendFunc();
    }

    /**
     * Elliptical gradient: transparent until radius "inner", reaches maxA at radius "full"
     * (radius 1.0 = screen edge at the middle of each side, ~1.41 = corners).
     */
    static void radial(GuiGraphics g, int w, int h, int rgb, float maxA, float inner, float full, boolean additive) {
        if (maxA <= 0.002f) return;
        inner = Math.max(0f, inner);
        full = Math.max(inner + 0.01f, full);
        final float outerK = 1.5f;
        final int rings = 14, segs = 48;
        float r = cr(rgb), gg = cg(rgb), b = cb(rgb);
        float cx = w / 2f, cy = h / 2f;
        Matrix4f m = g.pose().last().pose();
        if (additive) RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < rings; i++) {
            float k0 = inner + (outerK - inner) * i / rings;
            float k1 = inner + (outerK - inner) * (i + 1) / rings;
            float a0 = maxA * smooth((k0 - inner) / (full - inner));
            float a1 = maxA * smooth((k1 - inner) / (full - inner));
            for (int j = 0; j < segs; j++) {
                double t0 = j * 2 * Math.PI / segs, t1 = (j + 1) * 2 * Math.PI / segs;
                float c0 = (float) Math.cos(t0), s0 = (float) Math.sin(t0);
                float c1 = (float) Math.cos(t1), s1 = (float) Math.sin(t1);
                vtx(bb, m, cx + c0 * k0 * cx, cy + s0 * k0 * cy, r, gg, b, a0);
                vtx(bb, m, cx + c0 * k1 * cx, cy + s0 * k1 * cy, r, gg, b, a1);
                vtx(bb, m, cx + c1 * k1 * cx, cy + s1 * k1 * cy, r, gg, b, a1);
                vtx(bb, m, cx + c1 * k0 * cx, cy + s1 * k0 * cy, r, gg, b, a0);
            }
        }
        BufferUploader.drawWithShader(bb.end());
        if (additive) RenderSystem.defaultBlendFunc();
    }

    private static void hline(GuiGraphics g, int x1, int x2, int y, int c) {
        g.fill(Math.min(x1, x2), y, Math.max(x1, x2), y + 1, c);
    }

    private static void vline(GuiGraphics g, int x, int y1, int y2, int c) {
        g.fill(x, Math.min(y1, y2), x + 1, Math.max(y1, y2), c);
    }

    // ------------------------------------------------------------------ DARKNESS / WHITEOUT
    /** Full-screen color fade. size > 0 leaves a clear spot in the middle (flashlight / fog from the edges). */
    static void fade(GuiGraphics g, int w, int h, ActiveFx a, float s) {
        FxSettings f = a.settings;
        float alpha = f.opacity * Math.min(1f, f.intensity * s);
        if (f.size <= 0.01f) {
            g.fill(0, 0, w, h, argb(f.color, alpha));
        } else {
            float inner = f.size * 0.9f;
            radial(g, w, h, f.color, alpha, inner, inner + 0.5f, false);
        }
    }

    // ------------------------------------------------------------------ BLOOD + HEARTBEAT
    private static float bump(float p, float c, float wd) {
        float d = (p - c) / wd;
        return (float) Math.exp(-d * d);
    }

    /** "lub-dub" heartbeat curve, phase 0..1 */
    private static float beatCurve(float p) {
        float lub = Math.max(bump(p, 0f, 0.08f), bump(p, 1f, 0.08f));
        float dub = 0.7f * bump(p, 0.28f, 0.09f);
        return Math.max(lub, dub);
    }

    static void blood(GuiGraphics g, int w, int h, ActiveFx a, float s, float pt) {
        FxSettings f = a.settings;
        int per = a.beatPeriod();
        float phase = ((a.age + pt) % per) / per;
        float beat = beatCurve(phase);
        float strength = f.intensity * s;
        float k = 0.55f + 0.45f * beat;
        float inner = Mth.lerp(Mth.clamp(strength * k, 0f, 1f), 0.95f, 0.95f - 0.8f * Math.max(0.1f, f.size));
        float edge = f.opacity * Math.min(1f, strength * 1.15f) * (0.7f + 0.3f * beat);
        g.fill(0, 0, w, h, argb(f.color, 0.10f * strength * beat * f.opacity));
        radial(g, w, h, f.color, edge, inner, inner + 0.55f, false);

        // blood drips from the top edge
        Random r = new Random(1337L);
        int n = 8 + (int) (14 * f.intensity);
        float t = a.age + pt;
        for (int i = 0; i < n; i++) {
            int x = r.nextInt(Math.max(1, w));
            int wd = 2 + r.nextInt(4);
            float max = h * (0.06f + r.nextFloat() * 0.32f) * (0.4f + 0.6f * f.intensity);
            float delay = r.nextFloat() * 60f;
            float gr = Mth.clamp((t - delay) / 120f, 0f, 1f);
            int len = (int) (max * gr * (2f - gr));
            if (len < 2) continue;
            int c1 = argb(f.color, 0.9f * s * f.opacity);
            int c2 = argb(f.color, 0.7f * s * f.opacity);
            g.fillGradient(x, 0, x + wd, len, c1, c2);
            g.fill(x - 1, len - 1, x + wd + 1, len + 2, c2);
        }
    }

    // ------------------------------------------------------------------ ROBOT EYES
    static void robot(GuiGraphics g, int w, int h, ActiveFx a, float s, float pt, Minecraft mc) {
        FxSettings f = a.settings;
        float op = f.opacity * f.intensity * s;
        float t = a.age + pt;
        int c = f.color;
        g.fill(0, 0, w, h, argb(c, 0.16f * op));

        int sc = argb(0x000000, 0.28f * op);
        for (int y = 0; y < h; y += 3) g.fill(0, y, w, y + 1, sc);

        int by = (int) ((t * f.speed * 2f) % (h + 40)) - 20;
        g.fillGradient(0, by - 12, w, by, argb(c, 0f), argb(c, 0.22f * op));
        g.fillGradient(0, by, w, by + 4, argb(c, 0.22f * op), argb(c, 0f));

        radial(g, w, h, 0x000000, 0.75f * op, 0.55f, 1.15f, false);

        int m = 14, L = 24;
        int line = argb(c, 0.9f * op);
        hline(g, m, m + L, m, line);                 vline(g, m, m, m + L, line);
        hline(g, w - m - L, w - m, m, line);         vline(g, w - m, m, m + L, line);
        hline(g, m, m + L, h - m, line);             vline(g, m, h - m - L, h - m, line);
        hline(g, w - m - L, w - m, h - m, line);     vline(g, w - m, h - m - L, h - m, line);

        int cx = w / 2, cy = h / 2;
        int rr = (int) (10 + 40 * f.size);
        hline(g, cx - rr, cx - rr / 2, cy, line);
        hline(g, cx + rr / 2, cx + rr, cy, line);
        vline(g, cx, cy - rr, cy - rr / 2, line);
        vline(g, cx, cy + rr / 2, cy + rr, line);

        if (op > 0.08f) {
            int tc = argb(c, Math.min(1f, 0.95f * op));
            g.drawString(mc.font, "OPTIC.SYS // ONLINE", m + 4, m + 6, tc, false);
            g.drawString(mc.font, "DIST " + (int) (100 + 60 * Math.sin(t * 0.07)) + "." + ((int) t % 10) + " M", m + 4, h - m - 14, tc, false);
            if (((int) (t / 10)) % 2 == 0) {
                g.drawString(mc.font, "TARGET SCAN", w - m - 4 - mc.font.width("TARGET SCAN"), m + 6, tc, false);
            }
        }
    }

    // ------------------------------------------------------------------ GLOW
    static void glow(GuiGraphics g, int w, int h, ActiveFx a, float s, float pt) {
        FxSettings f = a.settings;
        float t = a.age + pt;
        float breath = f.speed <= 0.01f ? 1f : 0.75f + 0.25f * (float) Math.sin(t * 0.06f * f.speed);
        float st = f.intensity * s * f.opacity * breath;
        addRect(g, 0, 0, w, h, f.color, 0.16f * st);
        radial(g, w, h, f.color, 0.6f * st, 0.2f + (1f - f.size) * 0.45f, 1.25f, true);
    }

    // ------------------------------------------------------------------ VIGNETTE
    static void vignette(GuiGraphics g, int w, int h, ActiveFx a, float s, float pt) {
        FxSettings f = a.settings;
        float breath = f.speed <= 0.01f ? 1f : 1f + 0.12f * (float) Math.sin((a.age + pt) * 0.08f * f.speed);
        float alpha = f.opacity * Math.min(1f, f.intensity * s * 1.15f);
        float inner = Math.max(0.05f, (0.9f - 0.7f * f.size)) * (2f - breath);
        radial(g, w, h, f.color, alpha, inner, inner + 0.6f, false);
    }

    // ------------------------------------------------------------------ GRAIN
    static void grain(GuiGraphics g, int w, int h, ActiveFx a, float s, float pt) {
        FxSettings f = a.settings;
        int step = (int) ((a.age + pt) * Math.max(0.1f, f.speed) * 0.5f);
        NoiseTexture.draw(g, w, h, 0.55f * f.intensity * f.opacity * s, 0.4f + f.size, step);
    }

    // ------------------------------------------------------------------ OLD FILM
    static void film(GuiGraphics g, int w, int h, ActiveFx a, float s, float pt) {
        FxSettings f = a.settings;
        float op = f.opacity * f.intensity * s;
        float t = a.age + pt;
        int step = (int) (t * Math.max(0.1f, f.speed) * 0.7f);
        Random r = new Random(step * 7919L + 13L);

        g.fill(0, 0, w, h, argb(f.color, 0.28f * op));

        float fl = r.nextFloat();
        if (fl < 0.35f) g.fill(0, 0, w, h, argb(0xFFFFFF, 0.06f * op * r.nextFloat()));
        else if (fl < 0.55f) g.fill(0, 0, w, h, argb(0x000000, 0.10f * op * r.nextFloat()));

        int scratches = 1 + (int) (4 * f.size);
        for (int i = 0; i < scratches; i++) {
            if (r.nextFloat() < 0.6f) {
                int x = r.nextInt(Math.max(1, w));
                int y0 = r.nextInt(Math.max(1, h / 2));
                int y1 = Math.min(h, y0 + h / 4 + r.nextInt(Math.max(1, h / 2)));
                g.fill(x, y0, x + 1, y1, argb(0xFFFFFF, (0.2f + 0.4f * r.nextFloat()) * op));
            }
        }
        int dust = 6 + (int) (20 * f.size);
        for (int i = 0; i < dust; i++) {
            int x = r.nextInt(Math.max(1, w));
            int y = r.nextInt(Math.max(1, h));
            int sz = 1 + r.nextInt(2);
            int col = r.nextBoolean() ? 0x000000 : 0xFFFFFF;
            g.fill(x, y, x + sz, y + sz, argb(col, (0.35f + 0.4f * r.nextFloat()) * op));
        }

        NoiseTexture.draw(g, w, h, 0.30f * op, 0.5f + f.size * 0.5f, step);
        radial(g, w, h, 0x000000, 0.6f * op, 0.45f, 1.2f, false);
    }

    // ------------------------------------------------------------------ FLICKER
    static void flicker(GuiGraphics g, int w, int h, ActiveFx a, float s, float pt) {
        FxSettings f = a.settings;
        int slot = (int) ((a.age + pt) * Math.max(0.1f, f.speed) * 1.5f);
        Random r = new Random(slot * 6151L + 7L);
        if (r.nextFloat() < f.intensity * 0.6f) {
            float al = f.opacity * s * (0.4f + 0.6f * r.nextFloat());
            g.fill(0, 0, w, h, argb(f.color, al));
        }
    }

    // ------------------------------------------------------------------ CINEMA BARS
    static void letterbox(GuiGraphics g, int w, int h, ActiveFx a, float s) {
        FxSettings f = a.settings;
        int bar = (int) (h * f.size * s);
        if (bar <= 0) return;
        int c = argb(f.color, f.opacity * f.intensity);
        g.fill(0, 0, w, bar, c);
        g.fill(0, h - bar, w, h, c);
    }

    // ------------------------------------------------------------------ TINT
    static void tint(GuiGraphics g, int w, int h, ActiveFx a, float s, float pt) {
        FxSettings f = a.settings;
        float t = a.age + pt;
        float pulse = f.speed <= 0.01f ? 1f : 0.7f + 0.3f * (float) Math.sin(t * 0.05f * f.speed);
        g.fill(0, 0, w, h, argb(f.color, f.opacity * f.intensity * s * pulse));
    }

    // ------------------------------------------------------------------ TV STATIC / GLITCH
    static void stat(GuiGraphics g, int w, int h, ActiveFx a, float s, float pt) {
        FxSettings f = a.settings;
        float st = f.intensity * s * f.opacity;
        int step = (int) ((a.age + pt) * Math.max(0.1f, f.speed) * 2f);
        NoiseTexture.draw(g, w, h, 0.6f * st, 0.9f, step);

        Random r = new Random(step * 104729L + 3L);
        int bands = 2 + (int) (8 * f.intensity);
        for (int i = 0; i < bands; i++) {
            if (r.nextFloat() < 0.5f + 0.5f * f.intensity) {
                int y = r.nextInt(Math.max(1, h));
                int bh = 1 + r.nextInt(Math.max(2, h / 12));
                int off = (r.nextInt(7) - 3) * 3;
                g.fill(0, y, w, y + bh, argb(0xFFFFFF, 0.12f * st));
                g.fill(Math.max(0, off), y, w, y + bh, argb(0x00FFFF, 0.10f * st));
                g.fill(0, y + 1, Math.max(1, w + off), y + bh + 1, argb(0xFF00FF, 0.10f * st));
            }
        }
        if (r.nextFloat() < 0.03f * f.intensity) g.fill(0, 0, w, h, argb(0xFFFFFF, 0.25f * st));
    }
}
