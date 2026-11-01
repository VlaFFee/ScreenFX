package com.horrorfx.client;

import com.horrorfx.fx.FxSettings;
import com.horrorfx.fx.FxType;
import com.horrorfx.net.Net;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/** Control panel: pick an effect, tune it, preview it, send it to players, save presets to the world. */
public class FxScreen extends Screen {
    private static final int LABEL_W = 46;

    private final Map<FxType, FxSettings> edit = new EnumMap<>(FxType.class);
    private FxType cur = FxType.DARKNESS;
    private String target = "@a";
    private Button previewBtn;
    private int cx, top, colorY, soundY, targetY, lx, rx, cw = 150;

    public FxScreen(Map<FxType, FxSettings> fromServer) {
        super(Component.translatable("horrorfx.gui.title"));
        for (FxType t : FxType.values()) {
            FxSettings s = fromServer.get(t);
            edit.put(t, s == null ? FxSettings.defaults(t) : s.copy());
        }
    }

    private static Component comp(String key, Object arg) {
        return Component.translatable("horrorfx.gui." + key, arg);
    }

    private static String pct(double v) {
        return String.format(Locale.ROOT, "%d%%", Math.round(v * 100));
    }

    private void addSlider(int x, int y, double min, double max, double val, boolean integer,
                           DoubleFunction<Component> label, DoubleConsumer set, boolean enabled) {
        FxSlider sl = new FxSlider(x, y, cw, 20, min, max, val, integer, label, set);
        sl.active = enabled;
        addRenderableWidget(sl);
    }

    @Override
    protected void init() {
        FxSettings s = edit.get(cur);
        cx = width / 2;
        top = Math.max(4, (height - 246) / 2);
        lx = cx - 155;
        rx = cx + 5;
        int rh = 22;
        int y0 = top + 38;

        addRenderableWidget(Button.builder(Component.literal("<"), b -> cycle(-1)).bounds(lx, top + 12, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> cycle(1)).bounds(cx + 135, top + 12, 20, 20).build());

        // left column
        addSlider(lx, y0, 0, 1, s.intensity, false, v -> comp("intensity", pct(v)), v -> s.intensity = (float) v, true);
        addSlider(lx, y0 + rh, 0, 1, s.opacity, false, v -> comp("opacity", pct(v)), v -> s.opacity = (float) v, cur.uses('O'));
        addSlider(lx, y0 + 2 * rh, 0, 5, s.speed, false,
                v -> comp("speed", String.format(Locale.ROOT, "%.2f", v)), v -> s.speed = (float) v, cur.uses('S'));
        addSlider(lx, y0 + 3 * rh, 0, 1, s.size, false, v -> comp("size", pct(v)), v -> s.size = (float) v, cur.uses('Z'));
        addSlider(lx, y0 + 4 * rh, 0, 10, s.fadeIn / 20.0, false,
                v -> comp("fade_in", String.format(Locale.ROOT, "%.1f", v)), v -> s.fadeIn = (int) Math.round(v * 20), true);
        addSlider(lx, y0 + 5 * rh, 0, 10, s.fadeOut / 20.0, false,
                v -> comp("fade_out", String.format(Locale.ROOT, "%.1f", v)), v -> s.fadeOut = (int) Math.round(v * 20), true);

        // right column
        addSlider(rx, y0, 0, 120, s.duration < 0 ? 0 : Math.round(s.duration / 20.0), true,
                v -> comp("duration", v <= 0 ? "\u221E" : (long) v + " s"), v -> s.duration = v <= 0 ? -1 : (int) v * 20, true);
        addSlider(rx, y0 + rh, 0, 2, s.volume, false,
                v -> comp("volume", pct(v)), v -> s.volume = (float) v, true);
        addSlider(rx, y0 + 2 * rh, 0.5, 2, s.pitch, false,
                v -> comp("pitch", String.format(Locale.ROOT, "%.2f", v)), v -> s.pitch = (float) v, true);
        addSlider(rx, y0 + 3 * rh, -1, 200, s.soundInterval, true,
                v -> comp("sound_interval", v < 0 ? Component.translatable("horrorfx.gui.sound_sync")
                        : v == 0 ? Component.translatable("horrorfx.gui.sound_once")
                        : Component.translatable("horrorfx.gui.sound_every", (long) v)),
                v -> s.soundInterval = (int) v, true);

        colorY = y0 + 4 * rh;
        EditBox hex = new EditBox(font, rx + LABEL_W, colorY, cw - LABEL_W - 24, 20, Component.translatable("horrorfx.gui.color"));
        hex.setMaxLength(7);
        hex.setValue(String.format("#%06X", s.color));
        hex.setResponder(v -> {
            String t = v.startsWith("#") ? v.substring(1) : v;
            if (t.length() == 6) {
                try {
                    s.color = Integer.parseInt(t, 16);
                } catch (NumberFormatException ignored) {
                }
            }
        });
        hex.setEditable(cur.uses('C'));
        hex.active = cur.uses('C');
        hex.setTooltip(Tooltip.create(Component.translatable("horrorfx.gui.tip.color")));
        addRenderableWidget(hex);

        soundY = y0 + 5 * rh;
        EditBox sound = new EditBox(font, rx + LABEL_W, soundY, cw - LABEL_W, 20, Component.translatable("horrorfx.gui.sound"));
        sound.setMaxLength(256);
        sound.setValue(s.sound == null ? "" : s.sound);
        sound.setResponder(v -> s.sound = v.trim());
        sound.setTooltip(Tooltip.create(Component.translatable("horrorfx.gui.tip.sound")));
        addRenderableWidget(sound);

        // target + preview
        targetY = y0 + 6 * rh;
        EditBox tgt = new EditBox(font, lx + LABEL_W, targetY, cw - LABEL_W, 20, Component.translatable("horrorfx.gui.targets"));
        tgt.setMaxLength(120);
        tgt.setValue(target);
        tgt.setResponder(v -> target = v);
        tgt.setTooltip(Tooltip.create(Component.translatable("horrorfx.gui.tip.targets")));
        addRenderableWidget(tgt);

        previewBtn = Button.builder(previewLabel(), b -> togglePreview()).bounds(rx, targetY, cw, 20).build();
        addRenderableWidget(previewBtn);

        // action rows
        int ay = targetY + 26;
        addRenderableWidget(Button.builder(Component.translatable("horrorfx.gui.start"),
                b -> send(Net.START, Map.of(cur, s.copy()))).bounds(lx, ay, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("horrorfx.gui.stop"),
                b -> send(Net.STOP, Map.of(cur, s.copy()))).bounds(lx + 105, ay, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("horrorfx.gui.stop_all"),
                b -> send(Net.STOP_ALL, Map.of())).bounds(lx + 210, ay, 100, 20).build());

        int by = ay + 24;
        addRenderableWidget(Button.builder(Component.translatable("horrorfx.gui.save"),
                        b -> send(Net.SAVE, edit))
                .tooltip(Tooltip.create(Component.translatable("horrorfx.gui.tip.save")))
                .bounds(lx, by, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("horrorfx.gui.reset"), b -> {
            ClientFxManager.clearPreviews();
            edit.put(cur, FxSettings.defaults(cur));
            rebuildWidgets();
        }).bounds(rx, by, 76, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("horrorfx.gui.close"),
                b -> onClose()).bounds(rx + 80, by, 70, 20).build());
    }

