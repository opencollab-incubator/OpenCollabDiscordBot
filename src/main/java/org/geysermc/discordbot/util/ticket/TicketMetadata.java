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

import java.time.Instant;

/**
 * Permanent, database-backed record of a ticket.
 * <p>
 * A ticket keeps existing in storage for as long as the guild does, even after its channel is
 * closed and deleted - {@code channelId} is {@code null} for tickets that no longer have one.
 *
 * @param ticketId The stable, globally unique database identifier for this ticket. Unlike
 *                  {@code channelId}, this never changes and remains valid after the ticket is closed.
 * @param guildId The Discord guild this ticket belongs to
 * @param channelId The Discord channel currently backing this ticket, or {@code null} if it no longer has one
 * @param createdAt When the ticket was created
 * @param id The per-client sequential ticket number, used to build the channel name
 * @param clientId The client identifier the ticket was filed under
 * @param clientName The name of the client role at the time the ticket was created
 * @param clientRoleId The Discord role ID of the client
 * @param type The ticket's type
 * @param state Whether the ticket is still open or has been closed
 */
public record TicketMetadata(
        int ticketId,
        long guildId,
        Long channelId,
        Instant createdAt,
        String id,
        String clientId,
        String clientName,
        String clientRoleId,
        TicketType type,
        TicketState state
) {
    public String getChannelName() {
        String prefix = type.options().channelPrefix() == null ? "" : type.options().channelPrefix() + "-";
        return prefix + clientId + "-" + id;
    }

    public TicketMetadata withTicketType(String ticketType) {
        return new TicketMetadata(ticketId, guildId, channelId, createdAt, id, clientId, clientName, clientRoleId, TicketType.getFromName(ticketType), state);
    }

    public TicketMetadata withTicketId(int ticketId) {
        return new TicketMetadata(ticketId, guildId, channelId, createdAt, id, clientId, clientName, clientRoleId, type, state);
    }

    public TicketMetadata withChannelId(Long channelId) {
        return new TicketMetadata(ticketId, guildId, channelId, createdAt, id, clientId, clientName, clientRoleId, type, state);
    }

    public TicketMetadata withState(TicketState state) {
        return new TicketMetadata(ticketId, guildId, channelId, createdAt, id, clientId, clientName, clientRoleId, type, state);
    }
}
