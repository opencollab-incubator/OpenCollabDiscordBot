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

package org.geysermc.discordbot.http.pages;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import org.geysermc.discordbot.GeyserBot;
import org.geysermc.discordbot.http.html.Base;
import org.geysermc.discordbot.http.html.Element;
import org.geysermc.discordbot.http.html.FAIcon;
import org.geysermc.discordbot.http.pages.elements.ServerCard;
import org.geysermc.discordbot.util.BotServers;

import java.util.ArrayList;
import java.util.List;

public class Index extends Base {
    private final Element informationElement;

    public Index(User user, String loginError, String logout, String information) {
        super(user);

        if (loginError != null) {
            informationElement = banner(
                    "bg-red-200 dark:bg-red-900 border-2 border-red-300 dark:border-red-800",
                    "triangle-exclamation",
                    "An error occurred while logging in: " + loginError + "."
            );
        } else if (logout != null) {
            informationElement = banner(
                    "bg-blue-200 dark:bg-blue-900 border-2 border-blue-300 dark:border-blue-800",
                    "circle-info",
                    "Successfully logged out."
            );
        } else if (information != null) {
            informationElement = banner(
                    "bg-green-200 dark:bg-green-900 border-2 border-green-300 dark:border-green-800",
                    "circle-check",
                    information
            );
        } else {
            informationElement = null;
        }
    }

    private static Element banner(String colorClasses, String icon, String text) {
        return Element.div("rounded-xl p-4 mb-4 flex items-center gap-3")
                .addClass(colorClasses)
                .addInnerElement(new FAIcon(icon))
                .addInnerElement(Element.p().removeClass("text-gray-700").removeClass("dark:text-gray-300")
                        .addSafeInnerElement(text));
    }

    @Override
    public void addContent(Element container) {
        if (informationElement != null) container.addInnerElement(informationElement);
        container.addInnerElement(Element.h2().addInnerElement("Servers"));

        List<Guild> guilds = new ArrayList<>();
        for (Guild guild : GeyserBot.getJDA().getGuilds()) {
            if (BotServers.isHidden(guild, user)) continue;
            guilds.add(guild);
        }

        if (guilds.isEmpty()) {
            container.addInnerElement(
                    Element.div("flex flex-col items-center text-center gap-2 mt-8")
                            .addInnerElement(new FAIcon("server")
                                    .addClass("text-5xl text-gray-400 dark:text-gray-600 mb-2"))
                            .addInnerElement(Element.p().addInnerElement("No servers to show here yet."))
            );
            return;
        }

        Element serverContainer = Element.div("grid gap-4 justify-center");
        serverContainer.addField("style", "grid-template-columns: repeat(auto-fill, minmax(360px, 0.33fr));");

        for (Guild guild : guilds) {
            serverContainer.addInnerElement(new ServerCard(guild, user));
        }

        container.addInnerElement(serverContainer);
    }
}
