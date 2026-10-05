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

package org.geysermc.discordbot.http.controller;

import jakarta.servlet.http.HttpServletRequest;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import org.geysermc.discordbot.GeyserBot;
import org.geysermc.discordbot.http.PageUtils;
import org.geysermc.discordbot.http.pages.Error;
import org.geysermc.discordbot.http.pages.tickets.ViewTicketPage;
import org.geysermc.discordbot.http.pages.tickets.ViewTicketsPage;
import org.geysermc.discordbot.permissions.MemberRolePermissions;
import org.geysermc.discordbot.permissions.PermissionManager;
import org.geysermc.discordbot.storage.ServerModule;
import org.geysermc.discordbot.util.TicketHelper;
import org.geysermc.discordbot.util.ticket.TicketMetadata;
import org.geysermc.discordbot.util.ticket.TicketState;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
public class TicketsController {
    @GetMapping("/{guildId}/tickets")
    public ResponseEntity<String> getTickets(HttpServletRequest request, @AuthenticationPrincipal OAuth2User principal, @PathVariable String guildId) {
        AccessCheck access = checkAccess(request, principal, guildId);
        if (access.denied() != null) return access.denied();

        return PageUtils.getResponse(request, principal, new ViewTicketsPage(access.user(), access.guild()));
    }

    @GetMapping("/{guildId}/tickets/{ticketId}")
    public ResponseEntity<String> getTicket(HttpServletRequest request, @AuthenticationPrincipal OAuth2User principal, @PathVariable String guildId, @PathVariable int ticketId) {
        AccessCheck access = checkAccess(request, principal, guildId);
        if (access.denied() != null) return access.denied();

        TicketMetadata metadata = GeyserBot.storageManager.getTicket(ticketId);
        if (metadata == null || metadata.guildId() != access.guild().getIdLong()) {
            return PageUtils.getResponse(request, principal, new Error(access.user(), HttpStatus.NOT_FOUND), HttpStatus.NOT_FOUND);
        }

        boolean canManage = PermissionManager.hasPermission(access.member(), MemberRolePermissions.MANAGE_TICKETS);

        return PageUtils.getResponse(request, principal, new ViewTicketPage(access.user(), access.guild(), metadata, canManage));
    }

    /**
     * Serves a locally-archived message attachment for a ticket, so the "Messages" view can
     * render it (e.g. inline images) without depending on the original, expiring Discord CDN URL.
     */
    @GetMapping("/{guildId}/tickets/{ticketId}/attachments/{fileName}")
    public ResponseEntity<byte[]> getAttachment(HttpServletRequest request, @AuthenticationPrincipal OAuth2User principal, @PathVariable String guildId, @PathVariable int ticketId, @PathVariable String fileName) {
        AccessCheck access = checkAccess(request, principal, guildId);
        if (access.denied() != null) return ResponseEntity.status(access.denied().getStatusCode()).build();

        TicketMetadata metadata = GeyserBot.storageManager.getTicket(ticketId);
        if (metadata == null || metadata.guildId() != access.guild().getIdLong()) {
            return ResponseEntity.notFound().build();
        }

        Path directory = TicketHelper.getTicketDirectory(ticketId).toAbsolutePath().normalize();
        Path file = directory.resolve(fileName).normalize();
        if (!file.startsWith(directory) || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] data = Files.readAllBytes(file);
            String contentType = Files.probeContentType(file);
            return ResponseEntity.ok()
                    .contentType(contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Closes a ticket from the web panel: deletes its Discord channel (if it still has one) the
     * same way the in-Discord "Close Ticket" button does, and marks it closed. The ticket's
     * metadata and transcript stay on record permanently either way.
     */
    @PostMapping("/api/{guildId}/tickets/{ticketId}/close")
    public ResponseEntity<String> closeTicket(@AuthenticationPrincipal OAuth2User principal, @PathVariable String guildId, @PathVariable int ticketId) {
        User user = null;
        if (principal != null) {
            String userId = principal.getAttribute("id");
            user = GeyserBot.getJDA().getUserById(userId);
        }

        if (user == null) {
            return jsonError(HttpStatus.UNAUTHORIZED, "You must be signed in to access this endpoint.");
        }

        Guild guild = GeyserBot.getJDA().getGuildById(guildId);
        if (guild == null || !ServerModule.TICKETS.isEnabled(guild)) {
            return jsonError(HttpStatus.NOT_FOUND, "That server could not be found.");
        }

        Member member = guild.getMember(user);
        if (member == null || !PermissionManager.hasPermission(member, MemberRolePermissions.MANAGE_TICKETS)) {
            return jsonError(HttpStatus.FORBIDDEN, "You do not have permission to manage tickets in this server.");
        }

        TicketMetadata metadata = GeyserBot.storageManager.getTicket(ticketId);
        if (metadata == null || metadata.guildId() != guild.getIdLong()) {
            return jsonError(HttpStatus.NOT_FOUND, "That ticket could not be found.");
        }

        if (metadata.state() != TicketState.OPEN) {
            return jsonError(HttpStatus.CONFLICT, "This ticket has already been closed.");
        }

        GuildChannel channel = metadata.channelId() == null ? null : guild.getGuildChannelById(metadata.channelId());
        if (channel != null) {
            TicketHelper.closeTicket(channel, member);
        } else {
            GeyserBot.storageManager.updateTicket(metadata.withState(TicketState.CLOSED).withChannelId(null));
        }

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new JSONObject(Map.of(
                        "code", 200,
                        "message", "Ticket closed."
                )).toString());
    }

    private static ResponseEntity<String> jsonError(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new JSONObject(Map.of(
                        "code", status.value(),
                        "message", message
                )).toString());
    }

    /**
     * Resolves the requesting user and guild, and verifies the user is allowed to view tickets
     * for that guild. All ticket routes are gated behind this same check.
     */
    private AccessCheck checkAccess(HttpServletRequest request, OAuth2User principal, String guildId) {
        User user = null;
        if (principal != null) {
            String userId = principal.getAttribute("id");
            user = GeyserBot.getJDA().retrieveUserById(userId).complete();
        }

        ResponseEntity<String> notFound = PageUtils.getResponse(request, principal, new Error(user, HttpStatus.NOT_FOUND), HttpStatus.NOT_FOUND);

        Guild guild = GeyserBot.getJDA().getGuildById(guildId);
        if (guild == null || user == null) {
            return new AccessCheck(null, null, null, notFound);
        }

        if (!ServerModule.TICKETS.isEnabled(guild)) {
            return new AccessCheck(null, null, null, notFound);
        }

        Member member = guild.getMember(user);
        if (member == null) {
            return new AccessCheck(null, null, null, notFound);
        }

        if (!PermissionManager.hasPermission(member, MemberRolePermissions.VIEW_TICKETS)) {
            return new AccessCheck(null, null, null, notFound);
        }

        return new AccessCheck(user, guild, member, null);
    }

    private record AccessCheck(User user, Guild guild, Member member, ResponseEntity<String> denied) {
    }
}
