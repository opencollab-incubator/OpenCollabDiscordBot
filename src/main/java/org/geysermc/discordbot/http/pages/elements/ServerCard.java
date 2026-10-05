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

package org.geysermc.discordbot.http.pages.elements;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.geysermc.discordbot.http.html.Element;
import org.geysermc.discordbot.http.html.FAIcon;
import org.geysermc.discordbot.permissions.MemberRolePermissions;
import org.geysermc.discordbot.permissions.PermissionManager;
import org.geysermc.discordbot.permissions.UserPermissions;
import org.geysermc.discordbot.storage.ServerModule;
import org.springframework.web.util.HtmlUtils;

public class ServerCard extends Element {
    /**
     * Fallback banner gradients for servers without a Discord banner image, picked per-guild so
     * a page full of cards doesn't look monotone.
     */
    private static final String[] GRADIENTS = {
            "from-blue-600 to-indigo-600",
            "from-purple-600 to-pink-600",
            "from-emerald-600 to-teal-600",
            "from-orange-500 to-red-600",
            "from-cyan-600 to-blue-600",
            "from-fuchsia-600 to-purple-600",
    };

    public ServerCard(Guild guild, User user) {
        this(guild, user, false);
    }

    public ServerCard(Guild guild, User user, boolean userProfile) {
        super(userProfile ? "a" : "div");

        if (userProfile) {
            this.addField("href", "/user/%s/%s".formatted(user.getId(), guild.getId()));
        }

        String iconUrl = iconUrl(guild);

        if (userProfile) {
            buildCompact(guild, iconUrl);
        } else {
            buildFull(guild, user, iconUrl);
        }
    }

    /**
     * The small horizontal chip used in "also in these servers" lists (e.g. a user's profile).
     */
    private void buildCompact(Guild guild, String iconUrl) {
        this.addClass("flex items-center gap-3 rounded-xl bg-gray-200 dark:bg-gray-900 shadow-sm p-3 " +
                "transition hover:shadow-md hover:ring-2 hover:ring-blue-500");

        this.addInnerElement(Element.img(iconUrl, "Server Profile Picture", "w-10 h-10 rounded-full object-cover flex-shrink-0"));

        this.addInnerElement(
                Element.of("h3", "min-w-0 flex-1 truncate text-base font-semibold")
                        .addField("title", HtmlUtils.htmlEscape(guild.getName()))
                        .addSafeInnerElement(guild.getName())
        );

        this.addInnerElement(new FAIcon("chevron-right").addClass("text-gray-400 dark:text-gray-600"));
    }

    /**
     * The full card used on the home page: a banner header with the guild icon overlapping it,
     * name, member count and module action buttons.
     */
    private void buildFull(Guild guild, User user, String iconUrl) {
        this.addClass("grow max-w-md flex flex-col overflow-hidden rounded-xl bg-gray-200 dark:bg-gray-900 " +
                "shadow-sm transition duration-200 hover:shadow-lg hover:-translate-y-0.5");

        Element banner = Element.div("h-20 w-full bg-gradient-to-r " + gradientFor(guild));
        String bannerUrl = guild.getBannerUrl();
        if (bannerUrl != null) {
            banner.addField("style", "background-image:url('%s');background-size:cover;background-position:center;".formatted(bannerUrl));
        }
        this.addInnerElement(banner);

        this.addInnerElement(
                Element.div("px-4 -mt-8")
                        .addInnerElement(Element.img(iconUrl, "Server Profile Picture",
                                "w-16 h-16 rounded-full object-cover ring-4 ring-gray-200 dark:ring-gray-900 bg-gray-200 dark:bg-gray-900"))
        );

        this.addInnerElement(
                Element.div("flex flex-col gap-1 px-4 pt-2 pb-4")
                        .addInnerElement(Element.of("h3", "text-lg font-semibold truncate")
                                .addField("title", HtmlUtils.htmlEscape(guild.getName()))
                                .addSafeInnerElement(guild.getName()))
                        .addInnerElement(Element.of("p", "flex items-center gap-1.5 text-sm text-gray-700 dark:text-gray-300")
                                .addInnerElement(new FAIcon("users").addClass("text-xs"))
                                .addInnerElement("Serving " + guild.getMemberCount() + (guild.getMemberCount() == 1 ? " member" : " members")))
        );

        addButtons(guild, user);
    }

    private static String iconUrl(Guild guild) {
        String iconId = guild.getIconId();
        if (iconId != null && iconId.startsWith("a_")) iconId = iconId.substring(2);
        return (iconId == null)
                ? "https://cdn.discordapp.com/embed/avatars/1.png"
                : String.format(Guild.ICON_URL, guild.getId(), iconId, "png");
    }

    private static String gradientFor(Guild guild) {
        return GRADIENTS[Math.floorMod(guild.getIdLong(), GRADIENTS.length)];
    }

    private void addButtons(Guild guild, User user) {
        boolean leaderboard = ServerModule.LEVELS.isEnabled(guild);
        boolean configure = false;
        boolean tickets = ServerModule.TICKETS.isEnabled(guild);
        if (user != null) {
            configure = PermissionManager.hasPermission(user, UserPermissions.CONFIGURE_ALL_SERVERS);

            if (guild.isMember(user)) {
                Member member = guild.getMember(user);
                configure = configure || PermissionManager.hasPermission(member, MemberRolePermissions.CONFIGURE_SERVER);
                tickets = tickets && PermissionManager.hasPermission(member, MemberRolePermissions.VIEW_TICKETS);
            }
        }

        if (!leaderboard && !configure && !tickets) return;

        Element buttonContainer = Element.div("flex flex-wrap gap-2 px-4 pb-4");

        if (leaderboard) {
            buttonContainer.addInnerElement(makeButton(guild, "leaderboard", "Leaderboard"));
        }

        if (configure) {
            buttonContainer.addInnerElement(makeButton(guild, "configure", "Configure"));
        }

        if (tickets) {
            buttonContainer.addInnerElement(makeButton(guild, "tickets", "Tickets"));
        }

        this.addInnerElement(buttonContainer);
    }

    private Element makeButton(Guild guild, String type, String text) {
        return new LinkButton(text, "/" + guild.getId() + "/" + type);
    }
}
