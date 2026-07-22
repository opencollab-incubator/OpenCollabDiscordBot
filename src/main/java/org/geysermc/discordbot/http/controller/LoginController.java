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

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal login page. This exists mostly as a fallback/testing entry point -
 * the real panel frontend should link straight to /oauth2/authorization/discord.
 */
@RestController
public class LoginController {

    @GetMapping("/login")
    public ResponseEntity<String> login(@RequestParam(name = "error", required = false) String error,
                                         @RequestParam(name = "logout", required = false) String logout) {
        String message;
        if (error != null) {
            message = "<p style=\"color:#c0392b\">Login failed, please try again.</p>";
        } else if (logout != null) {
            message = "<p>You have been logged out.</p>";
        } else {
            message = "";
        }

        String html = """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <title>GeyserMC Bot - Login</title>
                </head>
                <body>
                    <h1>GeyserMC Bot Panel</h1>
                    %s
                    <a href="/oauth2/authorization/discord">Login with Discord</a>
                </body>
                </html>
                """.formatted(message);

        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }
}
