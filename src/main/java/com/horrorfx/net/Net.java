package com.horrorfx.net;

import com.horrorfx.HorrorFX;
import com.horrorfx.client.ClientHandlers;
import com.horrorfx.fx.FxSavedData;
import com.horrorfx.fx.FxSettings;
import com.horrorfx.fx.FxType;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/** Network channel + all packets. The mod must be installed on server AND on every client. */
public final class Net {
    private static final String VER = "1";
    public static final SimpleChannel CH = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(HorrorFX.MODID, "main"), () -> VER, VER::equals, VER::equals);

    // actions (used by both Play and Apply packets)
    public static final byte START = 0, STOP = 1, STOP_ALL = 2, CLEAR = 3, SAVE = 4;

    private Net() {}

    public static void register() {
        int i = 0;
        CH.registerMessage(i++, PlayPacket.class, PlayPacket::encode, PlayPacket::decode, PlayPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CH.registerMessage(i++, OpenGuiPacket.class, OpenGuiPacket::encode, OpenGuiPacket::decode, OpenGuiPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CH.registerMessage(i++, ApplyPacket.class, ApplyPacket::encode, ApplyPacket::decode, ApplyPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CH.registerMessage(i++, RequestGuiPacket.class, RequestGuiPacket::encode, RequestGuiPacket::decode, RequestGuiPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    /** Operators (permission level 2) and the owner of a single-player world. */
    public static boolean canUse(ServerPlayer p) {
        MinecraftServer s = p.getServer();
        return p.hasPermissions(2) || (s != null && s.isSingleplayerOwner(p.getGameProfile()));
    }

    public static void sendTo(ServerPlayer p, Object msg) {
        CH.send(PacketDistributor.PLAYER.with(() -> p), msg);
    }

    private static void writeMap(FriendlyByteBuf b, Map<FxType, FxSettings> m) {
        b.writeVarInt(m.size());
        for (Map.Entry<FxType, FxSettings> e : m.entrySet()) {
            b.writeByte(e.getKey().ordinal());
            e.getValue().write(b);
        }
    }

    private static Map<FxType, FxSettings> readMap(FriendlyByteBuf b) {
        int n = Math.min(b.readVarInt(), 64);
        Map<FxType, FxSettings> m = new EnumMap<>(FxType.class);
        for (int i = 0; i < n; i++) {
            FxType t = FxType.byOrdinal(b.readByte());
            FxSettings s = FxSettings.read(b);
            if (t != null) m.put(t, s);
        }
        return m;
    }

    // ------------------------------------------------------------------ server -> client: play / stop
    public static class PlayPacket {
        public final byte action;
        public final FxType type;
        public final FxSettings settings;

        public PlayPacket(byte action, FxType type, FxSettings settings) {
            this.action = action;
            this.type = type;
            this.settings = settings;
        }

        void encode(FriendlyByteBuf b) {
            b.writeByte(action);
            b.writeByte(type == null ? -1 : type.ordinal());
            b.writeBoolean(settings != null);
            if (settings != null) settings.write(b);
        }

        static PlayPacket decode(FriendlyByteBuf b) {
            byte a = b.readByte();
            FxType t = FxType.byOrdinal(b.readByte());
            FxSettings s = b.readBoolean() ? FxSettings.read(b) : null;
            return new PlayPacket(a, t, s);
        }

        static void handle(PlayPacket m, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandlers.play(m)));
            c.get().setPacketHandled(true);
        }
    }

    // ------------------------------------------------------------------ server -> client: open GUI with saved presets
    public static class OpenGuiPacket {
        public final Map<FxType, FxSettings> map;

        public OpenGuiPacket(Map<FxType, FxSettings> map) {
            this.map = map;
        }

        void encode(FriendlyByteBuf b) {
            writeMap(b, map);
        }

        static OpenGuiPacket decode(FriendlyByteBuf b) {
            return new OpenGuiPacket(readMap(b));
        }

        static void handle(OpenGuiPacket m, Supplier<NetworkEvent.Context> c) {
            c.get().enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandlers.openGui(m)));
            c.get().setPacketHandled(true);
        }
    }

    // ------------------------------------------------------------------ client -> server: GUI buttons
    public static class ApplyPacket {
        public final byte action;
        public final String target;
        public final Map<FxType, FxSettings> entries;

        public ApplyPacket(byte action, String target, Map<FxType, FxSettings> entries) {
            this.action = action;
            this.target = target;
            this.entries = entries;
        }

        void encode(FriendlyByteBuf b) {
            b.writeByte(action);
            b.writeUtf(target == null ? "@a" : target, 128);
            writeMap(b, entries);
        }

        static ApplyPacket decode(FriendlyByteBuf b) {
            byte a = b.readByte();
            String t = b.readUtf(128);
            return new ApplyPacket(a, t, readMap(b));
        }

        static void handle(ApplyPacket m, Supplier<NetworkEvent.Context> c) {
            NetworkEvent.Context ctx = c.get();
            ctx.enqueueWork(() -> {
                ServerPlayer p = ctx.getSender();
                if (p == null || !canUse(p) || p.getServer() == null) return;
                FxSavedData data = FxSavedData.get(p.getServer());

                if (m.action == SAVE) {
                    for (Map.Entry<FxType, FxSettings> e : m.entries.entrySet()) {
                        data.set(e.getKey(), e.getValue().copy().clamp());
                    }
                    p.sendSystemMessage(Component.translatable("horrorfx.msg.saved"));
                    return;
                }

                Collection<ServerPlayer> targets;
                try {
                    targets = EntityArgument.players().parse(new StringReader(m.target))
                            .findPlayers(p.createCommandSourceStack().withPermission(2));
                } catch (CommandSyntaxException ex) {
                    p.sendSystemMessage(Component.translatable("horrorfx.cmd.bad_target", m.target));
                    return;
                }

                for (ServerPlayer t : targets) {
                    switch (m.action) {
                        case START -> m.entries.forEach((type, s) -> sendTo(t, new PlayPacket(START, type, s.copy().clamp())));
                        case STOP -> m.entries.forEach((type, s) -> sendTo(t, new PlayPacket(STOP, type, null)));
                        case STOP_ALL -> sendTo(t, new PlayPacket(STOP_ALL, null, null));
                        case CLEAR -> sendTo(t, new PlayPacket(CLEAR, null, null));
                        default -> { }
                    }
                }
            });
            ctx.setPacketHandled(true);
        }
    }

    // ------------------------------------------------------------------ client -> server: "open GUI" key
    public static class RequestGuiPacket {
        void encode(FriendlyByteBuf b) {}

        static RequestGuiPacket decode(FriendlyByteBuf b) {
            return new RequestGuiPacket();
        }

        static void handle(RequestGuiPacket m, Supplier<NetworkEvent.Context> c) {
            NetworkEvent.Context ctx = c.get();
            ctx.enqueueWork(() -> {
                ServerPlayer p = ctx.getSender();
                if (p == null || !canUse(p) || p.getServer() == null) return;
                sendTo(p, new OpenGuiPacket(FxSavedData.get(p.getServer()).all()));
            });
            ctx.setPacketHandled(true);
        }
    }
}
