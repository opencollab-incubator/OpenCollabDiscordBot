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

package org.geysermc.discordbot.util;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import org.geysermc.discordbot.GeyserBot;
import org.geysermc.discordbot.storage.ServerConfiguration;
import org.geysermc.discordbot.storage.ServerModule;
import org.geysermc.discordbot.util.ticket.GuildTicket;
import org.geysermc.discordbot.util.ticket.GuildTickets;
import org.geysermc.discordbot.util.ticket.TicketData;
import org.geysermc.discordbot.util.ticket.TicketMetadata;
import org.geysermc.discordbot.util.ticket.TicketState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TicketHelper {
    private static final Logger log = LoggerFactory.getLogger(TicketHelper.class);

    /**
     * Base URL of the web panel page that lets staff view a ticket's permanent transcript.
     */
    private static final String TICKET_VIEW_URL = PropertiesManager.getPublicWebAddress();

    public static boolean isTicketChannel(GuildChannel c) {
        if (!(c instanceof TextChannel channel)) return false;

        List<Category> categories = ServerConfiguration.TICKET_CATEGORIES.get(c.getGuild());
        if (categories.isEmpty()) return false;

        for (Category category : categories) {
            if (category.getId().equals(channel.getParentCategoryId())) return true;
        }

        return false;
    }

    public static boolean canManageTicket(Member member, GuildChannel c) {
        if (!isTicketChannel(c)) return false;

        TextChannel channel = (TextChannel) c;
        TicketMetadata metadata = getTicketMetadata(channel);
        if (metadata == null) return false;
        Role role = c.getGuild().getRoleById(metadata.clientRoleId());
        List<Role> validRoles = new ArrayList<>();
        validRoles.add(role);
        validRoles.addAll(ServerConfiguration.TICKET_ALLOWED_ROLES.get(c.getGuild()));
        return member.getUnsortedRoles().stream().anyMatch(validRoles::contains);
    }

    /**
     * Closes a ticket's channel. The ticket's metadata and transcript are kept in the database
     * forever - only the Discord channel itself is removed. If an archive channel is configured,
     * a link to the ticket's permanent web view is posted there instead of a file export.
     */
    public static void closeTicket(GuildChannel channel, Member member) {
        if (!TicketHelper.isTicketChannel(channel)) return;

        TextChannel textChannel = (TextChannel) channel;

        TicketMetadata metadata = getTicketMetadata(textChannel);
        if (metadata == null) {
            log.info("Ticket channel has no tracked metadata, deleting channel without archiving.");
            channel.delete().queue();
            return;
        }

        User user = member.getUser();
        logTicketAction(metadata, "@%s (ID: %s) closed the ticket.".formatted(user.getName(), user.getId()));

        metadata = metadata.withState(TicketState.CLOSED).withChannelId(null);
        GeyserBot.storageManager.updateTicket(metadata);

        if (ServerConfiguration.TICKET_ARCHIVE_CHANNEL.isConfigured(channel.getGuild())) {
            TextChannel archiveChannel = ServerConfiguration.TICKET_ARCHIVE_CHANNEL.get(channel.getGuild());
            if (archiveChannel != null) {
                String link = "%s/%s/tickets/%d".formatted(TICKET_VIEW_URL, metadata.guildId(), metadata.ticketId());
                archiveChannel.sendMessage(
                                "Ticket closed by %s.\nClient: <@&%s>\nType: %s\nView transcript: <%s>"
                                        .formatted(member.getAsMention(), metadata.clientRoleId(), metadata.type().name(), link))
                        .setAllowedMentions(List.of()).queue();
            } else {
                log.warn("No archive channel found. Not posting a transcript link.");
            }
        }

        channel.delete().queue();
    }

    /**
     * Creates a ticket, both persisting its metadata to the database and setting up its channel.
     */
    public static TicketMetadata initTicket(TextChannel channel, TicketData ticketData) {
        channel.createWebhook("OC Bot Web Access").queue();

        TicketMetadata metadata = ticketData.metadata(channel);
        int ticketId = GeyserBot.storageManager.createTicket(metadata);
        return metadata.withTicketId(ticketId);
    }

    public static TicketMetadata getTicketMetadata(TextChannel channel) {
        return GeyserBot.storageManager.getTicketByChannel(channel.getIdLong());
    }

    public static void setTicketMetadata(TicketMetadata metadata) {
        GeyserBot.storageManager.updateTicket(metadata);
    }

    public static void logTicketAction(TicketMetadata metadata, String line) {
        GeyserBot.storageManager.addTicketLog(metadata.ticketId(), line);
    }

    /**
     * Directory attachments downloaded from a ticket's messages are saved to. Keyed by the
     * ticket's stable database ID rather than its (potentially now-deleted) channel ID. Created
     * on demand if it doesn't already exist.
     */
    public static Path getTicketDirectory(int ticketId) {
        Path directory = Path.of("tickets", String.valueOf(ticketId));
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return directory;
    }

    public static GuildTickets getTickets(Guild guild) {
        if (!ServerModule.TICKETS.isEnabled(guild)) return GuildTickets.EMPTY;

        List<GuildTicket> tickets = GeyserBot.storageManager.getTickets(guild.getIdLong()).stream()
                .map(GuildTicket::new)
                .toList();

        return new GuildTickets(tickets);
    }
}