    private Component previewLabel() {
        return Component.translatable(ClientFxManager.isPreview(cur) ? "horrorfx.gui.preview_off" : "horrorfx.gui.preview_on");
    }

    private void togglePreview() {
        if (ClientFxManager.isPreview(cur)) ClientFxManager.clear(cur);
        else ClientFxManager.start(cur, edit.get(cur), true);
        previewBtn.setMessage(previewLabel());
    }

    private void cycle(int dir) {
        ClientFxManager.clearPreviews();
        FxType[] v = FxType.values();
        cur = v[(cur.ordinal() + dir + v.length) % v.length];
        rebuildWidgets();
    }

    private void send(byte action, Map<FxType, FxSettings> entries) {
        String t = target == null || target.isBlank() ? "@a" : target.trim();
        Net.CH.sendToServer(new Net.ApplyPacket(action, t, entries));
    }

    @Override
    public void removed() {
        ClientFxManager.clearPreviews();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(cx - 162, top - 4, cx + 162, top + 246, 0xC0101010);
        g.drawCenteredString(font, title, cx, top + 2, 0xFFFFFF);
        g.drawCenteredString(font, Component.translatable(cur.langKey()).withStyle(ChatFormatting.YELLOW), cx, top + 18, 0xFFFFFF);

        g.drawString(font, Component.translatable("horrorfx.gui.color"), rx, colorY + 6, 0xCCCCCC, false);
        g.drawString(font, Component.translatable("horrorfx.gui.sound"), rx, soundY + 6, 0xCCCCCC, false);
        g.drawString(font, Component.translatable("horrorfx.gui.targets"), lx, targetY + 6, 0xCCCCCC, false);

        int sx = rx + cw - 20;
        g.fill(sx - 1, colorY - 1, sx + 21, colorY + 21, 0xFF888888);
        g.fill(sx, colorY, sx + 20, colorY + 20, 0xFF000000 | edit.get(cur).color);

        super.render(g, mx, my, pt);
    }
}
