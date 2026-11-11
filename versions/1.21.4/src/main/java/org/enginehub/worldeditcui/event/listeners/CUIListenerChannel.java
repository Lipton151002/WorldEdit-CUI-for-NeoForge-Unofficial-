/*
 * Copyright (c) 2011-2024 WorldEditCUI team and contributors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.enginehub.worldeditcui.event.listeners;

import org.enginehub.worldeditcui.WorldEditCUI;
import org.enginehub.worldeditcui.event.CUIEventArgs;

import java.util.Arrays;
import java.util.List;

/**
 * Listener class for incoming plugin channel messages
 * 
 * @author lahwran
 * @author yetanotherx
 * @author Adam Mummery-Smith
 */
public class CUIListenerChannel
{
	private WorldEditCUI controller;
	
	public CUIListenerChannel(WorldEditCUI controller)
	{
		this.controller = controller;
	}
	
	public void onMessage(final String message)
	{
		this.controller.getDebugger().debug("Received CUI event from server: " + message);
		
		try
		{
			final String[] split = message.split("\\|", -1);
			final boolean multi = split[0].startsWith("+");
			final String type = split[0].substring(multi ? 1 : 0);
			final List<String> args = split.length > 1 ? Arrays.asList(split).subList(1, split.length) : List.of("");
			CUIEventArgs eventArgs = new CUIEventArgs(this.controller, multi, type, args);
			this.controller.getDispatcher().raiseEvent(eventArgs);
		}
		catch (Exception ex)
		{
		}
	}
}
