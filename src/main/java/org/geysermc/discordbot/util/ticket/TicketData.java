/*
 * Copyright (c) 2026 Open Collaboration. https://opencollaboration.dev, GeyserMC. https://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author OpenCollab, GeyserMC
 * @link https://github.com/OpenCollaboration/OpenCollabDiscordBot
 */

package org.geysermc.discordbot.util.ticket;

import net.dv8tion.jda.api.entities.Role;
import org.geysermc.discordbot.GeyserBot;

public record TicketData(Role clientRole, String clientId, int id, TicketType type) {

    public TicketData(Role clientRole, String clientId, TicketType type) {
        this(clientRole, clientId, GeyserBot.storageManager.getAndIncrementTicketId(clientId), type);
    }

    public TicketData(Role clientRole, TicketType type) {
        this(clientRole, clientRole.getName().toLowerCase().replace(' ', '-'), type);
    }

    public String getChannelName() {
        String prefix = type.options().channelPrefix() == null ? "" : type.options().channelPrefix() + "-";
        return prefix + clientId + "-" + id;
    }

    public TicketMetadata metadata() {
        return new TicketMetadata(String.valueOf(id), clientId, clientRole.getName(), clientRole.getId(), type);
    }
}
