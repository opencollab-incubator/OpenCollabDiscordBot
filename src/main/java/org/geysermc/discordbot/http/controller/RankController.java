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
import org.geysermc.discordbot.GeyserBot;
import org.geysermc.discordbot.http.PageUtils;
import org.geysermc.discordbot.permissions.MemberRank;
import org.geysermc.discordbot.permissions.MemberRolePermissions;
import org.geysermc.discordbot.permissions.PermissionManager;
import org.geysermc.discordbot.permissions.UserPermissions;
import org.geysermc.discordbot.permissions.UserRank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;

@RestController
public class RankController {
    @PostMapping("/api/user/{userId}/ranks/set")
    public ResponseEntity<String> setUserRanks(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable String userId,
            @RequestBody JsonNode json
    ) {
        User user = null;
        if (principal != null) {
            String thisUserId = principal.getAttribute("id");
            user = GeyserBot.getJDA().retrieveUserById(thisUserId).complete();
        }

        if (user == null) {
            return PageUtils.getJsonResponse(HttpStatus.UNAUTHORIZED, "You must be signed in to access this endpoint.");
        }

        if (!PermissionManager.hasPermission(user, UserPermissions.MANAGE_USER_RANKS) &&
                !PermissionManager.hasPermission(user, UserPermissions.MANAGE_ALL_PERMISSIONS)) {
            return PageUtils.getJsonResponse(HttpStatus.FORBIDDEN, "You do not have the permissions to access this endpoint.");
        }

        User targetUser = GeyserBot.getJDA().retrieveUserById(userId).complete();

        if (targetUser == null) {
            return PageUtils.getJsonResponse(HttpStatus.NOT_FOUND, "The user specified doesn't exist.");
        }

        ArrayNode node = json.get("selectedElements").asArray();

        UserRank[] ranks = new UserRank[node.size()];

        int i = 0;
        for (JsonNode selectedElement : node) {
            if (!selectedElement.isString()) return PageUtils.getJsonResponse(
                    HttpStatus.BAD_REQUEST, "Invalid rank requested. (Not a string)"
            );

            String rankName = selectedElement.asString();

            try {
                UserRank userRank = UserRank.valueOf(rankName);
                ranks[i] = userRank;
            } catch (IllegalArgumentException e) {
                return PageUtils.getJsonResponse(
                        HttpStatus.BAD_REQUEST, "Invalid rank requested. (Rank not found)"
                );
            }

            i++;
        }

        GeyserBot.storageManager.setUserRanks(targetUser, ranks);

        return PageUtils.getJsonResponse(HttpStatus.OK, "Set user ranks successfully.");
    }

    @PostMapping("/api/guild/{guildId}/member/{userId}/ranks/set")
    public ResponseEntity<String> setMemberRanks(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable String guildId,
            @PathVariable String userId,
            @RequestBody JsonNode json
    ) {
        User user = null;
        if (principal != null) {
            String thisUserId = principal.getAttribute("id");
            user = GeyserBot.getJDA().retrieveUserById(thisUserId).complete();
        }

        if (user == null) {
            return PageUtils.getJsonResponse(HttpStatus.UNAUTHORIZED, "You must be signed in to access this endpoint.");
        }

        Guild guild = GeyserBot.getJDA().getGuildById(guildId);

        if (guild == null) {
            return PageUtils.getJsonResponse(HttpStatus.NOT_FOUND, "The guild specified doesn't exist.");
        }

        if (!PermissionManager.hasPermission(guild.getMember(user), MemberRolePermissions.MANAGE_RANKS) &&
                !PermissionManager.hasPermission(user, UserPermissions.MANAGE_ALL_PERMISSIONS)) {
            return PageUtils.getJsonResponse(HttpStatus.FORBIDDEN, "You do not have the permissions to access this endpoint.");
        }

        User targetUser = GeyserBot.getJDA().retrieveUserById(userId).complete();

        if (targetUser == null) {
            return PageUtils.getJsonResponse(HttpStatus.NOT_FOUND, "The user specified doesn't exist.");
        }

        Member member = guild.getMember(targetUser);

        if (member == null) {
            return PageUtils.getJsonResponse(HttpStatus.BAD_REQUEST, "The user specified is not in this guild.");
        }

        ArrayNode node = json.get("selectedElements").asArray();

        MemberRank[] ranks = new MemberRank[node.size()];

        int i = 0;
        for (JsonNode selectedElement : node) {
            if (!selectedElement.isString()) return PageUtils.getJsonResponse(
                    HttpStatus.BAD_REQUEST, "Invalid rank requested."
            );

            String rankName = selectedElement.asString();

            try {
                MemberRank memberRank = MemberRank.valueOf(rankName);
                ranks[i] = memberRank;
            } catch (IllegalArgumentException e) {
                return PageUtils.getJsonResponse(
                        HttpStatus.BAD_REQUEST, "Invalid rank requested."
                );
            }

            i++;
        }

        GeyserBot.storageManager.setMemberRanks(member, ranks);

        return PageUtils.getJsonResponse(HttpStatus.OK, "Set member ranks successfully.");
    }
}
