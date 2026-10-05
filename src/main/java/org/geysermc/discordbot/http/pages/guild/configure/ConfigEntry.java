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

package org.geysermc.discordbot.http.pages.guild.configure;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import org.geysermc.discordbot.http.html.Element;
import org.geysermc.discordbot.http.html.FAIcon;
import org.geysermc.discordbot.storage.ServerConfiguration;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConfigEntry extends Element {
    public ConfigEntry(ServerConfiguration.Configuration<?> configuration, Guild guild, String onEditFunctionName) {
        super(configuration.getType() == String.class ? "div" : "details");

        String key = configuration.getKey() + "-stringlist-";

        String INPUT_CLASS =
                key + "input flex-1 rounded-md border border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-900 " +
                        "px-3 py-2 text-sm text-black dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-600";

        String REMOVE_CLASS =
                key + "remove rounded-md border border-gray-300 dark:border-gray-700 px-2 py-2 text-sm " +
                        "text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/30";

        boolean isMultiple = configuration.getConfigType().equals(ServerConfiguration.ConfigType.MULTIPLE);

        if (configuration.getType() != String.class) {
            String id = "multiselect-" + configuration.getKey();

            this.addField("id", id);
            this.addClass("relative inline-block w-full");

            Element labelSpan = Element.of("span")
                    .addClass("multiselect-label truncate")
                    .addField("data-placeholder", isMultiple ? "Select Options" : "Select Option");
            labelSpan.addSafeInnerElement(isMultiple ? "Select Options" : "Select Option");

            Element summary = Element.of("summary")
                    .addClass("flex cursor-pointer list-none items-center justify-between gap-2 rounded-md border " +
                            "border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-900 px-3 py-2 text-sm " +
                            "text-black dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-600")
                    .addInnerElement(labelSpan)
                    .addInnerElement(new FAIcon("chevron-down").addClass("text-xs text-gray-500 dark:text-gray-400"));

            this.addInnerElement(summary);

            Element menu = Element.div("absolute z-10 mt-1 max-h-60 w-full overflow-y-auto rounded-md border " +
                    "border-gray-300 dark:border-gray-700 bg-white dark:bg-gray-900 shadow-lg");

            Map<String, Element> options = new LinkedHashMap<>();

            List<String> selectedIds = new ArrayList<>();

            if (configuration.getType() == Role.class) {
                guild.getRoles().forEach(c -> {
                    Element element = Element.of("span");
                    element.setColor(c.getColors().getPrimary());
                    element.addSafeInnerElement(c.getName());
                    options.put(c.getId(), element);
                });

                if (isMultiple) {
                    List<Role> selectedRoles = (List<Role>) configuration.get(guild);
                    if (selectedRoles != null) selectedIds.addAll(selectedRoles.stream().map(Role::getId).toList());
                } else {
                    Role selectedRole = (Role) configuration.get(guild);
                    if (selectedRole != null) selectedIds.add(selectedRole.getId());
                }
            } else if (GuildChannel.class.isAssignableFrom(configuration.getType())) {
                List<GuildChannel> channels = new ArrayList<>();
                for (GuildChannel c : guild.getChannels()) {
                    if (configuration.getType().isAssignableFrom(c.getClass())) channels.add(c);
                }
                channels.forEach(c -> {
                    Element element = Element.of("span");
                    if (c instanceof TextChannel) {
                        element.addInnerElement(new FAIcon("hashtag"))
                                .addInnerElement(" ");
                    } else if (c instanceof VoiceChannel) {
                        element.addInnerElement(new FAIcon("volume-high"))
                                .addInnerElement(" ");
                    } else if (c instanceof NewsChannel) {
                        element.addInnerElement(new FAIcon("bullhorn"))
                                .addInnerElement(" ");
                    } else if (c instanceof ForumChannel) {
                        element.addInnerElement(new FAIcon("comment"))
                                .addInnerElement(" ");
                    } else if (c instanceof Category) {
                        element.addInnerElement(new FAIcon("square-caret-down"))
                                .addInnerElement(" ");
                    } else if (c instanceof StageChannel) {
                        element.addInnerElement(new FAIcon("users"))
                                .addInnerElement(" ");
                    } else if (c instanceof MediaChannel) {
                        element.addInnerElement(new FAIcon("images"))
                                .addInnerElement(" ");
                    } else if (guild.getRulesChannel() != null && c.getId().equals(guild.getRulesChannel().getId())) {
                        element.addInnerElement(new FAIcon("book"))
                                .addInnerElement(" ");
                    }
                    element.addSafeInnerElement(c.getName());
                    options.put(c.getId(), element);
                });

                if (isMultiple) {
                    List<GuildChannel> selectedChannels = (List<GuildChannel>) configuration.get(guild);
                    if (selectedChannels != null) selectedIds.addAll(selectedChannels.stream().map(GuildChannel::getId).toList());
                } else {
                    GuildChannel selectedChannel = (GuildChannel) configuration.get(guild);
                    if (selectedChannel != null) selectedIds.add(selectedChannel.getId());
                }
            }

            for (Map.Entry<String, Element> option : options.entrySet()) {
                Element checkbox = Element.of("input")
                        .addField("type", "checkbox")
                        .addField("name", configuration.getName())
                        .addField("value", HtmlUtils.htmlEscape(option.getKey()))
                        .addField("data-label", option.getValue())
                        .addClass("multiselect-checkbox h-4 w-4 rounded border-gray-300 dark:border-gray-700 " +
                                "text-blue-600 focus:ring-blue-600");

                if (selectedIds.contains(option.getKey())) checkbox.addField("checked", "checked");

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

                  const labelEl = root.querySelector(".multiselect-label");
                  const checkboxes = Array.from(root.querySelectorAll(".multiselect-checkbox"));

                  function updateLabel() {
                    const checked = checkboxes.filter(cb => cb.checked);

                    if (checked.length === 0) {
                      labelEl.textContent = labelEl.dataset.placeholder;
                    } else if (checked.length <= 5) {
                      labelEl.innerHTML = checked.map(cb => cb.dataset.label).join(", ");
                    } else {
                      labelEl.textContent = checked.length + " selected";
                    }
                
                    %s(%s, checked.map(cb => cb.value));
                  }

                  checkboxes.forEach(cb => cb.addEventListener("change", updateLabel));
                  updateLabel();
                })();
                """.formatted(jsString(id, false), onEditFunctionName, jsString(configuration.getKey(), false))));
            } else {
                this.addInnerElement(Element.script("""
                (function () {
                  const root = document.getElementById(%s);
                  if (!root) return;

                  const labelEl = root.querySelector(".multiselect-label");
                  const checkboxes = Array.from(root.querySelectorAll(".multiselect-checkbox"));

                  function updateLabel(cb) {
                    checkboxes.forEach(checkbox => {
                        if (checkbox !== cb) checkbox.checked = false;
                    });
                
                    if (!cb.checked) {
                      labelEl.textContent = labelEl.dataset.placeholder;
                      %s(%s, '');
                    } else {
                      labelEl.innerHTML = cb.dataset.label;
                      %s(%s, cb.value);
                    }
                  }

                  checkboxes.forEach(cb => cb.addEventListener("change", () => updateLabel(cb)));
                })();
                """.formatted(jsString(id, false), onEditFunctionName, jsString(configuration.getKey(), false), onEditFunctionName, jsString(configuration.getKey(), false))));
            }

            this.addInnerElement(Element.script("""
                document.addEventListener("click", (event) => {
                    const element = document.getElementById(%s);
                    if (!element.contains(event.target)) {
                        element.open = false;
                    }
                });
                """.formatted(jsString(id, false))));
        } else if (isMultiple) {
            String id = "stringlist-" + configuration.getKey();

            this.addField("id", id);
            this.addClass("w-full");

            Element rows = Element.div(key + "rows flex flex-col gap-2");

            List<String> elements = (List<String>) configuration.get(guild);

            if (elements != null) {
                for (String value : elements) {
                    rows.addInnerElement(row(configuration.getName(), value, configuration.getKey(), onEditFunctionName, key, INPUT_CLASS, REMOVE_CLASS));
                }
            }

            this.addInnerElement(rows);

            Element addButton = Element.of("button")
                    .addField("type", "button")
                    .addClass(key + "add mt-2 flex items-center gap-2 rounded-md border border-gray-300 " +
                              "dark:border-gray-700 px-3 py-2 text-sm hover:bg-gray-100 dark:hover:bg-gray-800")
                    .addInnerElement(new FAIcon("plus"))
                    .addSafeInnerElement(" Add Entry");

            this.addInnerElement(addButton);

            this.addInnerElement(Element.script("""
                (function () {
                  const root = document.getElementById(%s);
                  if (!root) return;

                  const rowsContainer = root.querySelector(".%srows");
                  const addButton = root.querySelector(".%sadd");
                  const inputName = %s;
                  const placeholder = "Enter value";

                  function bindRemove(button, row) {
                    button.addEventListener("click", () => {
                        row.remove();
                        const values = [];
                        for (const element of document.getElementsByClassName("%sinput")) {
                            values.push(element.value);
                        }
                        %s(%s, values);
                    });
                  }

                  function createRow() {
                    const row = document.createElement("div");
                    row.className = "%srow flex items-center gap-2";

                    const input = document.createElement("input");
                    input.type = "text";
                    input.name = inputName;
                    input.placeholder = placeholder;
                    input.className = "%s";
                    input.oninput = () => {
                        const values = [];
                        for (const element of document.getElementsByClassName("%sinput")) {
                            values.push(element.value);
                        }
                        %s(%s, values);
                    };
                    row.appendChild(input);

                    const removeButton = document.createElement("button");
                    removeButton.type = "button";
                    removeButton.className = "%s";
                    removeButton.innerHTML = '<i class="fa-solid fa-trash"></i>';
                    bindRemove(removeButton, row);
                    row.appendChild(removeButton);

                    return row;
                  }

                  rowsContainer.querySelectorAll(".%sremove").forEach(button => {
                    bindRemove(button, button.closest(".%srow"));
                  });

                  addButton.addEventListener("click", () => rowsContainer.appendChild(createRow()));
                })();
                """.formatted(
                    jsString(id), key, key, jsString(configuration.getName()),
                    key,
                    onEditFunctionName, jsString(configuration.getKey()), key,
                    INPUT_CLASS, key, onEditFunctionName, jsString(configuration.getKey()),
                    REMOVE_CLASS, key, key
            )));
        } else {
            String value = (String) configuration.get(guild);

            this.addClass("w-full");

            Element input = Element.of("input")
                    .addField("type", "text")
                    .addField("name", configuration.getName())
                    .addField("value", value == null ? "" : HtmlUtils.htmlEscape(value))
                    .addField("placeholder", HtmlUtils.htmlEscape("Enter value"))
                    .addField("oninput", """
                            %s(%s, this.value)
                            """.formatted(onEditFunctionName, jsString(configuration.getKey(), false)))
                    .addClass(INPUT_CLASS)
                    .addClass("w-full");

            this.addInnerElement(input);
        }
    }

    private Element row(String name, String value, String configKey, String onEditFunctionName, String key, String INPUT_CLASS, String REMOVE_CLASS) {
        Element input = Element.of("input")
                .addField("type", "text")
                .addField("name", name)
                .addField("value", HtmlUtils.htmlEscape(value))
                .addField("placeholder", HtmlUtils.htmlEscape("Enter value"))
                .addField("oninput", """
                            const values = [];
                            for (const element of document.getElementsByClassName('%sinput')) {
                                values.push(element.value);
                            }
                            %s(%s, values);
                            """.formatted(key, onEditFunctionName, jsString(configKey, false)))
                .addClass(INPUT_CLASS);

        Element removeButton = Element.of("button")
                .addField("type", "button")
                .addClass(REMOVE_CLASS)
                .addInnerElement(new FAIcon("trash"));

        return Element.div(key + "row flex items-center gap-2")
                .addInnerElement(input)
                .addInnerElement(removeButton);
    }
}
