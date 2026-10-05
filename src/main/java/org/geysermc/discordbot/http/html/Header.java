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

import jakarta.servlet.http.HttpServletRequest;
import net.dv8tion.jda.api.entities.User;
import org.geysermc.discordbot.GeyserBot;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.util.HtmlUtils;

public final class Header {
    public static final AuthenticatedElement ELEMENT = new AuthenticatedElement(
            new Authenticated(),
            Element.a("/oauth2/authorization/discord")
                .addClass("px-3 py-1.5 rounded-md hover:bg-white/10 transition")
                .addInnerElement("Login")
    );

    private static class Authenticated extends Element {
        protected Authenticated() {
            super("a");

            this.addField("href", "/user/self");
            this.addClass("flex items-center gap-2 rounded-md pl-2 pr-3 py-1.5 transition hover:bg-white/10");
            this.addInnerElement(new ProfilePicture());
            this.addInnerElement(new DisplayName());
        }

        private static class ProfilePicture implements HtmlRenderable {
            @Override
            public String render(HttpServletRequest request, OAuth2User principal) {
                String userId = principal.getAttribute("id");
                User user = GeyserBot.getJDA().getUserById(userId);
                String avatarLink;
                if (user == null) {
                    avatarLink = "https://cdn.discordapp.com/embed/avatars/1.png";
                } else {
                    avatarLink = user.getEffectiveAvatarUrl();
                }

                return Element.img(HtmlUtils.htmlEscape(avatarLink), "Profile Picture")
                        .addClass("w-6 h-6 rounded-full object-cover").toString();
            }
        }

        private static class DisplayName implements HtmlRenderable {
            @Override
            public String render(HttpServletRequest request, OAuth2User principal) {
                String userId = principal.getAttribute("id");
                User user = GeyserBot.getJDA().getUserById(userId);
                String username;
                String displayName;
                if (user == null) {
                    username = principal.getAttribute("username");
                    String globalName = principal.getAttribute("global_name");
                    displayName = globalName == null ? username : globalName;
                } else {
                    username = user.getName();
                    displayName = user.getEffectiveName();
                }

                return Element.p()
                        .addField("title", HtmlUtils.htmlEscape(username))
                        .addClass("text-sm font-medium")
                        .addSafeInnerElement(HtmlUtils.htmlEscape(displayName)).toString();
            }
        }
    }
}
