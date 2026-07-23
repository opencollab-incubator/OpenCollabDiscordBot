/*
 * Copyright (c) 2020-2026 Open Collaboration. https://opencollaboration.dev, GeyserMC. https://geysermc.org
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

package org.geysermc.discordbot.commands.ticket;

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.geysermc.discordbot.util.TicketHelper;
import org.geysermc.discordbot.util.ticket.TicketMetadata;
import org.geysermc.discordbot.util.ticket.TicketType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class TicketCommand extends SlashCommand {

    public TicketCommand() {
        this.name = "ticket";
        this.hidden = true;
        this.help = "Help tool to manage tickets.";
        this.contexts = new InteractionContextType[]{InteractionContextType.GUILD};
        this.children = new SlashCommand[] {
                new CloseTicketSubCommand(),
                new AddUserTicketSubCommand(),
                new RemoveUserTicketSubCommand(),
                new AddRoleTicketSubCommand(),
                new RemoveRoleTicketSubCommand(),
                new SetTicketTypeSubCommand()
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

            if (!TicketHelper.canManageTicket(event.getMember(), event.getGuildChannel())) {
                event.reply("You do not have permission to manage this ticket.").setEphemeral(true).queue();
            }

            TicketHelper.closeTicket(event.getGuildChannel(), event.getMember());
        }
    }

    public static class AddUserTicketSubCommand extends SlashCommand {
        public AddUserTicketSubCommand() {
            this.name = "add-user";
            this.help = "Adds a user to the current ticket";
            this.userPermissions = new Permission[]{Permission.MANAGE_CHANNEL};
            this.options = List.of(
                    new OptionData(OptionType.USER, "member", "The member to add", true)
            );
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            if (!TicketHelper.isTicketChannel(event.getGuildChannel())) {
                event.reply("This command can only be used within tickets.").setEphemeral(true).queue();
            }

            if (!TicketHelper.canManageTicket(event.getMember(), event.getGuildChannel())) {
                event.reply("You do not have permission to manage this ticket.").setEphemeral(true).queue();
            }

            Member member = event.optMember("member");
            event.getTextChannel().upsertPermissionOverride(member)
                            .setAllowed(Permission.VIEW_CHANNEL).queue(permissionOverride -> {
                                event.getTextChannel().sendMessage(":wave: %s".formatted(member.getAsMention()))
                                        .setAllowedMentions(List.of()).queue();
                    });

            event.reply("Added %s.".formatted(member.getAsMention())).setEphemeral(true).queue();
        }
    }

    public static class RemoveUserTicketSubCommand extends SlashCommand {
        public RemoveUserTicketSubCommand() {
            this.name = "remove-user";
            this.help = "Removes a user to the current ticket";
            this.userPermissions = new Permission[]{Permission.MANAGE_CHANNEL};
            this.options = List.of(
                    new OptionData(OptionType.USER, "member", "The member to remove", true)
            );
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            if (!TicketHelper.isTicketChannel(event.getGuildChannel())) {
                event.reply("This command can only be used within tickets.").setEphemeral(true).queue();
            }

            if (!TicketHelper.canManageTicket(event.getMember(), event.getGuildChannel())) {
                event.reply("You do not have permission to manage this ticket.").setEphemeral(true).queue();
            }

            Member member = event.optMember("member");
            event.getTextChannel().upsertPermissionOverride(member)
                    .setDenied(Permission.VIEW_CHANNEL).queue(permissionOverride -> {
                        event.getTextChannel().sendMessage(":octagonal_sign: %s".formatted(member.getAsMention()))
                                .setAllowedMentions(List.of()).queue();
                    });

            event.reply("Removed %s.".formatted(member.getAsMention())).setEphemeral(true).queue();
        }
    }

    public static class AddRoleTicketSubCommand extends SlashCommand {
        public AddRoleTicketSubCommand() {
            this.name = "add-role";
            this.help = "Adds a role to the current ticket";
            this.userPermissions = new Permission[]{Permission.MANAGE_CHANNEL};
            this.options = List.of(
                    new OptionData(OptionType.ROLE, "role", "The role to add", true)
            );
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            if (!TicketHelper.isTicketChannel(event.getGuildChannel())) {
                event.reply("This command can only be used within tickets.").setEphemeral(true).queue();
            }

            if (!TicketHelper.canManageTicket(event.getMember(), event.getGuildChannel())) {
                event.reply("You do not have permission to manage this ticket.").setEphemeral(true).queue();
            }

            Role role = event.optRole("role");
            event.getTextChannel().upsertPermissionOverride(role)
                    .setAllowed(Permission.VIEW_CHANNEL).queue(permissionOverride -> {
                        event.getTextChannel().sendMessage(":wave: %s".formatted(role.getAsMention()))
                                .setAllowedMentions(List.of()).queue();
                    });

            event.reply("Added %s.".formatted(role.getAsMention())).setEphemeral(true).queue();
        }
    }

    public static class RemoveRoleTicketSubCommand extends SlashCommand {
        public RemoveRoleTicketSubCommand() {
            this.name = "remove-role";
            this.help = "Removes a role to the current ticket";
            this.userPermissions = new Permission[]{Permission.MANAGE_CHANNEL};
            this.options = List.of(
                    new OptionData(OptionType.ROLE, "role", "The role to remove", true)
            );
        }

        @Override
        protected void execute(SlashCommandEvent event) {
            if (!TicketHelper.isTicketChannel(event.getGuildChannel())) {
                event.reply("This command can only be used within tickets.").setEphemeral(true).queue();
            }

            if (!TicketHelper.canManageTicket(event.getMember(), event.getGuildChannel())) {
                event.reply("You do not have permission to manage this ticket.").setEphemeral(true).queue();
            }

            Role role = event.optRole("role");
            event.getTextChannel().upsertPermissionOverride(role)
                    .setDenied(Permission.VIEW_CHANNEL).queue(permissionOverride -> {
                        event.getTextChannel().sendMessage(":octagonal_sign: %s".formatted(role.getAsMention()))
                                .setAllowedMentions(List.of()).queue();
                    });

            event.reply("Removed %s.".formatted(role.getAsMention())).setEphemeral(true).queue();
        }
    }

    public static class SetTicketTypeSubCommand extends SlashCommand {
        public SetTicketTypeSubCommand() {
            this.name = "set-type";
            this.help = "Sets the current ticket type";

            this.options = List.of(
                    new OptionData(OptionType.STRING, "type", "The ticket type", true)
                            .addChoices(Arrays.stream(TicketType.values()).map(type ->
                                    new Command.Choice(type.name(), type.name())).toList())
            );
        }

        @Override
        protected void execute(@NotNull SlashCommandEvent event) {
            if (!TicketHelper.isTicketChannel(event.getGuildChannel())) {
                event.reply("This command can only be used within tickets.").setEphemeral(true).queue();
            }

            if (!TicketHelper.canManageTicket(event.getMember(), event.getGuildChannel())) {
                event.reply("You do not have permission to manage this ticket.").setEphemeral(true).queue();
            }

            TicketMetadata metadata = TicketHelper.getTicketMetadata(event.getTextChannel());
            if (metadata == null) {
                event.reply("Something went wrong.").setEphemeral(true).queue();
                return;
            }
            TicketType currentType = metadata.type();
            metadata = metadata.withTicketType(event.optString("type"));
            TicketHelper.setTicketMetadata(event.getTextChannel(), metadata);

            event.getTextChannel().getManager().setName(metadata.getChannelName()).queue();

            StringBuilder logLine = new StringBuilder();
            logLine.append("@").append(event.getUser().getName())
                    .append(" (ID: ")
                    .append(event.getUser().getId())
                    .append(") changed the ticket type from ")
                    .append(currentType.name())
                    .append(" to ")
                    .append(metadata.type().name());

            TicketHelper.logTicketAction(event.getTextChannel(), logLine.toString());

            event.reply("Changed ticket type.").setEphemeral(true).queue();
        }
    }
}
