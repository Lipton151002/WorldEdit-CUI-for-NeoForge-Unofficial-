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

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Consumer;

public final class CUIChannel {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CHANNEL_VERSION = "1";
    private static final String HANDSHAKE = "v|4";
    private static final String WORLDEDIT_PAYLOAD_CLASS = "com.sk89q.worldedit.neoforge.net.handler.WECUIPacketHandler$CuiPacket";

    private static volatile Consumer<String> receiver;
    private static volatile boolean ownPayloadRegistered;
    private static volatile Class<?> foreignPayloadClass;
    private static volatile MethodHandle foreignPayloadText;
    private static volatile MethodHandle foreignPayloadConstructor;
    private static volatile boolean foreignLookupFailed;

    private CUIChannel() {
    }

    public static void setReceiver(final Consumer<String> newReceiver) {
        receiver = newReceiver;
    }

    public static void register(final RegisterPayloadHandlersEvent event) {
        try {
            event.registrar(CHANNEL_VERSION)
                    .optional()
                    .playBidirectional(CUIStringPayload.TYPE, CUIStringPayload.STREAM_CODEC, (payload, context) -> {
                    });
            ownPayloadRegistered = true;
        } catch (final RuntimeException ex) {
            ownPayloadRegistered = false;
            LOGGER.info("worldedit:cui is already registered by another mod, using its payload");
        }
    }

    public static void onClientboundPayload(final CustomPacketPayload payload) {
        if (!CUIStringPayload.ID.equals(payload.type().id())) {
            return;
        }
        final String message = extractMessage(payload);
        final Consumer<String> target = receiver;
        if (message == null || message.isEmpty() || target == null) {
            return;
        }
        Minecraft.getInstance().execute(() -> target.accept(message));
    }

    public static void sendHandshake(final ClientPacketListener connection) {
        if (connection == null || !connection.hasChannel(CUIStringPayload.TYPE)) {
            return;
        }
        final CustomPacketPayload payload = createPayload(HANDSHAKE);
        if (payload != null) {
            PacketDistributor.sendToServer(payload);
        }
    }

    private static String extractMessage(final CustomPacketPayload payload) {
        if (payload instanceof CUIStringPayload own) {
            return own.message();
        }
        final MethodHandle text = resolveForeignText(payload.getClass());
        if (text == null) {
            return null;
        }
        try {
            return (String) text.invoke(payload);
        } catch (final Throwable ex) {
            LOGGER.debug("Failed to read worldedit:cui payload", ex);
            return null;
        }
    }

    private static CustomPacketPayload createPayload(final String message) {
        if (ownPayloadRegistered) {
            return new CUIStringPayload(message);
        }
        final MethodHandle constructor = resolveForeignConstructor();
        if (constructor == null) {
            return null;
        }
        try {
            return (CustomPacketPayload) constructor.invoke(message);
        } catch (final Throwable ex) {
            LOGGER.debug("Failed to create worldedit:cui payload", ex);
            return null;
        }
    }

    private static MethodHandle resolveForeignText(final Class<?> payloadClass) {
        if (payloadClass == foreignPayloadClass) {
            return foreignPayloadText;
        }
        try {
            final MethodHandle handle = MethodHandles.publicLookup()
                    .findVirtual(payloadClass, "text", MethodType.methodType(String.class));
            foreignPayloadText = handle;
            foreignPayloadClass = payloadClass;
            return handle;
        } catch (final ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }

    private static MethodHandle resolveForeignConstructor() {
        if (foreignPayloadConstructor != null || foreignLookupFailed) {
            return foreignPayloadConstructor;
        }
        try {
            final Class<?> payloadClass = Class.forName(WORLDEDIT_PAYLOAD_CLASS, true, CUIChannel.class.getClassLoader());
            foreignPayloadConstructor = MethodHandles.publicLookup()
                    .findConstructor(payloadClass, MethodType.methodType(void.class, String.class));
        } catch (final ReflectiveOperationException | RuntimeException ex) {
            foreignLookupFailed = true;
            LOGGER.warn("worldedit:cui is registered by an unknown mod, WorldEditCUI cannot send its handshake");
        }
        return foreignPayloadConstructor;
    }
}
