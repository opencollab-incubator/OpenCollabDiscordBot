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

import net.dv8tion.jda.api.entities.*;
import org.geysermc.discordbot.http.html.Element;
import org.geysermc.discordbot.http.html.FAIcon;
import org.geysermc.discordbot.permissions.MemberRank;
import org.geysermc.discordbot.permissions.PermissionManager;
import org.geysermc.discordbot.permissions.UserRank;

public class Name extends Element {
    public Name(User user, Guild guild) {
        this(user, guild, true);
    }

    public Name(User user, Guild guild, boolean includeIcons) {
        this(
                user, guild, includeIcons,
                PermissionManager.getTopRank(user),
                guild == null || !guild.isMember(user) ? null : PermissionManager.getTopRank(guild.getMember(user))
        );
    }

    public Name(User user, Guild guild, boolean includeIcons, UserRank userRank, MemberRank memberRank) {
        super("a");

        if (guild != null) {
            this.addField("href", "/user/%s/%s".formatted(user.getId(), guild.getId()));
        } else {
            this.addField("href", "/user/%s".formatted(user.getId()));
        }

        Element span = Element.of("span");

        this.addField("title", user.getName());

        Element nameSpan = Element.of("span");

        if (guild != null && guild.isMember(user)) {
            Member member = guild.getMember(user);
            nameSpan.setColor(member.getColors().getPrimary(), true);

            nameSpan.addSafeInnerElement(member.getEffectiveName());
        } else {
            nameSpan.addSafeInnerElement(user.getEffectiveName());
        }

        span.addInnerElement(nameSpan);

        if (includeIcons) {
            Element rankSpan = Element.of("span");

            if (!(userRank == null && memberRank == null)) {
                boolean userShown = userRank != null;
                boolean memberShown = memberRank != null;
                if (userShown && memberShown) {
                    rankSpan.addInnerElement(new FAIcon(memberRank.getIcon())
                            .addField("title", memberRank.toString())
                            .setColor(memberRank.getColor()));
                    rankSpan.addInnerElement("<span> • </span>");
                    rankSpan.addInnerElement(new FAIcon(userRank.getIcon())
                            .addField("title", userRank.toString())
                            .setColor(userRank.getColor()));
                } else if (userShown) {
                    rankSpan.addInnerElement(new FAIcon(userRank.getIcon())
                            .addField("title", userRank.toString())
                            .setColor(userRank.getColor()));
                } else {
                    rankSpan.addInnerElement(new FAIcon(memberRank.getIcon())
                            .addField("title", memberRank.toString())
                            .setColor(memberRank.getColor()));
                }
            }

            span.addInnerElement(rankSpan);
        }

        this.addInnerElement(span);
    }
}
