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
import org.geysermc.discordbot.http.pages.elements.ServerCard;

public class Index extends Base {
    private final Element informationElement;

    public Index(User user, String loginError, String logout) {
        super(user);

        if (loginError != null) {
            informationElement = Element.of("div", "bg-red-200 dark:bg-red-900 border-red-300 dark:border-red-800 rounded-xl")
                    .addInnerElement("<p>An error occurred while logging in.</p>");
        } else if (logout != null) {
            informationElement = Element.of("div", "bg-blue-200 dark:bg-blue-900 border-blue-300 dark:border-blue-800 rounded-xl")
                    .addInnerElement("<p>Successfully logged out.</p>");
        } else {
            informationElement = null;
        }
    }

    @Override
    public void addContent(Element container) {
        if (informationElement != null) container.addInnerElement(informationElement);
        container.addInnerElement("<h2 class=\"text-3xl font-bold mb-4\">Servers</h2>");

        Element serverContainer = Element.div("flex flex-row gap-4");

        for (Guild guild : GeyserBot.getJDA().getGuilds()) {
            serverContainer.addInnerElement(new ServerCard(guild, user));
        }

        container.addInnerElement(serverContainer);
    }
}
