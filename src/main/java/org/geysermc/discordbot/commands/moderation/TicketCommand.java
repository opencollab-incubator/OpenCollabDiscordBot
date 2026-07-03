/*
 * Copyright (c) 2020-2026 GeyserMC. http://geysermc.org
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
 * @author GeyserMC
 * @link https://github.com/GeyserMC/GeyserDiscordBot
 */

package org.geysermc.discordbot.commands.moderation;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumTag;
import net.dv8tion.jda.api.entities.channel.forums.ForumTagSnowflake;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.managers.channel.concrete.ThreadChannelManager;
import net.dv8tion.jda.internal.utils.Checks;
import org.geysermc.discordbot.listeners.TicketHandlers;
import org.geysermc.discordbot.storage.ServerSettings;
import org.geysermc.discordbot.util.BotColors;
import org.geysermc.discordbot.util.DicesCoefficient;
import org.geysermc.discordbot.util.TicketHelper;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;

public class TicketCommand extends SlashCommand {

    public TicketCommand() {
        this.name = "ticket";
        this.hidden = true;
        this.help = "Help tool to manage tickets.";
        this.contexts = new InteractionContextType[]{InteractionContextType.GUILD};
        this.userPermissions = new Permission[]{Permission.MANAGE_CHANNEL};
        this.children = new SlashCommand[] {
                new CloseTicketSubCommand(),
                new AddUserTicketSubCommand(),
                new RemoveUserTicketSubCommand()
        };
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        // unused
    }

    public static class CloseTicketSubCommand extends SlashCommand {
        public CloseTicketSubCommand() {
            this.name = "close";
            this.help = "Closes the current ticket";
        }

        @Override
        protected void execute(@NotNull SlashCommandEvent event) {
            if (!TicketHelper.isTicketChannel(event.getGuildChannel())) {
                event.reply("This command can only be used within tickets.").setEphemeral(true).queue();
            }

            TicketHelper.closeTicket(event.getGuildChannel(), event.getMember());
        }
    }

    public static class AddUserTicketSubCommand extends SlashCommand {
        public AddUserTicketSubCommand() {
            this.name = "add-user";
            this.help = "Adds a user to the current ticket";
            this.userPermissions = new Permission[]{Permission.MANAGE_CHANNEL};
            this.options = Arrays.asList(
                    new OptionData(OptionType.USER, "member", "The member to add", true)
            );
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            if (!TicketHelper.isTicketChannel(event.getGuildChannel())) {
                event.reply("This command can only be used within tickets.").setEphemeral(true).queue();
            }

            Member member = event.optMember("member");
            event.getTextChannel().upsertPermissionOverride(member)
                            .setAllowed(Permission.VIEW_CHANNEL).queue(permissionOverride -> {
                                event.getTextChannel().sendMessage(":wave: %s".formatted(member.getAsMention())).queue();
                    });

            event.reply("Added %s.".formatted(member.getAsMention())).setEphemeral(true).queue();
        }
    }

    public static class RemoveUserTicketSubCommand extends SlashCommand {
        public RemoveUserTicketSubCommand() {
            this.name = "remove-user";
            this.help = "Removes a user to the current ticket";
            this.userPermissions = new Permission[]{Permission.MANAGE_CHANNEL};
            this.options = Arrays.asList(
                    new OptionData(OptionType.USER, "member", "The member to remove", true)
            );
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            if (!TicketHelper.isTicketChannel(event.getGuildChannel())) {
                event.reply("This command can only be used within tickets.").setEphemeral(true).queue();
            }

            Member member = event.optMember("member");
            event.getTextChannel().upsertPermissionOverride(member)
                    .setDenied(Permission.VIEW_CHANNEL).queue(permissionOverride -> {
                        event.getTextChannel().sendMessage(":octagonal_sign: %s".formatted(member.getAsMention())).queue();
                    });

            event.reply("Removed %s.".formatted(member.getAsMention())).setEphemeral(true).queue();
        }
    }
}
