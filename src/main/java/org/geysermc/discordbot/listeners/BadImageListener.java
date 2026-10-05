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

package org.geysermc.discordbot.listeners;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.geysermc.discordbot.GeyserBot;
import org.geysermc.discordbot.storage.BadImage;
import org.geysermc.discordbot.util.ModerationHelper;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class BadImageListener extends ListenerAdapter {
    @Override
    public void onMessageReceived(@NonNull MessageReceivedEvent event) {
        if (event.isWebhookMessage() || !event.isFromGuild() || event.getAuthor().isBot()) return;

        List<BadImage> images = GeyserBot.storageManager.getBadImages();

        String reason = null;

        attachmentLoop: for (Message.Attachment attachment : event.getMessage().getAttachments()) {
            BadImage image = new BadImage(attachment);

            for (BadImage badImage : images) {
                double similarity = image.getSimilarity(badImage);
                if (similarity < 0.2) {
                    String percent = "%.2f".formatted((1 - similarity) * 100);
                    reason = "Bad Image™ detected! (" + percent + "% similar)";
                    break attachmentLoop;
                }
            }
        }

        if (reason != null) {
            ModerationHelper.quarantineMember(
                    event.getMember(), event.getGuild(),
                    reason, true,
                    event.getGuild().getSelfMember(),
                    event.getMessage(), true
            );
        }
    }
}
