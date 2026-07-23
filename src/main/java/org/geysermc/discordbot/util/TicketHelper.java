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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.io.FileUtils;
import org.geysermc.discordbot.storage.ServerSettings;
import org.geysermc.discordbot.util.ticket.TicketData;
import org.geysermc.discordbot.util.ticket.TicketMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.time.temporal.ChronoField.*;

public class TicketHelper {
    public static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendValue(DAY_OF_MONTH, 2)
            .appendLiteral('/')
            .appendValue(MONTH_OF_YEAR, 2)
            .appendLiteral('/')
            .appendValue(YEAR, 4)
            .appendLiteral(' ')
            .appendValue(HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(MINUTE_OF_HOUR, 2)
            .appendLiteral(":")
            .appendValue(SECOND_OF_MINUTE, 2)
            .optionalStart()
            .parseLenient()
            .appendOffset("+HHMMss", "")
            .parseStrict()
            .toFormatter();
    private static final Logger log = LoggerFactory.getLogger(TicketHelper.class);
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting().create();

    public static boolean isTicketChannel(GuildChannel c) {
        if (!(c instanceof TextChannel channel)) return false;

        List<Category> categories = ServerSettings.getTicketCategories(channel.getGuild());
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
        validRoles.addAll(ServerSettings.getTicketAllowedRoles(c.getGuild()));
        return member.getUnsortedRoles().stream().anyMatch(validRoles::contains);
    }

    public static void closeTicket(GuildChannel channel, Member member) {
        if (!TicketHelper.isTicketChannel(channel)) return;

        TextChannel textChannel = (TextChannel) channel;

        if (isLoggedTicket(textChannel)) {
            User user = member.getUser();
            logTicketAction(textChannel, "@%s (ID: %s) closed the ticket.".formatted(user.getName(), user.getId()));
        }

        if (!isLoggedTicket(textChannel)) {
            log.info("Not a logged ticket. Unable to archive.");
            channel.delete().queue();
            return;
        }

        if (!ServerSettings.isTicketArchiving(channel.getGuild())) {
            log.info("Not ticket archiving, deleting channel but not ticket data.");
            channel.delete().queue();
            return;
        }

        TextChannel archiveChannel = ServerSettings.getTicketArchiveChannel(channel.getGuild());
        if (archiveChannel == null) {
            log.warn("No archive channel found. Not archiving.");
            channel.delete().queue();
            return;
        }

        TicketMetadata metadata = getTicketMetadata(textChannel);
        if (metadata == null) {
            log.error("Ticket is logged but no metadata is present.");
            textChannel.sendMessage("Something went wrong while deleting ticket.").queue();
            return;
        }

        Path logDirectory = TicketHelper.getTicketDirectory(textChannel);

        ByteArrayOutputStream stream;
        try {
            stream = new ByteArrayOutputStream();
            ZipOutputStream zipOut = new ZipOutputStream(stream);
            Files.list(logDirectory).forEach(item -> {
                try {
                    FileInputStream fis = new FileInputStream(item.toFile());
                    ZipEntry zipEntry = new ZipEntry(item.toFile().getName());
                    zipOut.putNextEntry(zipEntry);
                    byte[] bytes = new byte[1024];
                    int length;
                    while((length = fis.read(bytes)) >= 0) {
                        zipOut.write(bytes, 0, length);
                    }
                    fis.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            zipOut.close();
            stream.close();

            FileUtils.deleteDirectory(logDirectory.toFile());
        } catch (IOException e) {
            StringWriter writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            textChannel.sendMessage(writer.toString()).queue();
            e.printStackTrace();
            return;
        }

        archiveChannel.sendFiles(FileUpload.fromData(stream.toByteArray(),  "ticket_%s_%s.zip".formatted(metadata.clientId(), metadata.id())))
                .setContent("Ticket closed by %s.\nClient: <@&%s>\nType: %s".formatted(member.getAsMention(), metadata.clientRoleId(), metadata.type().name()))
                .setAllowedMentions(List.of()).queue(message -> {});

        channel.delete().queue();
    }

    public static boolean isLoggedTicket(TextChannel channel) {
        Path savePath = Path.of("tickets", channel.getId());
        return Files.exists(savePath);
    }

    public static void initTicket(TextChannel channel, TicketData ticketData) {
        Path savePath = Path.of("tickets", channel.getId(), "metadata.json");
        try {
            if (Files.notExists(savePath)) {
                Files.createDirectories(savePath.getParent());
                Files.createFile(savePath);
            }

            TicketMetadata metadata = ticketData.metadata();

            Files.writeString(savePath, GSON.toJson(metadata));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static TicketMetadata getTicketMetadata(TextChannel channel) {
        Path savePath = Path.of("tickets", channel.getId(), "metadata.json");
        if (Files.notExists(savePath)) return null;
        try {
            return GSON.fromJson(Files.newBufferedReader(savePath), TicketMetadata.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setTicketMetadata(TextChannel channel, TicketMetadata metadata) {
        Path savePath = Path.of("tickets", channel.getId(), "metadata.json");
        if (Files.notExists(savePath)) return;
        try {
            Files.writeString(savePath, GSON.toJson(metadata));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void logTicketAction(TextChannel channel, String line) {
        Path savePath = Path.of("tickets", channel.getId(), "log.txt");
        try {
            if (Files.notExists(savePath)) {
                Files.createDirectories(savePath.getParent());
                Files.createFile(savePath);
            }

            line = "[" + DATE_TIME_FORMATTER.format(LocalDateTime.now()) + "] " + line + "\n";

            Files.writeString(savePath, line, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Path getTicketDirectory(TextChannel channel) {
        return Path.of("tickets", channel.getId());
    }
}
