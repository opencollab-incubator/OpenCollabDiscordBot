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

package org.geysermc.discordbot.http.pages.user;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.geysermc.discordbot.GeyserBot;
import org.geysermc.discordbot.http.html.Base;
import org.geysermc.discordbot.http.html.Element;
import org.geysermc.discordbot.http.pages.elements.Name;
import org.geysermc.discordbot.http.pages.elements.PostRequestButton;
import org.geysermc.discordbot.http.pages.elements.PostSelectList;
import org.geysermc.discordbot.http.pages.elements.ServerCard;
import org.geysermc.discordbot.permissions.MemberRank;
import org.geysermc.discordbot.permissions.MemberRolePermissions;
import org.geysermc.discordbot.permissions.PermissionManager;
import org.geysermc.discordbot.permissions.UserPermissions;
import org.geysermc.discordbot.permissions.UserRank;
import org.geysermc.discordbot.storage.LevelInfo;
import org.geysermc.discordbot.storage.ServerModule;

import java.text.DecimalFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class UserPage extends Base {
    private static final DecimalFormat NUM_FORMAT = new DecimalFormat("###,###,###");

    private final User targetUser;
    private final Guild guild;

    public UserPage(User user, User targetUser, Guild guild) {
        super(user);
        this.targetUser = targetUser;
        this.guild = guild;
    }

    @Override
    public void appendHead(Element head) {
        super.appendHead(head);
        head.addInnerElement(Element.script("""
                window.messageBanner = function(data) {
                        let code, message;
                        if (data.status) code = data.status;
                        else code = data.code;
                        if (data.error) message = data.error;
                        else message = data.message;
                
                        const messageElement = document.getElementById('message-element');
                
                        messageElement.classList.remove('bg-red-200');
                        messageElement.classList.remove('dark:bg-red-900');
                        messageElement.classList.remove('border-red-300');
                        messageElement.classList.remove('dark:border-red-800');
                
                        messageElement.classList.remove('bg-orange-200');
                        messageElement.classList.remove('dark:bg-orange-900');
                        messageElement.classList.remove('border-orange-300');
                        messageElement.classList.remove('dark:border-orange-800');
                
                        messageElement.classList.remove('bg-yellow-200');
                        messageElement.classList.remove('dark:bg-yellow-900');
                        messageElement.classList.remove('border-yellow-300');
                        messageElement.classList.remove('dark:border-yellow-800');
                
                        messageElement.classList.remove('bg-green-200');
                        messageElement.classList.remove('dark:bg-green-900');
                        messageElement.classList.remove('border-green-300');
                        messageElement.classList.remove('dark:border-green-800');
                
                        messageElement.classList.remove('bg-blue-200');
                        messageElement.classList.remove('dark:bg-blue-900');
                        messageElement.classList.remove('border-blue-300');
                        messageElement.classList.remove('dark:border-blue-800');
                
                        if (code === 403) {
                            messageElement.classList.add('bg-red-200');
                            messageElement.classList.add('dark:bg-red-900');
                            messageElement.classList.add('border-red-300');
                            messageElement.classList.add('dark:border-red-800');
                        } else if (code === 400) {
                            messageElement.classList.add('bg-orange-200');
                            messageElement.classList.add('dark:bg-orange-900');
                            messageElement.classList.add('border-orange-300');
                            messageElement.classList.add('dark:border-orange-800');
                        } else if (code === 409) {
                            messageElement.classList.add('bg-yellow-200');
                            messageElement.classList.add('dark:bg-yellow-900');
                            messageElement.classList.add('border-yellow-300');
                            messageElement.classList.add('dark:border-yellow-800');
                        } else if (code === 200) {
                            messageElement.classList.add('bg-green-200');
                            messageElement.classList.add('dark:bg-green-900');
                            messageElement.classList.add('border-green-300');
                            messageElement.classList.add('dark:border-green-800');
                        } else {
                            messageElement.classList.add('bg-blue-200');
                            messageElement.classList.add('dark:bg-blue-900');
                            messageElement.classList.add('border-blue-300');
                            messageElement.classList.add('dark:border-blue-800');
                        }
                        messageElement.innerHTML = message;
                        messageElement.classList.remove('opacity-0');
                        messageElement.classList.remove('mb-0');
                        messageElement.classList.add('mb-4');
                        if (messageElement.dataset.scheduleId) clearTimeout(messageElement.dataset.scheduleId);
                        messageElement.dataset.scheduleId = setTimeout(() => {
                            messageElement.classList.add('opacity-0');
                            messageElement.classList.add('mb-0');
                            messageElement.classList.remove('mb-4');
                            messageElement.innerHTML = '';
                            messageElement.dataset.scheduleId = undefined;
                        }, 5000);
                }
                """));
    }

    @Override
    public void addContent(Element container) {
        container.addClass("flex flex-col gap-2");

        container.addInnerElement(Element.of("div")
                .addClass("opacity-0 mb-0 transition-all rounded-xl p-2")
                .addField("id", "message-element"));

        Element userDiv = Element.div("flex flex-row gap-4 content-center");

        String guildIconUrl = null;
        if (guild != null) {
            String iconId = guild.getIconId();
            if (iconId != null && iconId.startsWith("a_")) iconId = iconId.substring(2);
            guildIconUrl = (iconId == null)
                    ? "https://cdn.discordapp.com/embed/avatars/1.png"
                    : String.format(net.dv8tion.jda.api.entities.Guild.ICON_URL, guild.getId(), iconId, "png");
        }
        Element pfpDiv = Element.div("relative");

        String effectiveAvatarUrl;
        if (guild != null) {
            effectiveAvatarUrl = guild.getMember(targetUser).getEffectiveAvatarUrl();
        } else {
            effectiveAvatarUrl = targetUser.getEffectiveAvatarUrl();
        }

        pfpDiv.addInnerElement(Element.a("/user/%s".formatted(targetUser.getId())).addInnerElement(Element.img(
                effectiveAvatarUrl, "User Profile Picture",
                "w-32 h-32 rounded-lg border-4 border-gray-200 dark:border-gray-900"
        )));

        if (guildIconUrl != null) {
            pfpDiv.addInnerElement(Element.img(
                    guildIconUrl, "Guild Profile Picture",
                    "w-12 h-12 rounded-lg absolute -top-3 -left-3 bg-white border-4 border-gray-200 dark:border-gray-900"
            ).addClass("hover:w-14 hover:h-14 hover:-top-2 hover:-left-2 ease-out transition-all"));
        }

        userDiv.addInnerElement(pfpDiv);

        Element infoDiv = Element.div("flex flex-col gap-2 grow");

        infoDiv.addInnerElement(Element.of("h1", "text-3xl font-bold")
                .addInnerElement(new Name(targetUser, guild)));

        List<Guild> mutualGuilds = new ArrayList<>(targetUser.getMutualGuilds());

        if (guild != null) {
            Member member = guild.getMember(targetUser);
            if (ServerModule.LEVELS.isEnabled(member.getGuild())) {
                LevelInfo levelInfo = GeyserBot.storageManager.getLevel(member);

                if (levelInfo == null) {
                    infoDiv.addInnerElement(Element.p()
                            .addSafeInnerElement("Failed to load level data!"));
                } else {
                    infoDiv.addInnerElement(Element.p()
                            .addInnerElement("Level: ")
                            .addInnerElement(NUM_FORMAT.format(levelInfo.getLevel())));

                    infoDiv.addInnerElement(Element.p()
                            .addInnerElement("Experience: ")
                            .addInnerElement(NUM_FORMAT.format(levelInfo.getXp())));
                }
            }
        } else {
            OffsetDateTime earliestJoinTime = null;
            for (Guild guild : mutualGuilds) {
                if (guild.isMember(targetUser)) {
                    Member member = guild.getMember(targetUser);
                    if (!member.hasTimeJoined()) continue;

                    if (earliestJoinTime == null || earliestJoinTime.isAfter(member.getTimeJoined())) {
                        earliestJoinTime = member.getTimeJoined();
                    }
                }
            }

            if (earliestJoinTime != null) {
                infoDiv.addInnerElement(Element.p()
                        .addInnerElement("Earliest server join time: ")
                        .addInnerElement(Element.staticTimeDisplay(earliestJoinTime)));
            }
        }

        userDiv.addInnerElement(infoDiv);

        if (user != null && !user.getId().equals(targetUser.getId())) {
            Element permissionsRow = Element.div("flex flex-row gap-4");
            boolean hasPermissionsRow = false;

            if (PermissionManager.hasPermission(user, UserPermissions.MANAGE_ALL_PERMISSIONS)) {
                EnumSet<UserPermissions> set = EnumSet.noneOf(UserPermissions.class);
                set.addAll(List.of(GeyserBot.storageManager.getUserPermissions(targetUser)));
                permissionsRow.addInnerElement(new PostSelectList<>(
                        "Modify User Permissions",
                        "/api/user/%s/permissions/set".formatted(targetUser.getId()), "user-permissions",
                        UserPermissions.class, set,
                        """
                                window.messageBanner(data);
                                """,
                        true
                ));
                hasPermissionsRow = true;
            }
            if (guild != null &&
                    (
                            PermissionManager.hasPermission(
                                    guild.getMember(user),
                                    MemberRolePermissions.MANAGE_PERMISSIONS
                            ) ||
                                    PermissionManager.hasPermission(
                                            user,
                                            UserPermissions.MANAGE_ALL_PERMISSIONS
                                    )
                            )
            ) {
                EnumSet<MemberRolePermissions> set = EnumSet.noneOf(MemberRolePermissions.class);
                set.addAll(List.of(GeyserBot.storageManager.getMemberPermissions(guild.getMember(targetUser))));
                permissionsRow.addInnerElement(new PostSelectList<>(
                        "Modify Member Permissions",
                        "/api/guild/%s/member/%s/permissions/set".formatted(guild.getId(), targetUser.getId()), "member-permissions",
                        MemberRolePermissions.class, set,
                        """
                                window.messageBanner(data);
                                """,
                        true
                ));
                hasPermissionsRow = true;
            }

            Element ranksRow = Element.div("flex flex-row gap-4");
            boolean hasRanksRow = false;

            if (PermissionManager.hasPermission(user, UserPermissions.MANAGE_USER_RANKS) ||
                    PermissionManager.hasPermission(user, UserPermissions.MANAGE_ALL_PERMISSIONS)) {
                EnumSet<UserRank> set = EnumSet.noneOf(UserRank.class);
                set.addAll(List.of(GeyserBot.storageManager.getUserRanks(targetUser)));
                ranksRow.addInnerElement(new PostSelectList<>(
                        "Modify User Ranks",
                        "/api/user/%s/ranks/set".formatted(targetUser.getId()), "user-ranks",
                        UserRank.class, set,
                        """
                                window.messageBanner(data);
                                """,
                        true
                ));
                hasRanksRow = true;
            }
            if (guild != null &&
                    (
                            PermissionManager.hasPermission(
                                    guild.getMember(user),
                                    MemberRolePermissions.MANAGE_RANKS
                            ) ||
                                    PermissionManager.hasPermission(
                                            user,
                                            UserPermissions.MANAGE_ALL_PERMISSIONS
                                    )
                            )
            ) {
                EnumSet<MemberRank> set = EnumSet.noneOf(MemberRank.class);
                set.addAll(List.of(GeyserBot.storageManager.getMemberRanks(guild.getMember(targetUser))));
                ranksRow.addInnerElement(new PostSelectList<>(
                        "Modify Member Ranks",
                        "/api/guild/%s/member/%s/ranks/set".formatted(guild.getId(), targetUser.getId()), "member-ranks",
                        MemberRank.class, set,
                        """
                                window.messageBanner(data);
                                """,
                        true
                ));
                hasRanksRow = true;
            }

            if (hasPermissionsRow || hasRanksRow) {
                Element dropdownsDiv = Element.div("flex flex-col gap-2");
                if (hasPermissionsRow) dropdownsDiv.addInnerElement(permissionsRow);
                if (hasRanksRow) dropdownsDiv.addInnerElement(ranksRow);
                userDiv.addInnerElement(dropdownsDiv);
            }
        } else if (user != null && user.getId().equals(targetUser.getId())) {
            userDiv.addInnerElement(new PostRequestButton("Logout", "/logout", """
                    if (data.ok) {
                        window.location.href = window.location.protocol + '//' + window.location.host + '?logout';
                    } else {
                        alert(`Unable to logout, status code ${data.status}. Message: ${data.statusText}`);
                    }
                    """, "red", false)
                    .removeClass("flex-1")
                    .addClass("self-start"));
        }

        container.addInnerElement(userDiv);

        if (guild != null) {
            mutualGuilds.removeIf(g -> g.getId().equals(guild.getId()));
        }

        if (!mutualGuilds.isEmpty()) {
            Element otherGuildsDiv = Element.div("");

            otherGuildsDiv.addInnerElement(Element.of("p", "text-gray-700 dark:text-gray-300")
                    .addInnerElement(guild == null ? "In the following servers:" : "Also in:"));

            Element guildsDiv = Element.div("flex flex-row gap-2");

            for (Guild guild : mutualGuilds) {
                guildsDiv.addInnerElement(new ServerCard(guild, targetUser, true));
            }

            otherGuildsDiv.addInnerElement(guildsDiv);

            container.addInnerElement(otherGuildsDiv);
        }
    }
}
