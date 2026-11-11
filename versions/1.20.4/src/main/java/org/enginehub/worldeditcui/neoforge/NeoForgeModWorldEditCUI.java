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

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.ConfigScreenHandler;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import org.enginehub.worldeditcui.WorldEditCUI;
import org.enginehub.worldeditcui.config.CUIConfiguration;
import org.enginehub.worldeditcui.event.listeners.CUIListenerChannel;
import org.enginehub.worldeditcui.event.listeners.CUIListenerWorldRender;
import org.enginehub.worldeditcui.gui.CUIConfigPanel;
import org.enginehub.worldeditcui.render.OptifinePipelineProvider;
import org.enginehub.worldeditcui.render.PipelineProvider;
import org.enginehub.worldeditcui.render.VanillaPipelineProvider;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@Mod(NeoForgeModWorldEditCUI.MOD_ID)
public final class NeoForgeModWorldEditCUI {
    public static final String MOD_ID = "worldeditcui";
    private static final int DELAYED_HELO_TICKS = 10;
    private static final String KEYBIND_CATEGORY_WECUI = "key.categories.worldeditcui";

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

    public NeoForgeModWorldEditCUI(final IEventBus modBus) {
        instance = this;
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }

        modBus.addListener(this::onRegisterKeyMappings);
        modBus.addListener(this::onClientSetup);
        modBus.addListener(this::onRegisterPayloadHandlers);

        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onLoggingIn);
        NeoForge.EVENT_BUS.addListener(this::onRenderLevelStage);

        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> new CUIConfigPanel(parent, this.controller.getConfiguration())));
    }

    private static KeyMapping key(final String name) {
        return new KeyMapping("key." + MOD_ID + '.' + name, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, KEYBIND_CATEGORY_WECUI);
    }

    private void onRegisterKeyMappings(final RegisterKeyMappingsEvent event) {
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

    private void onClientTick(final TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || this.controller == null) {
            return;
        }

        final Minecraft mc = Minecraft.getInstance();
        final CUIConfiguration config = this.controller.getConfiguration();
        final boolean inGame = mc.player != null;
        final boolean clock = mc.getFrameTime() > 0;

        if (inGame && mc.screen == null) {
            while (this.keyBindToggleUI.consumeClick()) {
                this.visible = !this.visible;
            }

            while (this.keyBindClearSel.consumeClick()) {
                if (mc.player != null) {
                    mc.player.connection.sendUnsignedCommand("/sel");
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
                    mc.player.connection.sendUnsignedCommand("we cui");
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

    private void onRegisterPayloadHandlers(final RegisterPayloadHandlerEvent event) {
        event.registrar(CUIStringPayload.ID.getNamespace())
                .optional()
                .play(CUIStringPayload.ID, CUIStringPayload::read, handler -> handler.client(this::onPluginMessage));
    }

    private void onPluginMessage(final CUIStringPayload payload, final PlayPayloadContext context) {
        if (this.controller == null) {
            return;
        }
        if (payload.message().isEmpty()) {
            this.controller.getDebugger().debug("Warning, invalid (zero length) payload received from server");
            return;
        }
        context.workHandler().execute(() -> {
            if (this.channelListener != null) {
                this.channelListener.onMessage(payload.message());
            }
        });
    }

    private void onLoggingIn(final ClientPlayerNetworkEvent.LoggingIn event) {
        if (this.controller == null) {
            return;
        }
        this.visible = true;
        this.controller.getDebugger().debug("Joined game, sending initial handshake");
        this.helo(Minecraft.getInstance().getConnection());
    }

    private void onRenderLevelStage(final RenderLevelStageEvent event) {
        if (!this.visible || this.worldRenderListener == null) {
            return;
        }

        final Minecraft mc = Minecraft.getInstance();
        final RenderTarget translucentTarget = mc.levelRenderer.getTranslucentTarget();
        final RenderLevelStageEvent.Stage wantedStage = translucentTarget != null
                ? RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                : RenderLevelStageEvent.Stage.AFTER_LEVEL;
        if (event.getStage() != wantedStage) {
            return;
        }

        final PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        try {
            modelView.setIdentity();
            modelView.mulPoseMatrix(event.getPoseStack().last().pose());
            RenderSystem.applyModelViewMatrix();
            if (translucentTarget != null) {
                translucentTarget.bindWrite(false);
            }
            this.worldRenderListener.onRender(event.getPartialTick());
        } finally {
            if (translucentTarget != null) {
                mc.getMainRenderTarget().bindWrite(false);
            }
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
        }
    }

    private void helo(final ClientPacketListener connection) {
        if (connection == null) {
            return;
        }
        connection.send(new ServerboundCustomPayloadPacket(new CUIStringPayload("v|" + WorldEditCUI.PROTOCOL_VERSION)));
    }

    public WorldEditCUI getController() {
        return this.controller;
    }

    public static NeoForgeModWorldEditCUI getInstance() {
        return instance;
    }
}
