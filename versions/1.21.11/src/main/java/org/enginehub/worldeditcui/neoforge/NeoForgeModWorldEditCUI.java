/*
 * Copyright (c) 2011-2024 WorldEditCUI team and contributors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.enginehub.worldeditcui.neoforge;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import org.enginehub.worldeditcui.WorldEditCUI;
import org.enginehub.worldeditcui.config.CUIConfiguration;
import org.enginehub.worldeditcui.event.listeners.CUIListenerChannel;
import org.enginehub.worldeditcui.event.listeners.CUIListenerWorldRender;
import org.enginehub.worldeditcui.gui.CUIConfigPanel;
import org.enginehub.worldeditcui.protocol.CUIPacket;
import org.enginehub.worldeditcui.protocol.CUIPacketHandler;
import org.enginehub.worldeditcui.render.OptifinePipelineProvider;
import org.enginehub.worldeditcui.render.PipelineProvider;
import org.enginehub.worldeditcui.render.VanillaPipelineProvider;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@Mod(value = NeoForgeModWorldEditCUI.MOD_ID, dist = Dist.CLIENT)
public final class NeoForgeModWorldEditCUI {
    public static final String MOD_ID = "worldeditcui";
    private static final int DELAYED_HELO_TICKS = 10;

    private static final KeyMapping.Category KEYBIND_CATEGORY_WECUI
            = new KeyMapping.Category(Identifier.fromNamespaceAndPath(MOD_ID, "general"));

    private static final List<PipelineProvider> RENDER_PIPELINES = List.of(
            new OptifinePipelineProvider(),
            new VanillaPipelineProvider()
    );

    private static NeoForgeModWorldEditCUI instance;

    private final KeyMapping keyBindToggleUI = key("toggle");
    private final KeyMapping keyBindClearSel = key("clear");
    private final KeyMapping keyBindChunkBorder = key("chunk");

    private WorldEditCUI controller;
    private CUIListenerWorldRender worldRenderListener;
    private CUIListenerChannel channelListener;

    private Level lastWorld;
    private LocalPlayer lastPlayer;

    private boolean visible = true;
    private int delayedHelo = 0;

    public NeoForgeModWorldEditCUI(final IEventBus modBus, final ModContainer container) {
        instance = this;

        modBus.addListener(this::onRegisterKeyMappings);
        modBus.addListener(this::onClientSetup);

        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onLoggingIn);
        NeoForge.EVENT_BUS.addListener(this::onRenderAfterMainPass);

        CUIPacketHandler.instance().registerClientboundHandler(this::onPluginMessage);

        container.registerExtensionPoint(IConfigScreenFactory.class,
                (modContainer, parent) -> new CUIConfigPanel(parent, this.controller.getConfiguration()));
    }

    private static KeyMapping key(final String name) {
        return new KeyMapping("key." + MOD_ID + '.' + name, GLFW.GLFW_KEY_UNKNOWN, KEYBIND_CATEGORY_WECUI);
    }

    private void onRegisterKeyMappings(final RegisterKeyMappingsEvent event) {
        event.registerCategory(KEYBIND_CATEGORY_WECUI);
        event.register(this.keyBindToggleUI);
        event.register(this.keyBindClearSel);
        event.register(this.keyBindChunkBorder);
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            final Minecraft client = Minecraft.getInstance();
            final WorldEditCUI newController = new WorldEditCUI();
            newController.initialise(client);
            this.worldRenderListener = new CUIListenerWorldRender(newController, client, RENDER_PIPELINES);
            this.channelListener = new CUIListenerChannel(newController);
            this.controller = newController;
        });
    }

    private void onClientTick(final ClientTickEvent.Post event) {
        if (this.controller == null) {
            return;
        }

        final Minecraft mc = Minecraft.getInstance();
        final CUIConfiguration config = this.controller.getConfiguration();
        final boolean inGame = mc.player != null;
        final boolean clock = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false) > 0;

        if (inGame && mc.screen == null) {
            while (this.keyBindToggleUI.consumeClick()) {
                this.visible = !this.visible;
            }

            while (this.keyBindClearSel.consumeClick()) {
                if (mc.player != null) {
                    mc.player.connection.sendUnattendedCommand("/sel", null);
                }

                if (config.isClearAllOnKey()) {
                    this.controller.clearRegions();
                }
            }

            while (this.keyBindChunkBorder.consumeClick()) {
                this.controller.toggleChunkBorders();
            }
        }

        if (inGame && clock) {
            if (mc.level != this.lastWorld || mc.player != this.lastPlayer) {
                this.lastWorld = mc.level;
                this.lastPlayer = mc.player;

                this.controller.getDebugger().debug("World change detected, sending new handshake");
                this.controller.clear();
                this.helo(mc.getConnection());
                this.delayedHelo = DELAYED_HELO_TICKS;
                if (mc.player != null && config.isPromiscuous()) {
                    mc.player.connection.sendUnattendedCommand("we cui", null);
                }
            }

            if (this.delayedHelo > 0) {
                this.delayedHelo--;
                if (this.delayedHelo == 0) {
                    this.helo(mc.getConnection());
                }
            }
        }
    }

    private void onPluginMessage(final CUIPacket payload, final CUIPacketHandler.PacketContext ctx) {
        try {
            ctx.workExecutor().execute(() -> {
                if (this.channelListener != null) {
                    this.channelListener.onMessage(payload);
                }
            });
        } catch (final Exception ex) {
            if (this.controller != null) {
                this.controller.getDebugger().info("Error decoding payload from server", ex);
            }
        }
    }

    private void onLoggingIn(final ClientPlayerNetworkEvent.LoggingIn event) {
        if (this.controller == null) {
            return;
        }
        this.visible = true;
        this.controller.getDebugger().debug("Joined game, sending initial handshake");
        this.helo(Minecraft.getInstance().getConnection());
    }

    private void onRenderAfterMainPass(final RenderLevelStageEvent.AfterTripwireBlocks event) {
        if (!this.visible || this.worldRenderListener == null) {
            return;
        }

        final float partialTicks = Minecraft.getInstance().getDeltaTracker().getRealtimeDeltaTicks();
        RenderSystem.getModelViewStack().pushMatrix();
        try {
            this.worldRenderListener.onRender(partialTicks);
        } finally {
            RenderSystem.getModelViewStack().popMatrix();
        }
    }

    private void helo(final ClientPacketListener connection) {
        if (connection == null || !connection.hasChannel(CUIPacket.TYPE)) {
            return;
        }
        ClientPacketDistributor.sendToServer(new CUIPacket("v", CUIPacket.protocolVersion()));
    }

    public WorldEditCUI getController() {
        return this.controller;
    }

    public static NeoForgeModWorldEditCUI getInstance() {
        return instance;
    }
}
