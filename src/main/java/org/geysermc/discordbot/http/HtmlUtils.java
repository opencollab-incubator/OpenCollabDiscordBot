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

package org.geysermc.discordbot.http;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlUtils {
    private static final Pattern AUTH_PATTERN = Pattern.compile("\\$\\[auth:([^:\\]]+):([^:\\]]+)]");

    public static String getHtml(HttpServletRequest request, OAuth2User principal, String name, Map<String, String> variables) throws IOException {
        Map<String, String> mutableVars = new HashMap<>(variables);
        mutableVars.putAll(getBaseVars(request, principal));
        String rawHtml = new String(HtmlUtils.class.getClassLoader().getResourceAsStream("web/" + name + ".html").readAllBytes(), StandardCharsets.UTF_8);
        for (Map.Entry<String, String> variable : mutableVars.entrySet()) {
            rawHtml = rawHtml.replace("![" + variable.getKey() + "]", variable.getValue());
        }

        Matcher matcher = AUTH_PATTERN.matcher(rawHtml);
        rawHtml = matcher.replaceAll(result -> {
            try {
                if (principal == null) {
                    return getHtml(request, null, result.group(2), variables);
                } else {
                    return getHtml(request, principal, result.group(1), variables);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        return rawHtml;
    }

    public static String getPage(HttpServletRequest request, OAuth2User principal, String name, Map<String, String> variables) throws IOException {
        String base = getHtml(request, principal, "base", variables);
        String page = getHtml(request, principal, name, variables);
        return base.replace("![content]", page);
    }

    public static String getRawPage(HttpServletRequest request, OAuth2User principal, String html, Map<String, String> variables) throws IOException {
        String base = getHtml(request, principal, "base", variables);
        return base.replace("![content]", html);
    }

    public static Map<String, String> getBaseVars(HttpServletRequest request, OAuth2User principal) {
        if (principal == null) return Map.of();

        String userId = principal.getAttribute("id");
        String avatarHash = principal.getAttribute("avatar");
        String username = principal.getAttribute("username");
        String globalName = principal.getAttribute("global_name");

        Map<String, String> data = new HashMap<>();
        data.put("id", userId);
        data.put("username", username);
        data.put("globalName", globalName);
        data.put("displayName", globalName == null ? username : globalName);
        data.put("avatarUrl", avatarHash == null
                ? "https://cdn.discordapp.com/embed/avatars/1.png"
                : "https://cdn.discordapp.com/avatars/%s/%s.png".formatted(userId, avatarHash));

        return data;
    }
}
