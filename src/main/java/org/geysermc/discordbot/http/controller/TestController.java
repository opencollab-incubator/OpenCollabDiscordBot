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

package org.geysermc.discordbot.http.controller;

import jakarta.servlet.http.HttpServletRequest;
import net.dv8tion.jda.api.entities.User;
import org.geysermc.discordbot.GeyserBot;
import org.geysermc.discordbot.http.PageUtils;
import org.geysermc.discordbot.http.html.Base;
import org.geysermc.discordbot.http.html.Element;
import org.geysermc.discordbot.http.pages.elements.Name;
import org.geysermc.discordbot.permissions.MemberRank;
import org.geysermc.discordbot.permissions.UserRank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RestController
public class TestController {
    // TODO Disable when a certain bot arg is present
    @GetMapping("/test")
    private ResponseEntity<String> response(HttpServletRequest request, @AuthenticationPrincipal OAuth2User principal) {
        User user = null;
        if (principal != null) {
            String userId = principal.getAttribute("id");
            user = GeyserBot.getJDA().retrieveUserById(userId).complete();
        }

        Base page = new Base(user) {
            @Override
            public void addContent(Element container) {
                container.addInnerElement(Element.h2().addInnerElement("Rank name tests:"));
                for (UserRank userRank : UserRank.values()) {
                    for (MemberRank memberRank : MemberRank.values()) {
                        container.addInnerElement(Element.of("p")
                                .addClass("font-bold")
                                .addInnerElement("User Rank: ")
                                .addInnerElement(userRank.toString())
                                .addInnerElement(" | Member Rank: ")
                                .addInnerElement(memberRank.toString()));
                        container.addInnerElement(Element.of("p")
                                .addInnerElement(new Name(user, null, true, userRank, memberRank)));
                    }
                    container.addInnerElement(Element.br());
                }

                container.addInnerElement(Element.h2().addInnerElement("Time tests:"));

                container.addInnerElement(Element.h3().addInnerElement("Dynamic (Relative)"));
                container.addInnerElement(Element.dynamicTimeDisplay(Instant.now().plusSeconds(5)).addField("data-type", "relative"));
                container.addInnerElement(Element.br());
                container.addInnerElement(Element.dynamicTimeDisplay(Instant.now().plusSeconds(10)).addField("data-type", "relative"));
                container.addInnerElement(Element.br());
                container.addInnerElement(Element.dynamicTimeDisplay(Instant.now().plusSeconds(15)).addField("data-type", "relative"));
                container.addInnerElement(Element.br());
                container.addInnerElement(Element.dynamicTimeDisplay(Instant.now().plusSeconds(20)).addField("data-type", "relative"));

                container.addInnerElement(Element.h3().addInnerElement("Dynamic (Expire)"));
                container.addInnerElement(Element.dynamicTimeDisplay(Instant.now().plusSeconds(5)));
                container.addInnerElement(Element.br());
                container.addInnerElement(Element.dynamicTimeDisplay(Instant.now().plusSeconds(10)));
                container.addInnerElement(Element.br());
                container.addInnerElement(Element.dynamicTimeDisplay(Instant.now().plusSeconds(15)));
                container.addInnerElement(Element.br());
                container.addInnerElement(Element.dynamicTimeDisplay(Instant.now().plusSeconds(20)));

                container.addInnerElement(Element.h3().addInnerElement("Static"));
                container.addInnerElement(Element.staticTimeDisplay(Instant.now().plus(5, ChronoUnit.SECONDS)));
                container.addInnerElement(Element.br());
                container.addInnerElement(Element.staticTimeDisplay(Instant.now().plus(5, ChronoUnit.MINUTES)));
                container.addInnerElement(Element.br());
                container.addInnerElement(Element.staticTimeDisplay(Instant.now().plus(5, ChronoUnit.HOURS)));
                container.addInnerElement(Element.br());
                container.addInnerElement(Element.staticTimeDisplay(Instant.now().plus(5, ChronoUnit.DAYS)));
                container.addInnerElement(Element.br());
                container.addInnerElement(Element.staticTimeDisplay(Instant.now().plus(10, ChronoUnit.SECONDS)));
                container.addInnerElement(Element.br());
                container.addInnerElement(Element.staticTimeDisplay(Instant.now().plus(10, ChronoUnit.MINUTES)));
                container.addInnerElement(Element.br());
                container.addInnerElement(Element.staticTimeDisplay(Instant.now().plus(10, ChronoUnit.HOURS)));
                container.addInnerElement(Element.br());
                container.addInnerElement(Element.staticTimeDisplay(Instant.now().plus(10, ChronoUnit.DAYS)));
                container.addInnerElement(Element.br());
                container.addInnerElement(Element.staticTimeDisplay(Instant.now().plus(15, ChronoUnit.SECONDS)));
                container.addInnerElement(Element.br());
                container.addInnerElement(Element.staticTimeDisplay(Instant.now().plus(15, ChronoUnit.MINUTES)));
                container.addInnerElement(Element.br());
                container.addInnerElement(Element.staticTimeDisplay(Instant.now().plus(15, ChronoUnit.HOURS)));
                container.addInnerElement(Element.br());
                container.addInnerElement(Element.staticTimeDisplay(Instant.now().plus(15, ChronoUnit.DAYS)));
            }
        };

        return PageUtils.getResponse(request, principal, page);
    }
}
