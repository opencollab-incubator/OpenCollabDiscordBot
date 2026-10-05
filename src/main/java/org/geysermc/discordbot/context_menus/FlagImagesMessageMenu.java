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
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import org.geysermc.discordbot.GeyserBot;
import org.geysermc.discordbot.storage.BadImage;

public class FlagImagesMessageMenu extends MessageContextMenu {
    public FlagImagesMessageMenu() {
        this.name = "Mark Bad Images";

        this.contexts = new InteractionContextType[] { InteractionContextType.GUILD };

        //this.userPermissions = new Permission[] { Permission.MODERATE_MEMBERS };
        //this.botPermissions = new Permission[] { Permission.MODERATE_MEMBERS };
    }

    @Override
    protected void execute(MessageContextMenuEvent event) {
        for (Message.Attachment attachment : event.getTarget().getAttachments()) {
            GeyserBot.storageManager.addBadImage(new BadImage(attachment));
        }

        event.reply("Flagged Bad Images™.").setEphemeral(true).queue();
    }
}
