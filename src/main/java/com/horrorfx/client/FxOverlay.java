package com.horrorfx.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** Draws all 2D effects above the whole HUD. Shake and blur are handled elsewhere. */
public class FxOverlay implements IGuiOverlay {
    @Override
    public void render(ForgeGui gui, GuiGraphics g, float pt, int w, int h) {
        if (ClientFxManager.ACTIVE.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        for (ActiveFx a : ClientFxManager.ACTIVE.values()) {
            float s = a.level(pt);
            if (s <= 0.001f) continue;
            switch (a.type) {
                case DARKNESS, WHITEOUT -> FxRender.fade(g, w, h, a, s);
                case BLOOD -> FxRender.blood(g, w, h, a, s, pt);
                case ROBOT -> FxRender.robot(g, w, h, a, s, pt, mc);
                case GLOW -> FxRender.glow(g, w, h, a, s, pt);
                case VIGNETTE -> FxRender.vignette(g, w, h, a, s, pt);
                case GRAIN -> FxRender.grain(g, w, h, a, s, pt);
                case FILM -> FxRender.film(g, w, h, a, s, pt);
                case FLICKER -> FxRender.flicker(g, w, h, a, s, pt);
                case LETTERBOX -> FxRender.letterbox(g, w, h, a, s);
                case TINT -> FxRender.tint(g, w, h, a, s, pt);
                case STATIC -> FxRender.stat(g, w, h, a, s, pt);
                default -> { }
            }
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
    }
}
