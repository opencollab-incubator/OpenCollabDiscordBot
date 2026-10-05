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

import net.dv8tion.jda.api.entities.channel.concrete.*;
import org.geysermc.discordbot.http.html.Element;
import org.geysermc.discordbot.http.html.FAIcon;

import java.util.*;

public class PostSelectList<E extends Enum<E>> extends Element {
    public PostSelectList(String display, String postUrl, String id, Class<E> enumClass, EnumSet<E> selectedValues, String onRetrieve, boolean isMultiple) {
        super("details");

        this.addField("id", id);
        this.addClass("relative inline-block w-64");

        Element labelSpan = Element.of("span")
                .addClass("select-label truncate");
        labelSpan.addSafeInnerElement(display);

        Element summary = Element.of("summary")
                .addClass("flex cursor-pointer list-none items-center justify-between gap-2 rounded-md border " +
                        "border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-900 px-3 py-2 text-sm " +
                        "text-black dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-600")
                .addInnerElement(labelSpan)
                .addInnerElement(new FAIcon("chevron-down").addClass("text-xs text-gray-500 dark:text-gray-400"));

        this.addInnerElement(summary);

        Element menu = Element.div("absolute z-10 mt-1 max-h-60 w-full overflow-y-auto rounded-md border " +
                "border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-900 shadow-lg");

        Map<E, Element> options = new LinkedHashMap<>();

        for (E e : enumClass.getEnumConstants()) {
            options.put(e, Element.p().addSafeInnerElement(e.toString()));
        }

        for (Map.Entry<E, Element> option : options.entrySet()) {
            Element checkbox = Element.of("input")
                    .addField("type", "checkbox")
                    .addField("name", id)
                    .addField("value", option.getKey().name())
                    .addField("data-label", option.getValue())
                    .addClass("select-checkbox h-4 w-4 rounded border-gray-300 dark:border-gray-700 " +
                            "text-blue-600 focus:ring-blue-600");

            if (selectedValues.contains(option.getKey())) checkbox.addField("checked", "checked");

            Element optionLabel = Element.of("label")
                    .addClass("flex cursor-pointer items-center gap-2 px-3 py-2 text-sm hover:bg-gray-100 dark:hover:bg-gray-800")
                    .addInnerElement(checkbox)
                    .addInnerElement(Element.of("span").addInnerElement(option.getValue()));

            menu.addInnerElement(optionLabel);
        }

        this.addInnerElement(menu);

        if (isMultiple) {
            this.addInnerElement(Element.script("""
                (function () {
                  const root = document.getElementById(%s);
                  if (!root) return;

                  const checkboxes = Array.from(root.querySelectorAll(".select-checkbox"));

                  function updateLabel() {
                    const checked = checkboxes.filter(cb => cb.checked);
                
                    const selectedElements = [];
                
                    checked.forEach(cb => selectedElements.push(cb.value));
                
                    fetch(window.location.origin + %s, {
                        method: 'POST',
                        body: JSON.stringify({selectedElements}),
                        headers: {
                            'Accept': 'application/json',
                            'Content-Type': 'application/json'
                        }
                    })
                    .then(response => response.json())
                    .then(data => {
                        %s
                    });
                  }

                  checkboxes.forEach(cb => cb.addEventListener("change", updateLabel));
                })();
                """.formatted(jsString(id, false), jsString(postUrl, false), onRetrieve)));
        } else {
            this.addInnerElement(Element.script("""
                (function () {
                  const root = document.getElementById(%s);
                  if (!root) return;
                
                  const checkboxes = Array.from(root.querySelectorAll(".select-checkbox"));
                
                  let selectedElement;
                
                  function updateLabel(cb) {
                    checkboxes.forEach(checkbox => {
                        if (checkbox !== cb) {
                            checkbox.checked = false;
                            checkbox.enabled = true;
                        } else {
                            checkbox.enabled = false;
                            selectedElement = checkbox.value;
                        }
                    });
                
                    fetch(window.location.origin + %s, {
                        method: 'POST',
                        body: JSON.stringify({selectedElement}),
                        headers: {
                            'Accept': 'application/json',
                            'Content-Type': 'application/json'
                        }
                    })
                    .then(response => response.json())
                    .then(data => {
                        %s
                    });
                  }

                  checkboxes.forEach(cb => cb.addEventListener("change", () => updateLabel(cb)));
                })();
                """.formatted(jsString(id, false), jsString(postUrl, false), onRetrieve)));
        }

        this.addInnerElement(Element.script("""
                document.addEventListener("click", (event) => {
                    const element = document.getElementById(%s);
                    if (!element.contains(event.target)) {
                        element.open = false;
                    }
                });
                """.formatted(jsString(id, false))));
    }
}
