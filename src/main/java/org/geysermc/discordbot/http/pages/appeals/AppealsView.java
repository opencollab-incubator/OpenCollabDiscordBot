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

package org.geysermc.discordbot.http.pages.appeals;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import org.geysermc.discordbot.GeyserBot;
import org.geysermc.discordbot.http.html.Base;
import org.geysermc.discordbot.http.html.Element;
import org.geysermc.discordbot.http.html.FAIcon;
import org.geysermc.discordbot.http.pages.elements.AppealCard;
import org.geysermc.discordbot.http.pages.elements.BanCard;
import org.geysermc.discordbot.permissions.PermissionManager;
import org.geysermc.discordbot.permissions.UserPermissions;
import org.geysermc.discordbot.storage.Appeal;
import org.geysermc.discordbot.storage.BanData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AppealsView extends Base {
    public AppealsView(User user) {
        super(user);
    }

    @Override
    public void addContent(Element container) {
        container.addInnerElement(Element.of("div")
                .addClass("opacity-0 transition-opacity rounded-xl p-2 mb-4")
                .addField("id", "message-element"));

        BanData data = GeyserBot.storageManager.getBan(user.getIdLong());
        List<Appeal> appeals = GeyserBot.storageManager.getAppeals(user, 5);

        if (data == null && appeals.isEmpty()) {
            if (PermissionManager.hasPermission(user, UserPermissions.VIEW_APPEALS)) {
                addModerationContent(container);
                return;
            }

            container.addClass("flex flex-col items-center text-center gap-2 mt-16");
            container.addInnerElement(new FAIcon("circle-check")
                    .addClass("text-5xl text-green-500 dark:text-green-400 mb-2"));
            container.addInnerElement(Element.h2().removeClass("mb-4")
                    .addInnerElement("You have no bans active currently."));
            container.addInnerElement(Element.p()
                    .addInnerElement("Good for you! You should be proud :)"));

            return;
        }

        boolean hasPendingAppeal = false;

        for (Appeal appeal : appeals) {
            if (appeal.state().equals(Appeal.State.PENDING)) {
                hasPendingAppeal = true;
                break;
            }
        }

        if (data != null) {
            container.addInnerElement(Element.h2().addInnerElement("Ban:"));

            container.addInnerElement(new BanCard(data, hasPendingAppeal));
        }

        if (!appeals.isEmpty()) {
            container.addInnerElement(Element.h2().addInnerElement("Your appeals:"));

            for (Appeal appeal : appeals) {
                container.addInnerElement(new AppealCard(appeal, user, false));
            }
        }
    }

    private void addModerationContent(Element container) {
        List<Appeal> appeals = GeyserBot.storageManager.getAppeals(25);
        Map<UserSnowflake, Appeal> pendingAppeals = new LinkedHashMap<>();

        for (Appeal appeal : appeals) {
            if (pendingAppeals.containsKey(appeal.user())) continue;
            if (!appeal.state().equals(Appeal.State.PENDING)) continue;

            pendingAppeals.put(appeal.user(), appeal);
        }

        container.addInnerElement(Element.h2().addInnerElement("Active Appeals:"));

        if (pendingAppeals.isEmpty()) {
            container.addInnerElement(Element.p()
                    .addInnerElement("There are no pending appeals right now."));
            return;
        }

        Element appealsContainer = Element.div("grid gap-4 justify-center");
        appealsContainer.addField("style", "grid-template-columns: 0.5fr 0.5fr;");

        for (Map.Entry<UserSnowflake, Appeal> entry : pendingAppeals.entrySet()) {
            User user = GeyserBot.getJDA().retrieveUserById(entry.getKey().getIdLong()).complete();
            Element appeal = Element.of("div");
            appeal.addInnerElement(
                    Element.of("span")
                            .addInnerElement(Element.img(
                                    user.getEffectiveAvatarUrl(), "User Profile Picture",
                                    "w-16 h-16 rounded-lg mb-2"
                            ))
            );

            appeal.addInnerElement(new AppealCard(entry.getValue(), user, true));

            appealsContainer.addInnerElement(appeal);
        }

        container.addInnerElement(appealsContainer);
    }
}
