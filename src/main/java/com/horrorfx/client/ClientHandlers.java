package com.horrorfx.client;

import com.horrorfx.net.Net;
import net.minecraft.client.Minecraft;

/** Client-side packet handling. Only loaded on the client (via DistExecutor). */
public final class ClientHandlers {
    private ClientHandlers() {}

    public static void play(Net.PlayPacket m) {
        switch (m.action) {
            case Net.START -> {
                if (m.type != null && m.settings != null) ClientFxManager.start(m.type, m.settings, false);
            }
            case Net.STOP -> {
                if (m.type != null) ClientFxManager.stop(m.type);
            }
            case Net.STOP_ALL -> ClientFxManager.stopAll();
            case Net.CLEAR -> ClientFxManager.clearAll();
            default -> { }
        }
    }

    public static void openGui(Net.OpenGuiPacket m) {
        Minecraft.getInstance().setScreen(new FxScreen(m.map));
    }
}
