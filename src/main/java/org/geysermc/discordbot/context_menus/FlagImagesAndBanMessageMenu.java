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

package org.geysermc.discordbot.context_menus;

import com.jagrosh.jdautilities.command.MessageContextMenu;
import com.jagrosh.jdautilities.command.MessageContextMenuEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.internal.components.thumbnail.ThumbnailFileUpload;
import org.geysermc.discordbot.GeyserBot;
import org.geysermc.discordbot.storage.BadImage;
import org.geysermc.discordbot.util.BotColors;
import org.geysermc.discordbot.util.ModerationHelper;

public class FlagImagesAndBanMessageMenu extends MessageContextMenu {
    public FlagImagesAndBanMessageMenu() {
        this.name = "Mark Bad Images and Quick Ban";

        this.contexts = new InteractionContextType[] { InteractionContextType.GUILD };

        this.userPermissions = new Permission[] { Permission.MODERATE_MEMBERS };
        this.botPermissions = new Permission[] { Permission.MODERATE_MEMBERS };
    }

    @Override
    protected void execute(MessageContextMenuEvent event) {
        for (Message.Attachment attachment : event.getTarget().getAttachments()) {
            GeyserBot.storageManager.addBadImage(new BadImage(attachment));
        }

        MessageEmbed embed = ModerationHelper.banUser(
                event.getTarget().getMember(), event.getMember(),
                event.getGuild(), 1, 1, false,
                "Scammer or compromised account (Marked bad images)",
                null
        );

        String description = BotColors.FAILURE.getColor().equals(embed.getColor()) ?
                "The images are now flagged as bad images, but I was unable to ban the user." :
                "The images are now flagged as bad images and the user has been banned.";

        event.replyEmbeds(new EmbedBuilder()
                        .setTitle("Bad Images™ flagged")
                        .setDescription(description)
                        .setColor(BotColors.SUCCESS.getColor())
                        .build())
                .addFiles(event.getTarget().getAttachments().stream()
                        .map(a -> a.getProxy().downloadAsFileUpload(a.getFileName()))
                        .toList())
                .setEphemeral(true).queue();
    }
}
