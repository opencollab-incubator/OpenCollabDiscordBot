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

import org.geysermc.discordbot.http.html.Element;
import org.geysermc.discordbot.http.html.FAIcon;
import org.geysermc.discordbot.storage.BanData;

public class BanCard extends Element {
    public BanCard(BanData banData, boolean hasPendingAppeal) {
        super("div");

        Element expiresOn = Element.of("p")
                .addSafeInnerElement("Expires ");
        if (banData.expiryTime() == null) {
            expiresOn.addSafeInnerElement("Never");
        } else {
            expiresOn.addInnerElement(Element.dynamicTimeDisplay(banData.expiryTime()));
        }

        this.addClass("flex grow max-w-md flex-col overflow-hidden rounded-xl bg-gray-200 dark:bg-gray-900 shadow-sm mb-2");
        this.addInnerElement(
                Element.of("div")
                        .addClass("flex items-stretch gap-4 p-4")
                        .addInnerElement(
                                Element.div("flex min-w-0 flex-col justify-center")
                                        .addInnerElement(
                                                Element.of("h3", "mb-2 text-lg font-semibold")
                                                        .addInnerElement(new FAIcon("hammer"))
                                                        .addInnerElement(" Active ban")
                                        )
                                        .addInnerElement(
                                                Element.of("p", "text-sm leading-relaxed text-gray-700 dark:text-gray-300")
                                                        .addInnerElement(Element.of("p")
                                                                .addSafeInnerElement("Issued On ")
                                                                .addInnerElement(Element.of("span")
                                                                        .addClass("time-display")
                                                                        .addField("data-time", String.valueOf(banData.issuedOn().getEpochSecond()))
                                                                        .addSafeInnerElement("Loading...")))
                                                        .addInnerElement(expiresOn)
                                        )
                        )
        );

        if (!hasPendingAppeal) {
            Element buttonContainer = Element.div("flex gap-2 px-4 pb-4");

            buttonContainer.addInnerElement(new LinkButton("Appeal", "/appeals/create"));

            this.addInnerElement(buttonContainer);
        }
    }
}
