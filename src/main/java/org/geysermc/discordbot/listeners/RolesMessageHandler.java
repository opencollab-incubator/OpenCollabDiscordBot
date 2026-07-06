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

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.geysermc.discordbot.util.BotColors;
import org.geysermc.discordbot.util.ticket.TicketType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RolesMessageHandler extends ListenerAdapter {
    public static final String MODAL_ID = "roles-modal";
    public static final String BUTTON_PREFIX_ID = "role-button-";

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!event.getInteraction().getCustomId().startsWith(BUTTON_PREFIX_ID)) return;

        String roleId = event.getInteraction().getCustomId().substring(BUTTON_PREFIX_ID.length());

        Role role = event.getGuild().getRoleById(roleId);
        if (role == null) {
            event.replyEmbeds(new EmbedBuilder()
                    .setTitle("Error!")
                    .setDescription("Unable to find role. Please report this issue!")
                    .setColor(BotColors.FAILURE.getColor())
                    .build()).setEphemeral(true).queue();
            return;
        }

        if (event.getMember().getUnsortedRoles().contains(role)) {
            event.getGuild().removeRoleFromMember(event.getMember(), role).queue(v -> {
                event.replyEmbeds(new EmbedBuilder()
                        .setDescription("Removed role.")
                        .setColor(BotColors.FAILURE.getColor())
                        .build()).setEphemeral(true).queue();
            }, throwable -> {
                event.replyEmbeds(new EmbedBuilder()
                        .setDescription("Failed to remove role. Try again later, if the issue persists, please report the issue.")
                        .setColor(BotColors.FAILURE.getColor())
                        .build()).setEphemeral(true).queue();
            });
        } else {
            event.getGuild().addRoleToMember(event.getMember(), role).queue(v -> {
                event.replyEmbeds(new EmbedBuilder()
                        .setDescription("Added role.")
                        .setColor(BotColors.SUCCESS.getColor())
                        .build()).setEphemeral(true).queue();
            }, throwable -> {
                event.replyEmbeds(new EmbedBuilder()
                        .setDescription("Failed to add role. Try again later, if the issue persists, please report the issue.")
                        .setColor(BotColors.FAILURE.getColor())
                        .build()).setEphemeral(true).queue();
            });
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        String title = event.getInteraction().getValue("title").getAsString();

        String description = event.getInteraction().getValue("description").getAsString();

        TextChannel channel = (TextChannel) event.getInteraction().getValue("channel-selection").getAsMentions().getChannels().getFirst();

        List<Role> roles = event.getInteraction().getValue("role-selection").getAsMentions().getRoles();

        List<Button> buttons = roles.stream().map(role -> Button.primary(BUTTON_PREFIX_ID + role.getId(), role.getName())).toList();

        channel.sendMessageComponents(Container.of(
                        TextDisplay.of("## " + title),
                        TextDisplay.of(description),
                        Separator.createDivider(Separator.Spacing.SMALL),
                        ActionRow.of(buttons)
                ).withAccentColor(BotColors.SUCCESS.getColor()))
                .useComponentsV2()
                .queue();

        event.reply("Sent message.").setEphemeral(true).queue();
    }
}
