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

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;

public record CUIStringPayload(String message) implements CustomPacketPayload {
    public static final ResourceLocation ID = new ResourceLocation("worldedit", "cui");

    public static CUIStringPayload read(final FriendlyByteBuf buffer) {
        final int readableBytes = buffer.readableBytes();
        final String message = buffer.toString(buffer.readerIndex(), readableBytes, StandardCharsets.UTF_8);
        buffer.skipBytes(readableBytes);
        return new CUIStringPayload(message);
    }

    @Override
    public void write(final FriendlyByteBuf buffer) {
        buffer.writeBytes(this.message.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }
}
