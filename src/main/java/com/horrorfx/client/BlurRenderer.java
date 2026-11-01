package com.horrorfx.client;

import com.horrorfx.HorrorFX;
import com.horrorfx.fx.FxType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Blur using the vanilla "blur" shader program; 5 strength levels are separate post-chain json files. */
public final class BlurRenderer {
    private static final Logger LOGGER = LogManager.getLogger("horrorfx");
    private static final int LEVELS = 5;
    private static final PostChain[] chains = new PostChain[LEVELS + 1];
    private static int cw = -1, ch = -1;
    private static boolean broken;

    private BlurRenderer() {}

    public static void render(float pt) {
        ActiveFx a = ClientFxManager.ACTIVE.get(FxType.BLUR);
        if (a == null) {
            closeAll();
            return;
        }
        if (broken) return;
        int level = Mth.clamp((int) Math.ceil(a.settings.intensity * a.level(pt) * LEVELS - 0.001f), 0, LEVELS);
        if (level <= 0) return;

        Minecraft mc = Minecraft.getInstance();
        try {
            int w = mc.getWindow().getWidth();
            int h = mc.getWindow().getHeight();
            if (w != cw || h != ch) {
                for (PostChain c : chains) if (c != null) c.resize(w, h);
                cw = w;
                ch = h;
            }
            PostChain chain = chains[level];
            if (chain == null) {
                chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(),
                        new ResourceLocation(HorrorFX.MODID, "shaders/post/blur_" + level + ".json"));
                chain.resize(w, h);
                chains[level] = chain;
            }
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.resetTextureMatrix();
            chain.process(pt);
            mc.getMainRenderTarget().bindWrite(false);
            RenderSystem.enableDepthTest();
        } catch (Exception e) {
            LOGGER.error("Blur effect failed, disabling it", e);
            broken = true;
            closeAll();
        }
    }

    public static void closeAll() {
        for (int i = 0; i < chains.length; i++) {
            if (chains[i] != null) {
                chains[i].close();
                chains[i] = null;
            }
        }
        cw = ch = -1;
    }
}
