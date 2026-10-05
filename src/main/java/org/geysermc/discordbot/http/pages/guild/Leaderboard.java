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
import org.geysermc.discordbot.GeyserBot;
import org.geysermc.discordbot.http.html.Base;
import org.geysermc.discordbot.http.html.Element;
import org.geysermc.discordbot.http.html.FAIcon;
import org.geysermc.discordbot.http.pages.elements.Name;
import org.geysermc.discordbot.storage.LevelInfo;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;

public class Leaderboard extends Base {
    private static final DecimalFormat NUM_FORMAT = new DecimalFormat("###,###,###");
    private static final String[] COLORS = new String[]{
            "#da9e3b", "#989898", "#ae7441"
    };

    private final Guild guild;

    public Leaderboard(User user, Guild guild) {
        super(user);
        this.guild = guild;
    }

    @Override
    public void addContent(Element container) {
        container.addInnerElement(Element.h2()
                .addInnerElement(new FAIcon("trophy").addClass("text-yellow-500"))
                .addInnerElement(" Leaderboard"));

        List<LevelInfo> levels = GeyserBot.storageManager.getLevels(guild);

        if (levels.isEmpty()) {
            container.addInnerElement(
                    Element.div("flex flex-col items-center text-center gap-2 mt-8")
                            .addInnerElement(new FAIcon("trophy")
                                    .addClass("text-5xl text-gray-400 dark:text-gray-600 mb-2"))
                            .addInnerElement(Element.p().addInnerElement("No one has earned any experience here yet."))
            );
            return;
        }

        Element cardContainer = Element.div("grid gap-4 justify-center");
        cardContainer.addField("style", "grid-template-columns: repeat(auto-fill, minmax(400px, 0.25fr));");

        for (int i = 0; i < levels.size(); i++) {
            cardContainer.addInnerElement(createCard(levels.get(i), i));
        }

        container.addInnerElement(cardContainer);
    }

    private Element createCard(LevelInfo info, int place) {
        String color = "#1e2939";
        if (place < COLORS.length) {
            color = COLORS[place];
        }

        boolean isCurrentUser = user != null && info.getUser().getId().equals(user.getId());
        String backgroundClasses = isCurrentUser
                ? "bg-blue-200 dark:bg-blue-900 border-2 border-blue-300 dark:border-blue-800"
                : "bg-gray-200 dark:bg-gray-900";

        String avatarLink = info.getUser().getAvatarUrl() == null
                ? "https://cdn.discordapp.com/embed/avatars/1.png"
                : info.getUser().getAvatarUrl();

        Element pfpDiv = Element.div("relative")
                .addInnerElement(Element.img(
                        avatarLink, "User Profile Picture",
                        "h-[136px] w-[136px] flex-shrink-0 rounded-lg object-cover border-4 border-[" + color + "]"
                ));

        if (place < 3) {
            pfpDiv.addInnerElement(new FAIcon("crown")
                    .addClass("absolute right-[43%] -top-3 text-[" + color + "]"));
        }

        Element nameRow = Element.of("h3", "mb-0.5 flex flex-wrap items-center gap-2 text-lg font-semibold")
                .addInnerElement(new Name(info.getUser(), guild))
                .addInnerElement(Element.of("span", "text-sm font-normal text-gray-500 dark:text-gray-400")
                        .addInnerElement("#" + (place + 1)));

        if (isCurrentUser) {
            nameRow.addInnerElement(Element.of("span", "rounded-full bg-blue-600 px-2 py-0.5 text-xs font-semibold text-white")
                    .addInnerElement("You"));
        }

        return Element.div("flex max-w-md flex-col overflow-hidden rounded-xl shadow-sm transition hover:shadow-md")
                .addClass(backgroundClasses)
                .addInnerElement(Element.of("div")
                        .addClass("flex items-stretch gap-4 p-4 relative")
                        .addInnerElement(pfpDiv)
                        .addInnerElement(
                                Element.div("flex min-w-0 flex-col justify-center")
                                        .addInnerElement(nameRow)
                                        .addInnerElement(statRow("chart-line", "Level", NUM_FORMAT.format(info.getLevel())))
                                        .addInnerElement(statRow("star", "Experience", NUM_FORMAT.format(info.getXp())))
                                        .addInnerElement(statRow("comment", "Messages", NUM_FORMAT.format(info.getMessages())))
                        ));
    }

    private Element statRow(String icon, String label, String value) {
        return Element.of("p", "flex items-center gap-1.5 text-sm text-gray-700 dark:text-gray-300")
                .addInnerElement(new FAIcon(icon).addClass("text-xs"))
                .addInnerElement(label + ": ")
                .addInnerElement(Element.of("span", "font-semibold text-black dark:text-white")
                        .addInnerElement(value));
    }
}
