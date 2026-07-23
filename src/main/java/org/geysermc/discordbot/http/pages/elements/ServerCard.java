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
import net.dv8tion.jda.api.entities.User;
import org.geysermc.discordbot.http.html.Element;
import org.geysermc.discordbot.permissions.MemberRolePermissions;
import org.geysermc.discordbot.permissions.PermissionManager;
import org.geysermc.discordbot.permissions.UserPermissions;
import org.geysermc.discordbot.storage.ServerModule;

public class ServerCard extends Element {
    public ServerCard(Guild guild, User user) {
        super("div");

        String iconId = guild.getIconId();
        if (iconId != null && iconId.startsWith("a_")) iconId = iconId.substring(2);
        String iconUrl = (iconId == null)
                ? "https://cdn.discordapp.com/embed/avatars/1.png"
                : String.format(Guild.ICON_URL, guild.getId(), iconId, "png");

        this.addClass("flex grow max-w-md flex-col overflow-hidden rounded-xl bg-gray-200 dark:bg-gray-900 shadow-sm");
        this.addInnerElement(
                Element.of("div")
                        .addClass("flex items-stretch gap-4 p-4")
                        .addInnerElement(Element.img(
                                iconUrl, "Server Profile Picture",
                                "h-full w-32 flex-shrink-0 rounded-lg object-cover"
                        ))
                        .addInnerElement(
                                Element.div("flex min-w-0 flex-col justify-center")
                                        .addInnerElement(
                                                Element.of("h3", "mb-2 text-lg font-semibold")
                                                        .addInnerElement(guild.getName())
                                        )
                                        .addInnerElement(
                                                Element.of("p", "text-sm leading-relaxed text-gray-700 dark:text-gray-300")
                                                        .addInnerElement("Serving " + guild.getMemberCount() + " members")
                                        )
                        )
        );

        boolean leaderboard = ServerModule.LEVELS.isEnabled(guild);
        boolean configure = false;
        if (user != null) {
            configure = PermissionManager.hasPermission(user, UserPermissions.CONFIGURE_ALL_SERVERS);

            if (guild.isMember(user)) {
                configure = configure || PermissionManager.hasPermission(guild.getMember(user), MemberRolePermissions.CONFIGURE_SERVER);
            }
        }

        if (leaderboard || configure) {
            Element buttonContainer = Element.div("flex gap-2 px-4 pb-4");

            if (leaderboard) {
                buttonContainer.addInnerElement(makeButton(guild, "leaderboard", "Leaderboard"));
            }

            if (configure) {
                buttonContainer.addInnerElement(makeButton(guild, "configure", "Configure"));
            }

            this.addInnerElement(buttonContainer);
        }
    }

    private Element makeButton(Guild guild, String type, String text) {
        return Element.of("a", "flex-1 rounded-md border px-3 py-2 text-sm bg-blue-600 hover:bg-blue-700 text-white")
                .addField("href", "/" + guild.getId() + "/" + type)
                .addInnerElement(text);
    }
}
