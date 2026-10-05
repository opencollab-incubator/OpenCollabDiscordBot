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

package org.geysermc.discordbot.http.pages.tickets;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import org.geysermc.discordbot.GeyserBot;
import org.geysermc.discordbot.http.html.Base;
import org.geysermc.discordbot.http.html.Element;
import org.geysermc.discordbot.http.html.FAIcon;
import org.geysermc.discordbot.http.pages.elements.CloseTicketButton;
import org.geysermc.discordbot.http.pages.elements.Tabs;
import org.geysermc.discordbot.http.pages.elements.TicketMessageBubble;
import org.geysermc.discordbot.storage.TicketChatMessage;
import org.geysermc.discordbot.storage.TicketLogEntry;
import org.geysermc.discordbot.util.ticket.TicketMetadata;
import org.geysermc.discordbot.util.ticket.TicketState;

import java.util.List;

public class ViewTicketPage extends Base {
    private final Guild guild;
    private final TicketMetadata metadata;
    private final boolean canManage;

    public ViewTicketPage(User user, Guild guild, TicketMetadata metadata, boolean canManage) {
        super(user);
        this.guild = guild;
        this.metadata = metadata;
        this.canManage = canManage;
    }

    @Override
    public void addContent(Element container) {
        container.addInnerElement(Element.a("/" + guild.getId() + "/tickets")
                .addClass("text-sm text-gray-700 dark:text-gray-300 mb-4 inline-block")
                .addInnerElement(new FAIcon("arrow-left"))
                .addInnerElement(" Back to tickets"));

        container.addInnerElement(Element.h2().addInnerElement(metadata.getChannelName()));

        boolean open = metadata.state() == TicketState.OPEN;
        String badgeClasses = open
                ? "bg-green-200 dark:bg-green-900 border-2 border-green-300 dark:border-green-800"
                : "bg-gray-200 dark:bg-gray-900 border-2 border-gray-300 dark:border-gray-700";

        Element details = Element.div("flex flex-col overflow-hidden rounded-xl shadow-sm mb-6 p-4")
                .addClass(badgeClasses)
                .addInnerElement(
                        Element.of("h3", "mb-2 text-lg font-semibold")
                                .addInnerElement(new FAIcon(open ? "envelope-open-text" : "box-archive"))
                                .addInnerElement(" ")
                                .addSafeInnerElement(open ? "Open" : "Closed")
                )
                .addInnerElement(
                        Element.of("div", "text-sm leading-relaxed text-gray-700 dark:text-gray-300")
                                .addInnerElement(Element.of("p").addSafeInnerElement("Client: " + metadata.clientName()))
                                .addInnerElement(Element.of("p").addSafeInnerElement("Type: " + metadata.type().name()))
                                .addInnerElement(Element.of("p")
                                        .addInnerElement("Created ")
                                        .addInnerElement(Element.staticTimeDisplay(metadata.createdAt())))
                                .addInnerElement(metadata.channelId() == null
                                        ? Element.of("p").addSafeInnerElement("Channel has been removed.")
                                        : Element.of("p").addSafeInnerElement("Channel ID: " + metadata.channelId()))
                );

        if (open && canManage) {
            details.addInnerElement(Element.div("flex gap-2 pt-4")
                    .addInnerElement(new CloseTicketButton(guild.getId(), metadata.ticketId())));
        }

        container.addInnerElement(details);

        Element detailsTab = Element.of("div");
        detailsTab.addInnerElement(Element.h3().addInnerElement("Transcript"));

        List<TicketLogEntry> log = GeyserBot.storageManager.getTicketLog(metadata.ticketId());

        if (log.isEmpty()) {
            detailsTab.addInnerElement(Element.p().addInnerElement("Nothing has been logged for this ticket yet."));
        } else {
            Element transcript = Element.div("flex flex-col gap-2 rounded-xl bg-gray-200 dark:bg-gray-900 p-4 font-mono text-sm overflow-x-auto");

            for (TicketLogEntry entry : log) {
                transcript.addInnerElement(renderLogLine(entry));
            }

            detailsTab.addInnerElement(transcript);
        }

        Element messagesTab = Element.of("div");

        List<TicketChatMessage> messages = GeyserBot.storageManager.getTicketMessages(metadata.ticketId());

        if (messages.isEmpty()) {
            messagesTab.addInnerElement(Element.p().addInnerElement("No messages have been sent in this ticket yet."));
        } else {
            Element chat = Element.div("flex flex-col divide-y divide-gray-300 dark:divide-gray-700 rounded-xl bg-gray-100 dark:bg-gray-950/40 border border-gray-300 dark:border-gray-700 p-4");

            for (TicketChatMessage message : messages) {
                chat.addInnerElement(new TicketMessageBubble(guild.getId(), metadata.ticketId(), message));
            }

            messagesTab.addInnerElement(chat);
        }

        container.addInnerElement(new Tabs("ticket-tabs", List.of(
                new Tabs.Tab("details", "Details", detailsTab),
                new Tabs.Tab("messages", "Messages", messagesTab)
        )));
    }

    private static Element renderLogLine(TicketLogEntry entry) {
        Element line = Element.of("p")
                .addInnerElement(Element.staticTimeDisplay(entry.time())
                        .addClass("text-gray-500 dark:text-gray-400 mr-2"));

        String[] parts = entry.line().split("\n");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) line.addInnerElement(Element.br());
            line.addSafeInnerElement(parts[i]);
        }

        return line;
    }
}
