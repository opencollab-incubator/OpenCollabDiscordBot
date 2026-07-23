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
import org.springframework.security.oauth2.core.user.OAuth2User;

public final class Header {
    public static final AuthenticatedElement ELEMENT = new AuthenticatedElement(new Authenticated(), new Unauthenticated());

    private static class Authenticated extends Element {
        protected Authenticated() {
            super("a");

            this.addField("href", "/api/me");
            this.addClass("container mx-auto flex gap-2 items-center hover:text-blue-200");
            this.addInnerElement(new ProfilePicture());
            this.addInnerElement(new DisplayName());
        }

        private static class ProfilePicture implements HtmlRenderable {
            @Override
            public String render(HttpServletRequest request, OAuth2User principal) {
                String userId = principal.getAttribute("id");
                String avatarHash = principal.getAttribute("avatar");
                String avatarLink = avatarHash == null
                        ? "https://cdn.discordapp.com/embed/avatars/1.png"
                        : "https://cdn.discordapp.com/avatars/%s/%s.png".formatted(userId, avatarHash);

                return "<img alt=\"Profile Picture\" class=\"w-8 h-8 rounded-xl\" src=\"" + avatarLink + "\" />";
            }
        }

        private static class DisplayName implements HtmlRenderable {
            @Override
            public String render(HttpServletRequest request, OAuth2User principal) {
                String username = principal.getAttribute("username");
                String globalName = principal.getAttribute("global_name");
                String displayName = globalName == null ? username : globalName;

                return "<p title=\"" + username + "\">" + displayName + "</p>";
            }
        }
    }

    private static class Unauthenticated implements HtmlRenderable {
        @Override
        public String render(HttpServletRequest request, OAuth2User principal) {
            return "<a href=\"/oauth2/authorization/discord\" class=\"hover:text-blue-200\">Login</a>";
        }
    }
}
