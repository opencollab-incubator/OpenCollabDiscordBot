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

package org.geysermc.discordbot.http.pages.elements;

import org.geysermc.discordbot.http.html.Element;
import org.geysermc.discordbot.http.html.FAIcon;
import org.geysermc.discordbot.storage.TicketChatMessage;

import java.util.List;

/**
 * Renders one stored {@link TicketChatMessage} the way it would have looked in Discord: avatar,
 * author, timestamp, content and any attachments (rendered inline if they're images).
 */
public class TicketMessageBubble extends Element {
    private static final List<String> IMAGE_EXTENSIONS = List.of("png", "jpg", "jpeg", "gif", "webp", "bmp", "svg");
    private static final String DEFAULT_AVATAR = "https://cdn.discordapp.com/embed/avatars/1.png";

    public TicketMessageBubble(String guildId, int ticketId, TicketChatMessage message) {
        super("div");

        this.addClass("flex items-start gap-3 py-2");
        if (message.deleted()) this.addClass("opacity-50");

        String avatar = message.authorAvatarUrl() == null ? DEFAULT_AVATAR : message.authorAvatarUrl();
        this.addInnerElement(Element.img(avatar, "Avatar", "w-10 h-10 rounded-full flex-shrink-0 object-cover"));

        Element body = Element.div("flex min-w-0 flex-col grow");

        Element header = Element.of("div", "flex items-baseline gap-2 flex-wrap")
                .addInnerElement(Element.of("span", "font-semibold")
                        .addSafeInnerElement(message.authorName()))
                .addInnerElement(Element.staticTimeDisplay(message.sentAt())
                        .addClass("text-xs text-gray-500 dark:text-gray-400"));

        if (message.edited() && !message.deleted()) {
            header.addInnerElement(Element.of("span", "text-xs text-gray-500 dark:text-gray-400")
                    .addSafeInnerElement("(edited)"));
        }

        body.addInnerElement(header);

        String content = message.content().strip();

        if (message.deleted()) {
            body.addInnerElement(renderContent(
                    "mt-1 mb-0 text-sm italic line-through text-gray-500 dark:text-gray-400",
                    content.isEmpty() ? "Message deleted" : content
            ));
            body.addInnerElement(Element.of("span", "text-xs italic text-red-500 dark:text-red-400")
                    .addSafeInnerElement("Deleted"));
        } else if (!content.isEmpty()) {
            body.addInnerElement(renderContent("mt-1 mb-0 break-words text-sm", content));
        }

        if (!message.attachments().isEmpty()) {
            Element attachmentContainer = Element.div("flex flex-wrap gap-2 mt-2");

            for (String fileName : message.attachments()) {
                attachmentContainer.addInnerElement(renderAttachment(guildId, ticketId, fileName));
            }

            body.addInnerElement(attachmentContainer);
        }

        this.addInnerElement(body);
    }

    private static Element renderContent(String classes, String content) {
        Element paragraph = Element.of("p", classes);

        String[] lines = content.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) paragraph.addInnerElement(Element.br());
            paragraph.addSafeInnerElement(lines[i]);
        }

        return paragraph;
    }

    private static Element renderAttachment(String guildId, int ticketId, String fileName) {
        String url = "/%s/tickets/%d/attachments/%s".formatted(guildId, ticketId, fileName);
        String displayName = fileName.contains("_") ? fileName.substring(fileName.indexOf('_') + 1) : fileName;
        String extension = displayName.contains(".")
                ? displayName.substring(displayName.lastIndexOf('.') + 1).toLowerCase()
                : "";

        if (IMAGE_EXTENSIONS.contains(extension)) {
            return Element.a(url)
                    .addField("target", "_blank")
                    .addInnerElement(Element.img(url, displayName, "max-h-64 rounded-lg border border-gray-300 dark:border-gray-700"));
        }

        return Element.a(url)
                .addClass("flex items-center gap-2 rounded-lg bg-gray-200 dark:bg-gray-900 px-3 py-2 text-sm")
                .addField("download")
                .addInnerElement(new FAIcon("paperclip"))
                .addInnerElement(" ")
                .addSafeInnerElement(displayName);
    }
}
