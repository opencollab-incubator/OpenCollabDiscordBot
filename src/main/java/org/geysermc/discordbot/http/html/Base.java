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

package org.geysermc.discordbot.http.html;

import net.dv8tion.jda.api.entities.User;

public abstract class Base extends HtmlPage {
    protected final User user;

    public Base(User user) {
        this.user = user;
    }

    @Override
    public void appendHead(Element head) {
        head.addInnerElement("<title>Open Collab Bot</title>")
                .addInnerElement("<script src=\"https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4\"></script>")
                .addInnerElement("<link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/font-awesome/7.3.0/css/all.min.css\" integrity=\"sha512-ApSLB1Pd3/bZN8fWB/RG9YhN/7bd9Hkf3AGaE2mPfebjrxagjuBtx2GcgdqIlJkUzwylBo61r9Xa9NmgBI0swA==\" crossorigin=\"anonymous\" referrerpolicy=\"no-referrer\" />");
    }

    @Override
    public void appendBody(Element body) {
        Element divContainer = Element.of("div")
                .addClass("container mx-auto");

        addContent(divContainer);

        Element mainContainer = Element.of("div")
                .addClass("flex flex-col h-screen")
                .addInnerElement(
                        Element.of("header")
                                .addClass("bg-gray-200 dark:bg-gray-900 text-black dark:text-white p-4")
                                .addInnerElement(
                                        Element.of("div")
                                                .addClass("container mx-auto flex justify-between items-center")
                                                .addInnerElement("<h1 class=\"text-2xl font-bold\">Open Collab Bot</h1>")
                                                .addInnerElement(
                                                        Element.of("nav")
                                                                .addClass("flex gap-4 items-center")
                                                                .addInnerElement("<a href=\"/\" class=\"hover:text-blue-200\">Home</a>")
                                                                .addInnerElement("<a href=\"/appeals\" class=\"hover:text-blue-200\">Appeals</a>")
                                                                .addInnerElement(Header.ELEMENT)
                                                )
                                )
                )
                .addInnerElement(
                        Element.of("main")
                                .addClass("flex-1 bg-gray-100 dark:bg-gray-800 text-black dark:text-white p-8 overflow-y-auto")
                                .addInnerElement(divContainer)
                );

        this.addInnerElement(mainContainer);
    }

    public abstract void addContent(Element container);
}
