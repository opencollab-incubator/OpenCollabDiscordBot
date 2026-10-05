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

package org.geysermc.discordbot.storage;

import java.time.Instant;
import java.util.List;

/**
 * A single chat message belonging to a ticket, kept so the web panel can render a ticket's
 * conversation the way it looked in Discord.
 *
 * @param attachments The saved file names of any attachments, as they were written under
 *                     {@code tickets/<ticketId>/} - not the original (expiring) Discord CDN URLs
 */
public record TicketChatMessage(
        int ticketId,
        long messageId,
        long authorId,
        String authorName,
        String authorAvatarUrl,
        String content,
        List<String> attachments,
        Instant sentAt,
        boolean edited,
        boolean deleted
) {
    public TicketChatMessage withEdited() {
        return new TicketChatMessage(ticketId, messageId, authorId, authorName, authorAvatarUrl, content, attachments, sentAt, true, deleted);
    }

    public TicketChatMessage withDeleted() {
        return new TicketChatMessage(ticketId, messageId, authorId, authorName, authorAvatarUrl, content, attachments, sentAt, edited, true);
    }
}
