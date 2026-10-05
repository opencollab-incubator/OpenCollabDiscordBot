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

package org.geysermc.discordbot.http.pages.guild;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import org.geysermc.discordbot.http.html.Base;
import org.geysermc.discordbot.http.html.Element;
import org.geysermc.discordbot.http.pages.elements.ScriptButton;
import org.geysermc.discordbot.http.pages.guild.configure.ConfigEntry;
import org.geysermc.discordbot.storage.ServerConfiguration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Configure extends Base {
    /**
     * Groups the flat list of {@link ServerConfiguration} entries into themed sections for
     * display. Keyed by the entries' {@link ServerConfiguration.Configuration#getKey()} so this
     * stays independent of registration order.
     */
    private static final Map<String, List<String>> SECTION_KEYS = new LinkedHashMap<>();

    static {
        SECTION_KEYS.put("Moderation", List.of(
                "allowed-invites", "banned-domains", "banned-ips", "check-domains",
                "convert-extensions", "honey-pot-channel", "moderation-channel",
                "moderation-role", "punishment-message"
        ));
        SECTION_KEYS.put("Logging & Health", List.of(
                "log-channel", "dont-log", "dont-check-error",
                "health-check-channel", "health-check-urls"
        ));
        SECTION_KEYS.put("Tickets", List.of(
                "client-roles", "ticket-allowed-roles", "ticket-archive-channel",
                "ticket-categories", "ticket-ping-roles", "exploit-reports-notify-channel"
        ));
        SECTION_KEYS.put("Leveling", List.of("dont-level", "rank-roles"));
        SECTION_KEYS.put("Content & Feeds", List.of(
                "donations-feeds-channel", "rss-feeds-channels", "rss-feeds-links",
                "forum-channel", "preview-channel", "preview-feeds-channel", "tag-repo"
        ));
    }

    private final Guild guild;

    public Configure(User user, Guild guild) {
        super(user);
        this.guild = guild;
    }

    @Override
    public void appendHead(Element head) {
        super.appendHead(head);
        head.addInnerElement(Element.script("""
                window.modifiedValues = {};
                
                window.addModified = function(key, value) {
                    if (value === null) {
                        delete window.modifiedValues[key];
                    } else {
                        window.modifiedValues[key] = value;
                    }
                }
                
                window.saveModifications = function() {
                    fetch(window.location.origin + "/api/configure/modify/%s", {
                        method: 'POST',
                        headers: {
                            'Accept': 'application/json'
                        },
                        body: JSON.stringify(window.modifiedValues)
                    })
                    .then(response => response.json())
                    .then(data => {
                        let code, message;
                        if (data.status) code = data.status;
                        else code = data.code;
                        if (data.error) message = data.error;
                        else message = data.message;
                
                        const messageElement = document.getElementById('message-element');
                
                        messageElement.classList.remove('bg-red-200');
                        messageElement.classList.remove('dark:bg-red-900');
                        messageElement.classList.remove('border-red-300');
                        messageElement.classList.remove('dark:border-red-800');
                
                        messageElement.classList.remove('bg-orange-200');
                        messageElement.classList.remove('dark:bg-orange-900');
                        messageElement.classList.remove('border-orange-300');
                        messageElement.classList.remove('dark:border-orange-800');
                
                        messageElement.classList.remove('bg-yellow-200');
                        messageElement.classList.remove('dark:bg-yellow-900');
                        messageElement.classList.remove('border-yellow-300');
                        messageElement.classList.remove('dark:border-yellow-800');
                
                        messageElement.classList.remove('bg-green-200');
                        messageElement.classList.remove('dark:bg-green-900');
                        messageElement.classList.remove('border-green-300');
                        messageElement.classList.remove('dark:border-green-800');
                
                        messageElement.classList.remove('bg-blue-200');
                        messageElement.classList.remove('dark:bg-blue-900');
                        messageElement.classList.remove('border-blue-300');
                        messageElement.classList.remove('dark:border-blue-800');
                
                        if (code === 403) {
                            messageElement.classList.add('bg-red-200');
                            messageElement.classList.add('dark:bg-red-900');
                            messageElement.classList.add('border-red-300');
                            messageElement.classList.add('dark:border-red-800');
                        } else if (code === 400) {
                            messageElement.classList.add('bg-orange-200');
                            messageElement.classList.add('dark:bg-orange-900');
                            messageElement.classList.add('border-orange-300');
                            messageElement.classList.add('dark:border-orange-800');
                        } else if (code === 409) {
                            messageElement.classList.add('bg-yellow-200');
                            messageElement.classList.add('dark:bg-yellow-900');
                            messageElement.classList.add('border-yellow-300');
                            messageElement.classList.add('dark:border-yellow-800');
                        } else if (code === 200) {
                            messageElement.classList.add('bg-green-200');
                            messageElement.classList.add('dark:bg-green-900');
                            messageElement.classList.add('border-green-300');
                            messageElement.classList.add('dark:border-green-800');
                        } else {
                            messageElement.classList.add('bg-blue-200');
                            messageElement.classList.add('dark:bg-blue-900');
                            messageElement.classList.add('border-blue-300');
                            messageElement.classList.add('dark:border-blue-800');
                        }
                        messageElement.innerHTML = message;
                        messageElement.classList.remove('opacity-0');
                        if (messageElement.dataset.scheduleId) clearTimeout(messageElement.dataset.scheduleId);
                        messageElement.dataset.scheduleId = setTimeout(() => {
                            messageElement.classList.add('opacity-0');
                            messageElement.innerHTML = '';
                            messageElement.dataset.scheduleId = undefined;
                        }, 5000);
                    });
                }
                """.formatted(guild.getId())));
    }

    @Override
    public void addContent(Element container) {
        container.addInnerElement(Element.of("div")
                .addClass("opacity-0 transition-opacity rounded-xl p-2 mb-4")
                .addField("id", "message-element"));

        container.addInnerElement(Element.div("flex items-center justify-between mb-6")
                .addInnerElement(Element.h2().removeClass("mb-4")
                        .addInnerElement("Configuring ")
                        .addSafeInnerElement(guild.getName()))
                .addInnerElement(Element.of("div")
                        .addInnerElement(new ScriptButton("Save", "window.saveModifications();"))));

        Map<String, ServerConfiguration.Configuration<?>> byKey = new LinkedHashMap<>();
        for (ServerConfiguration.Configuration<?> configuration : ServerConfiguration.getAllConfigs()) {
            byKey.put(configuration.getKey(), configuration);
        }

        for (Map.Entry<String, List<String>> section : SECTION_KEYS.entrySet()) {
            List<ServerConfiguration.Configuration<?>> configurations = section.getValue().stream()
                    .map(byKey::remove)
                    .filter(Objects::nonNull)
                    .toList();

            if (configurations.isEmpty()) continue;

            container.addInnerElement(sectionCard(section.getKey(), configurations));
        }

        // Anything not explicitly grouped above still shows up, rather than silently disappearing.
        if (!byKey.isEmpty()) {
            container.addInnerElement(sectionCard("Other", List.copyOf(byKey.values())));
        }
    }

    private Element sectionCard(String title, List<ServerConfiguration.Configuration<?>> configurations) {
        Element card = Element.div("rounded-xl bg-gray-200 dark:bg-gray-900 shadow-sm p-6 mb-6");
        card.addInnerElement(Element.h3().removeClass("mb-4").addInnerElement(title));

        Element fields = Element.div("flex flex-col gap-4 mt-4");

        for (ServerConfiguration.Configuration<?> configuration : configurations) {
            fields.addInnerElement(
                    Element.div("flex flex-col gap-1")
                            .addInnerElement(Element.of("label", "text-sm font-semibold text-gray-700 dark:text-gray-300")
                                    .addSafeInnerElement(configuration.getName()))
                            .addInnerElement(new ConfigEntry(configuration, guild, "window.addModified"))
            );
        }

        card.addInnerElement(fields);
        return card;
    }
}
