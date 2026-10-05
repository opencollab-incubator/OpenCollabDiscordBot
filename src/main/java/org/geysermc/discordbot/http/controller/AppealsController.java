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
import org.geysermc.discordbot.http.html.RedirectHtmlPage;
import org.geysermc.discordbot.http.pages.appeals.AppealsCreate;
import org.geysermc.discordbot.http.pages.appeals.AppealsView;
import org.geysermc.discordbot.http.pages.Error;
import org.geysermc.discordbot.permissions.PermissionManager;
import org.geysermc.discordbot.permissions.UserPermissions;
import org.geysermc.discordbot.storage.Appeal;
import org.geysermc.discordbot.storage.BanData;
import org.geysermc.discordbot.util.ModerationHelper;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@RestController
public class AppealsController {
    private static final Logger log = LoggerFactory.getLogger(AppealsController.class);

    @GetMapping("/appeals")
    public ResponseEntity<String> index(HttpServletRequest request, @AuthenticationPrincipal OAuth2User principal) throws IOException {
        User user = null;
        if (principal != null) {
            String userId = principal.getAttribute("id");
            user = GeyserBot.getJDA().retrieveUserById(userId).complete();
        }

        if (user == null) {
            return PageUtils.getResponse(request, principal, new Error(null, HttpStatus.UNAUTHORIZED.value(), "You must be logged in to view this page."), HttpStatus.UNAUTHORIZED);
        }

        return PageUtils.getResponse(request, principal, new AppealsView(user));
    }

    @GetMapping("/appeals/create")
    public ResponseEntity<String> create(HttpServletRequest request, @AuthenticationPrincipal OAuth2User principal) throws IOException {
        User user = null;
        if (principal != null) {
            String userId = principal.getAttribute("id");
            user = GeyserBot.getJDA().getUserById(userId);
        }

        if (user == null) {
            return PageUtils.getResponse(request, principal, new Error(null, HttpStatus.UNAUTHORIZED, "You must be logged in to view this page."), HttpStatus.UNAUTHORIZED);
        }

        BanData data = GeyserBot.storageManager.getBan(user.getIdLong());
        data = new BanData(user, Instant.EPOCH, Instant.now().plus(30, ChronoUnit.DAYS), "no no no");

        if (data == null) {
            return PageUtils.getResponse(request, principal, new Error(user, HttpStatus.BAD_REQUEST, "You do not have a ban to appeal!"));
        }

        List<Appeal> appeals = GeyserBot.storageManager.getAppeals(user, 1);

        if (!appeals.isEmpty() && appeals.getFirst().state() == Appeal.State.PENDING) {
            return PageUtils.getResponse(request, principal, new Error(user, HttpStatus.BAD_REQUEST, "You have already appealed your current ban!"));
        }

        return PageUtils.getResponse(request, principal, new AppealsCreate(user, data));
    }

    @GetMapping("/appeals/submit")
    public ResponseEntity<String> submit(
            HttpServletRequest request, @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(name = "appeal_message") String appealMessage
    ) {
        User user = null;
        if (principal != null) {
            String userId = principal.getAttribute("id");
            user = GeyserBot.getJDA().getUserById(userId);
        }

        if (user == null) {
            return PageUtils.getResponse(request, principal, new Error(null, HttpStatus.UNAUTHORIZED, "You must be logged in to execute this action."), HttpStatus.UNAUTHORIZED);
        }

        BanData data = GeyserBot.storageManager.getBan(user.getIdLong());
        //data = new BanData(user, Instant.EPOCH, Instant.now().plus(30, ChronoUnit.DAYS), "no no no");

        if (data == null) {
            return PageUtils.getResponse(request, principal, new Error(user, HttpStatus.BAD_REQUEST, "You do not have a ban to submit an appeal for!"));
        }

        List<Appeal> appeals = GeyserBot.storageManager.getAppeals(user, 1);

        if (!appeals.isEmpty() && appeals.getFirst().state() == Appeal.State.PENDING) {
            return PageUtils.getResponse(request, principal, new Error(user, HttpStatus.BAD_REQUEST, "You have already appealed your current ban!"));
        }

        int id = GeyserBot.storageManager.addAppeal(user, appealMessage);

        if (id == -1) {
            return PageUtils.getResponse(request, principal, new Error(user, HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong while submitting your appeal."));
        }

        return PageUtils.getResponse(request, principal, new RedirectHtmlPage("/?info=" + URLEncoder.encode("Appeal submitted successfully. (ID: %d)".formatted(id), StandardCharsets.UTF_8)));
    }

    @PostMapping("/api/appeals/approve/{id}")
    public ResponseEntity<String> apiApprove(@AuthenticationPrincipal OAuth2User principal, @PathVariable String id) {
        return commonAppealHandle(principal, id, Appeal.State.APPROVED, "Appeal approved!");
    }

    @PostMapping("/api/appeals/deny/{id}")
    public ResponseEntity<String> apiDeny(@AuthenticationPrincipal OAuth2User principal, @PathVariable String id) {
        return commonAppealHandle(principal, id, Appeal.State.DENIED, "Appeal denied!");
    }

    private ResponseEntity<String> commonAppealHandle(@AuthenticationPrincipal OAuth2User principal, @PathVariable String id, Appeal.State state, String message) {
        User user = null;
        if (principal != null) {
            String userId = principal.getAttribute("id");
            user = GeyserBot.getJDA().getUserById(userId);
        }

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new JSONObject(Map.of(
                            "code", 401,
                            "message", "You must be signed in to access this endpoint."
                    )).toString());
        }

        if (!PermissionManager.hasPermission(user, UserPermissions.MANAGE_APPEALS)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new JSONObject(Map.of(
                            "code", 403,
                            "message", "You do not have the permissions to access this endpoint."
                    )).toString());
        }

        int intId;
        try {
            intId = Integer.parseInt(id);
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new JSONObject(Map.of(
                            "code", 400,
                            "message", "An invalid ID was provided. (Non-numeric)"
                    )).toString());
        }

        Appeal appeal = GeyserBot.storageManager.getAppeal(intId);
        if (appeal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new JSONObject(Map.of(
                            "code", 400,
                            "message", "An invalid ID was provided. (Not found)"
                    )).toString());
        }

        if (!appeal.state().equals(Appeal.State.PENDING)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new JSONObject(Map.of(
                            "code", 409,
                            "message", "This appeal has already been handled by someone else."
                    )).toString());
        }

        GeyserBot.storageManager.updateAppeal(intId, state);

        if (state == Appeal.State.APPROVED) {
            ModerationHelper.revokeBan(appeal.user().getIdLong(), user, "Appeal accepted.");
        }

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new JSONObject(Map.of(
                        "code", 200,
                        "message", message
                )).toString());
    }

    @ExceptionHandler(produces = "application/json")
    public ResponseEntity<String> handleJson(Exception e) {
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        e.printStackTrace(printWriter);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new JSONObject(Map.of(
                "exception", writer.toString()
        )).toString());
    }

    @ExceptionHandler(produces = "text/html")
    public ResponseEntity<String> handle(Exception e) {
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        e.printStackTrace(printWriter);

        return PageUtils.getResponse(null, null, new Error(null, 500, writer.toString()), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
