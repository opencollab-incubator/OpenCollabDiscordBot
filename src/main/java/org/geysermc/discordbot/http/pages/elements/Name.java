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
import org.geysermc.discordbot.GeyserBot;
import org.geysermc.discordbot.http.html.Element;
import org.geysermc.discordbot.http.html.FAIcon;
import org.geysermc.discordbot.http.html.TooltipElement;
import org.geysermc.discordbot.permissions.MemberRank;
import org.geysermc.discordbot.permissions.UserRank;

public class Name extends Element {
    public Name(User user, Guild guild) {
        super("span");

        this.addField("title", user.getName());

        Element nameSpan = Element.of("span");

        if (guild != null && guild.isMember(user)) {
            Member member = guild.getMember(user);
            nameSpan.setColor(member.getColors().getPrimary(), true);

            if (member.getNickname() == null && member.getUser().getGlobalName() == null) {
                nameSpan.addSafeInnerElement(user.getName());
            } else if (member.getNickname() == null) {
                nameSpan.addSafeInnerElement(user.getGlobalName());
            } else {
                nameSpan.addSafeInnerElement(member.getEffectiveName());
            }
        } else if (user.getGlobalName() == null) {
            nameSpan.addSafeInnerElement(user.getName());
        } else {
            nameSpan.addSafeInnerElement(user.getGlobalName());
        }

        this.addInnerElement(nameSpan);

        setVisualRank(user, guild);
    }

    public void setVisualRank(User user, Guild guild) {
        MemberRank memberRank = MemberRank.MEMBER;
        if (guild.isMember(user)) {
            memberRank = GeyserBot.storageManager.getMemberRank(guild.getMember(user));
        }

        UserRank userRank = GeyserBot.storageManager.getUserRank(user);

        Element span = Element.of("span");

        if (userRank.equals(UserRank.NONE)) {
            if (memberRank.equals(MemberRank.MEMBER)) return;
            span.addInnerElement("<span> </span>");
            span.addInnerElement(new TooltipElement(
                    new FAIcon(memberRank.getIcon()).setColor(memberRank.getColor()),
                    memberRank.toString()
            ));
            span.addInnerElement(new FAIcon(memberRank.getIcon())
                    .setColor(memberRank.getColor()));
        } else {
            span.addInnerElement("<span> </span>");
            span.addInnerElement(new TooltipElement(
                            new FAIcon(memberRank.getIcon()).setColor(memberRank.getColor()),
                            memberRank.toString()
                    ));
            span.addInnerElement("<span> • </span>");
            span.addInnerElement(new TooltipElement(
                    new FAIcon(userRank.getIcon()).setColor(userRank.getColor()),
                    userRank.toString()
            ));
        }

        this.addInnerElement(span);
    }
}
