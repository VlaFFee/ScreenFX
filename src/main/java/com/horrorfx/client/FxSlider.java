package com.horrorfx.client;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

public class FxSlider extends AbstractSliderButton {
    private final double min, max;
    private final boolean integer;
    private final DoubleFunction<Component> label;
    private final DoubleConsumer setter;

    public FxSlider(int x, int y, int w, int h, double min, double max, double cur, boolean integer,
                    DoubleFunction<Component> label, DoubleConsumer setter) {
        super(x, y, w, h, Component.empty(), Mth.clamp((cur - min) / (max - min), 0.0, 1.0));
        this.min = min;
        this.max = max;
        this.integer = integer;
        this.label = label;
        this.setter = setter;
        updateMessage();
    }

    private double real() {
        double v = min + value * (max - min);
        return integer ? Math.round(v) : v;
    }

    @Override
    protected void updateMessage() {
        if (label == null) return;
        setMessage(label.apply(real()));
    }

    @Override
    protected void applyValue() {
        setter.accept(real());
    }
}
