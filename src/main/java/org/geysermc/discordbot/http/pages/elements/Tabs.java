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

import java.util.List;

/**
 * A simple client-side tab switcher. Every tab's content is rendered up front; a small inline
 * script toggles which panel is visible, with the first tab active by default.
 */
public class Tabs extends Element {
    private static final String ACTIVE_BUTTON_CLASSES = "border-blue-600 text-blue-600 dark:text-blue-400";
    private static final String INACTIVE_BUTTON_CLASSES = "border-transparent text-gray-500 dark:text-gray-400 hover:text-black dark:hover:text-white";

    public record Tab(String id, String label, Element content) {
    }

    public Tabs(String groupId, List<Tab> tabs) {
        super("div");
        this.addField("id", groupId);

        Element tabList = Element.div("flex gap-4 border-b border-gray-300 dark:border-gray-700 mb-4");
        Element panels = Element.of("div");

        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            boolean active = i == 0;

            tabList.addInnerElement(
                    Element.of("button")
                            .addField("type", "button")
                            .addField("data-tab-target", tab.id())
                            .addClass("tab-button px-1 pb-2 text-sm font-semibold border-b-2 -mb-px cursor-pointer")
                            .addClass(active ? ACTIVE_BUTTON_CLASSES : INACTIVE_BUTTON_CLASSES)
                            .addSafeInnerElement(tab.label())
            );

            Element panel = Element.div("tab-panel")
                    .addField("data-tab-panel", tab.id())
                    .addInnerElement(tab.content());

            if (!active) panel.addClass("hidden");

            panels.addInnerElement(panel);
        }

        this.addInnerElement(tabList);
        this.addInnerElement(panels);

        this.addInnerElement(Element.script("""
                (function () {
                    const root = document.getElementById(%s);
                    if (!root) return;

                    const activeClasses = %s.split(" ");
                    const inactiveClasses = %s.split(" ");

                    const buttons = Array.from(root.querySelectorAll(".tab-button"));
                    const panels = Array.from(root.querySelectorAll(".tab-panel"));

                    buttons.forEach(button => button.addEventListener("click", () => {
                        const target = button.dataset.tabTarget;

                        buttons.forEach(b => {
                            const isActive = b === button;
                            b.classList.remove(...activeClasses, ...inactiveClasses);
                            b.classList.add(...(isActive ? activeClasses : inactiveClasses));
                        });

                        panels.forEach(panel => panel.classList.toggle("hidden", panel.dataset.tabPanel !== target));
                    }));
                })();
                """.formatted(
                jsString(groupId, false),
                jsString(ACTIVE_BUTTON_CLASSES),
                jsString(INACTIVE_BUTTON_CLASSES)
        )));
    }
}
