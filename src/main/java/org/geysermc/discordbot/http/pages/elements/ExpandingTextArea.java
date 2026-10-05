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

public class ExpandingTextArea extends Element {
    public ExpandingTextArea(String name) {
        super("div");

        this.addClass("grid text-sm after:px-3.5 after:py-2.5 [&>textarea]:text-inherit " +
                "after:text-inherit [&>textarea]:resize-none [&>textarea]:overflow-hidden " +
                "[&>textarea]:[grid-area:1/1/2/2] after:[grid-area:1/1/2/2] after:whitespace-pre-wrap " +
                "after:invisible after:content-[attr(data-cloned-val)_'_'] after:border");

        this.addField("data-cloned-val");

        Element textarea = Element.of("textarea");

        textarea.addClass("w-full bg-gray-200 dark:bg-gray-900 border border-transparent hover:border-slate-200")
                .addClass("appearance-none rounded px-3.5 py-2.5 outline-none")
                .addClass("focus:bg-gray-100 focus:dark:bg-gray-950 focus:border-indigo-400 focus:ring-2 focus:ring-indigo-100");

        textarea.addField("id", name)
                .addField("name", name)
                .addField("rows", "2")
                .addField("onInput", "this.parentNode.dataset.clonedVal = this.value")
                .addField("onLoad", "document.getElementById('%s').value = 'e'".formatted(name))
                .addField("placeholder", "Enter message here...")
                .addField("required");

        this.addInnerElement(textarea);
    }
}
