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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServletRequest;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import org.geysermc.discordbot.GeyserBot;
import org.geysermc.discordbot.http.PageUtils;
import org.geysermc.discordbot.http.pages.Error;
import org.geysermc.discordbot.http.pages.HomeMessage;
import org.geysermc.discordbot.http.pages.guild.Configure;
import org.geysermc.discordbot.http.pages.guild.Leaderboard;
import org.geysermc.discordbot.permissions.MemberRolePermissions;
import org.geysermc.discordbot.permissions.PermissionManager;
import org.geysermc.discordbot.permissions.UserPermissions;
import org.geysermc.discordbot.storage.ServerConfiguration;
import org.geysermc.discordbot.util.BotServers;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class ConfigureController {
    private static final Logger log = LoggerFactory.getLogger(ConfigureController.class);

    @GetMapping("/{guildId}/configure")
    public ResponseEntity<String> getConfigure(HttpServletRequest request, @AuthenticationPrincipal OAuth2User principal, @PathVariable String guildId) {
        User user = null;
        if (principal != null) {
            String userId = principal.getAttribute("id");
            user = GeyserBot.getJDA().retrieveUserById(userId).complete();
        }

        if (user == null) {
            return PageUtils.getResponse(request, principal, new Error(null, HttpStatus.UNAUTHORIZED, "You must login to continue."), HttpStatus.UNAUTHORIZED);
        }

        Guild guild = GeyserBot.getJDA().getGuildById(guildId);
        if (guild == null || BotServers.isHidden(guild, user)) {
            return PageUtils.getResponse(request, principal, new Error(user, 404, "Server not found."), HttpStatus.NOT_FOUND);
        }

        boolean hasPermission = false;

        if (PermissionManager.hasPermission(user, UserPermissions.CONFIGURE_ALL_SERVERS)) {
            hasPermission = true;
        } else if (guild.isMember(user)) {
            Member member = guild.getMember(user);
            hasPermission = PermissionManager.hasPermission(member, MemberRolePermissions.CONFIGURE_SERVER);
        }

        if (!hasPermission) {
            return PageUtils.getResponse(request, principal, new Error(
                    user, 401,
                    "You do not have permission to view this page."
            ), HttpStatus.UNAUTHORIZED);
        }

        return PageUtils.getResponse(request, principal, new Configure(user, guild));
    }

    @PostMapping("/api/configure/modify/{guildId}")
    public ResponseEntity<String> modifyConfigure(HttpServletRequest request, @AuthenticationPrincipal OAuth2User principal, @PathVariable String guildId, @RequestBody String jsonString) {
        User user = null;
        if (principal != null) {
            String userId = principal.getAttribute("id");
            user = GeyserBot.getJDA().getUserById(userId);
        }

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new JSONObject(Map.of(
                            "code", 401,
                            "message", "You must be signed in to access this endpoint."
                    )).toString());
        }

        Guild guild = GeyserBot.getJDA().getGuildById(guildId);

        if (guild == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new JSONObject(Map.of(
                            "code", 400,
                            "message", "No guild with that ID was found."
                    )).toString());
        }

        Member member = guild.getMember(user);

        if (
                !PermissionManager.hasPermission(member, MemberRolePermissions.CONFIGURE_SERVER) &&
                !PermissionManager.hasPermission(user, UserPermissions.CONFIGURE_ALL_SERVERS)
        ) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new JSONObject(Map.of(
                            "code", 403,
                            "message", "You do not have the permissions to access this endpoint."
                    )).toString());
        }

        JsonObject configOptions = JsonParser.parseString(jsonString).getAsJsonObject();

        // We only want to set all the configs if all of them are valid.
        List<Runnable> validActions = new ArrayList<>();

        for (Map.Entry<String, JsonElement> configOption : configOptions.entrySet()) {
            JsonElement value = configOption.getValue();
            ServerConfiguration.Configuration<?> configuration = ServerConfiguration.get(configOption.getKey());
            if (configuration == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new JSONObject(Map.of(
                                "code", 400,
                                "message", "An invalid configuration key was provided. (%s)".formatted(configOption.getKey())
                        )).toString());
            }

            ResponseEntity<String> badResponse = ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new JSONObject(Map.of(
                            "code", 400,
                            "message", "An invalid configuration value was provided. (%s)".formatted(configOption.getKey())
                    )).toString());

            switch (configuration.getConfigType()) {
                case SINGLE -> {
                    if (!value.isJsonPrimitive()) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(new JSONObject(Map.of(
                                        "code", 400,
                                        "message", "An invalid configuration value was provided, expected primitive, got %s. (%s)".formatted(value.getClass().getSimpleName(), configOption.getKey())
                                )).toString());
                    }
                    if (configuration.getType() == String.class) {
                        validActions.add(() -> {
                            ((ServerConfiguration.Configuration<String>) configuration)
                                    .set(guild, value.getAsString());
                        });
                    } else if (configuration.getType() == Role.class) {
                        String str = value.getAsString();
                        Role role = str.isEmpty() ? null : guild.getRoleById(str);

                        if (role == null) ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(new JSONObject(Map.of(
                                        "code", 400,
                                        "message", "An invalid configuration value was provided, role %s not found. (%s)".formatted(str, configOption.getKey())
                                )).toString());

                        validActions.add(() -> {
                            ((ServerConfiguration.Configuration<Role>) configuration)
                                    .set(guild, role);
                        });
                    } else if (GuildChannel.class.isAssignableFrom(configuration.getType())) {
                        String str = value.getAsString();
                        GuildChannel channel = str.isEmpty() ? null : guild.getChannelById((Class<? extends GuildChannel>) configuration.getType(), str);

                        if (channel == null) ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(new JSONObject(Map.of(
                                        "code", 400,
                                        "message", "An invalid configuration value was provided, channel %s not found. (%s)".formatted(str, configOption.getKey())
                                )).toString());

                        validActions.add(() -> {
                            ((ServerConfiguration.Configuration<GuildChannel>) configuration)
                                    .set(guild, channel);
                        });
                    }
                }
                case MULTIPLE -> {
                    if (!value.isJsonArray()) return badResponse;
                    if (configuration.getType() == String.class) {
                        List<String> strings = new ArrayList<>();

                        for (JsonElement element : value.getAsJsonArray()) {
                            if (!element.isJsonPrimitive()) return badResponse;

                            strings.add(element.getAsString());
                        }

                        if (strings.isEmpty()) strings = null;

                        List<String> finalStrings = strings;
                        validActions.add(() -> {
                            ((ServerConfiguration.Configuration<List<String>>) configuration)
                                    .set(guild, finalStrings);
                        });
                    } else if (configuration.getType() == Role.class) {
                        List<Role> roles = new ArrayList<>();

                        for (JsonElement element : value.getAsJsonArray()) {
                            if (!element.isJsonPrimitive()) return badResponse;

                            Role role = guild.getRoleById(element.getAsString());

                            if (role == null) return badResponse;

                            roles.add(role);
                        }

                        if (roles.isEmpty()) roles = null;

                        List<Role> finalRoles = roles;
                        validActions.add(() -> {
                            ((ServerConfiguration.Configuration<List<Role>>) configuration)
                                    .set(guild, finalRoles);
                        });
                    } else if (GuildChannel.class.isAssignableFrom(configuration.getType())) {
                        List<GuildChannel> channels = new ArrayList<>();

                        for (JsonElement element : value.getAsJsonArray()) {
                            if (!element.isJsonPrimitive()) return badResponse;

                            GuildChannel channel = guild.getChannelById((Class<? extends GuildChannel>) configuration.getType(), element.getAsString());

                            if (channel == null) return badResponse;

                            channels.add(channel);
                        }

                        if (channels.isEmpty()) channels = null;

                        List<GuildChannel> finalChannels = channels;
                        validActions.add(() -> {
                            ((ServerConfiguration.Configuration<List<? extends GuildChannel>>) configuration)
                                    .set(guild, finalChannels);
                        });
                    }
                }
            }
        }

        validActions.forEach(Runnable::run);

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new JSONObject(Map.of(
                        "code", 200,
                        "message", "Set configurations successfully."
                )).toString());
    }
}
