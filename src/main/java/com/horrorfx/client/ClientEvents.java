package com.horrorfx.client;

import com.horrorfx.HorrorFX;
import com.horrorfx.fx.FxSettings;
import com.horrorfx.fx.FxType;
import com.horrorfx.net.Net;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = HorrorFX.MODID, value = Dist.CLIENT)
public class ClientEvents {
    public static final KeyMapping OPEN_KEY = new KeyMapping("key.horrorfx.open",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.horrorfx");

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        ClientFxManager.tick();
        Minecraft mc = Minecraft.getInstance();
        while (OPEN_KEY.consumeClick()) {
            if (mc.player != null && mc.screen == null) {
                Net.CH.sendToServer(new Net.RequestGuiPacket());
            }
        }
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles e) {
        ActiveFx a = ClientFxManager.ACTIVE.get(FxType.SHAKE);
        if (a == null) return;
        float pt = (float) e.getPartialTick();
        float s = a.level(pt);
        if (s <= 0.001f) return;
        FxSettings f = a.settings;
        float t = (a.age + pt) * 0.9f * Math.max(0.1f, f.speed);
        float amp = f.intensity * s;
        e.setYaw(e.getYaw() + noise(t, 1.3f) * amp * 3.0f);
        e.setPitch(e.getPitch() + noise(t, 7.1f) * amp * 3.0f);
        e.setRoll(e.getRoll() + noise(t, 13.7f) * amp * 5.0f);
    }

    private static float noise(float t, float seed) {
        return (float) (Math.sin(t * 1.7 + seed) * 0.5 + Math.sin(t * 3.1 + seed * 2.3) * 0.3 + Math.sin(t * 5.9 + seed * 4.1) * 0.2);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent e) {
        if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            BlurRenderer.render((float) e.getPartialTick());
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        ClientFxManager.clearAll();
        BlurRenderer.closeAll();
    }
}
