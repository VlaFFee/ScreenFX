package com.horrorfx;

import com.horrorfx.net.Net;
import net.minecraftforge.fml.common.Mod;

@Mod(HorrorFX.MODID)
public class HorrorFX {
    public static final String MODID = "horrorfx";

    public HorrorFX() {
        Net.register();
    }
}
