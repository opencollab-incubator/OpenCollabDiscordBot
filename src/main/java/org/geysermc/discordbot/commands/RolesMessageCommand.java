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

package org.geysermc.discordbot.commands;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.modals.Modal;
import org.geysermc.discordbot.listeners.RolesMessageHandler;

public class RolesMessageCommand extends SlashCommand {
    public RolesMessageCommand() {
        this.name = "roles-message";
        this.help = "Sends a message which allows users to toggle roles";
        this.contexts = new InteractionContextType[]{InteractionContextType.GUILD};
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        event.replyModal(Modal.create(RolesMessageHandler.MODAL_ID, "Select Roles")
                .addComponents(
                        Label.of(
                                "Title", "The title of the embed sent.",
                                TextInput.of("title", TextInputStyle.SHORT)
                        ),
                        Label.of(
                                "Description", "The description of the embed sent.",
                                TextInput.of("description", TextInputStyle.PARAGRAPH)
                        ),
                        Label.of(
                                "Channel", "The channel to send the message in.",
                                EntitySelectMenu.create("channel-selection", EntitySelectMenu.SelectTarget.CHANNEL)
                                        .setChannelTypes(ChannelType.TEXT)
                                        .setMinValues(1).setMaxValues(1)
                                        .setRequired(true).build()
                        ),
                        Label.of(
                                "Roles", "Select the roles to present.",
                                EntitySelectMenu.create("role-selection", EntitySelectMenu.SelectTarget.ROLE)
                                        .setMinValues(1).setMaxValues(5)
                                        .setRequired(true).build()
                        )
                )
                .build()).queue();
    }
}
