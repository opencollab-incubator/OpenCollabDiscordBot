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
import org.geysermc.discordbot.http.html.Base;
import org.geysermc.discordbot.http.html.Element;
import org.geysermc.discordbot.http.html.FAIcon;
import org.geysermc.discordbot.http.pages.elements.ExpandingTextArea;
import org.geysermc.discordbot.storage.BanData;

public class AppealsCreate extends Base {
    private final BanData data;

    public AppealsCreate(User user, BanData data) {
        super(user);
        this.data = data;
    }

    @Override
    public void addContent(Element container) {
        container.addClass("flex flex-col gap-4 max-w-2xl");

        container.addInnerElement(Element.h2().addInnerElement("Submit appeal"));

        container.addInnerElement(
                Element.div("flex flex-col gap-1 rounded-xl bg-gray-200 dark:bg-gray-900 shadow-sm p-4")
                        .addInnerElement(
                                Element.of("h3", "text-sm font-semibold text-gray-700 dark:text-gray-300")
                                        .addInnerElement(new FAIcon("hammer"))
                                        .addInnerElement(" Ban reason")
                        )
                        .addInnerElement(Element.p().removeClass("text-gray-700").removeClass("dark:text-gray-300")
                                .addSafeInnerElement(data.reason()))
        );

        container.addInnerElement(Element.of("form")
                .addClass("flex flex-col gap-2")
                .addField("action", "/appeals/submit")
                .addField("method", "get")
                .addInnerElement(new ExpandingTextArea("appeal_message"))
                .addInnerElement(
                        Element.of("input")
                                .addClass("self-start rounded-md px-3 py-2 text-sm bg-blue-600 hover:bg-blue-700 text-white cursor-pointer")
                                .addField("type", "submit")
                                .addField("value", "Submit")
                ));
    }
}
