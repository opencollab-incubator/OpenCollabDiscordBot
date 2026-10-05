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

import jakarta.servlet.http.HttpServletRequest;
import org.geysermc.discordbot.http.html.HtmlPage;
import org.geysermc.discordbot.http.html.HtmlRenderable;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class PageUtils {
    private static final MediaType TEXT_HTML_UTF8 = new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8);

    public static ResponseEntity<String> getResponse(HttpServletRequest request, OAuth2User principal, HtmlRenderable renderable) {
        return getResponse(request, principal, renderable, HttpStatus.OK);
    }

    public static ResponseEntity<String> getResponse(HttpServletRequest request, OAuth2User principal, HtmlRenderable renderable, HttpStatusCode code) {
        if (renderable instanceof HtmlPage page) page.create();
        return ResponseEntity.status(code).contentType(TEXT_HTML_UTF8).body(renderable.render(request, principal));
    }

    public static ResponseEntity<String> getJsonResponse(HttpStatus status, JSONObject json) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(json.toString());
    }

    public static ResponseEntity<String> getJsonResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new JSONObject(Map.of(
                        "code", status.value(),
                        "message", message
                )).toString());
    }
}
