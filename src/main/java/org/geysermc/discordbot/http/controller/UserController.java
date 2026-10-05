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
import net.dv8tion.jda.api.entities.User;
import org.geysermc.discordbot.GeyserBot;
import org.geysermc.discordbot.http.PageUtils;
import org.geysermc.discordbot.http.pages.Error;
import org.geysermc.discordbot.http.pages.user.UserPage;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    @GetMapping("/user/{userId}")
    private ResponseEntity<String> noGuild(HttpServletRequest request, @AuthenticationPrincipal OAuth2User principal, @PathVariable String userId) {
        User user = null;
        if (principal != null) {
            String thisUserId = principal.getAttribute("id");
            user = GeyserBot.getJDA().retrieveUserById(thisUserId).complete();
        }

        if (userId.equals("self") && user != null) userId = user.getId();

        User targetUser = GeyserBot.getJDA().getUserById(userId);

        if (targetUser == null) {
            return PageUtils.getResponse(request, principal, new Error(user, 404, "User was not found!"));
        }

        return PageUtils.getResponse(request, principal, new UserPage(user, targetUser, null));
    }

    @GetMapping("/user/{userId}/{guildId}")
    private ResponseEntity<String> guild(HttpServletRequest request, @AuthenticationPrincipal OAuth2User principal, @PathVariable String userId, @PathVariable String guildId) {
        User user = null;
        if (principal != null) {
            String thisUserId = principal.getAttribute("id");
            user = GeyserBot.getJDA().retrieveUserById(thisUserId).complete();
        }

        User targetUser = GeyserBot.getJDA().getUserById(userId);

        if (targetUser == null) {
            return PageUtils.getResponse(request, principal, new Error(user, 404, "User was not found!"));
        }

        Guild guild = GeyserBot.getJDA().getGuildById(guildId);

        if (guild == null) {
            return PageUtils.getResponse(request, principal, new Error(user, 404, "Guild was not found!"));
        }

        if (!guild.isMember(targetUser)) {
            return PageUtils.getResponse(request, principal, new Error(user, 400, "User is not in the guild!"));
        }

        return PageUtils.getResponse(request, principal, new UserPage(user, targetUser, guild));
    }
}
