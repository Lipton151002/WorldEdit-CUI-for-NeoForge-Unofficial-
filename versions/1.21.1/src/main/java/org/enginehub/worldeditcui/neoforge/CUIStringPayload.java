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

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;

public record CUIStringPayload(String message) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("worldedit", "cui");
    public static final Type<CUIStringPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<ByteBuf, CUIStringPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeBytes(payload.message().getBytes(StandardCharsets.UTF_8)),
            buffer -> {
                final int readableBytes = buffer.readableBytes();
                final String message = buffer.toString(buffer.readerIndex(), readableBytes, StandardCharsets.UTF_8);
                buffer.skipBytes(readableBytes);
                return new CUIStringPayload(message);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
