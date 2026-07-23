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

package org.geysermc.discordbot.http.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.geysermc.discordbot.GeyserBot;
import org.geysermc.discordbot.storage.AuthSession;
import org.jspecify.annotations.Nullable;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;

/**
 * Stores the logged-in Discord user in the {@code auth_sessions} database table instead of the
 * servlet container's (in-memory) HTTP session, so a logged-in user stays logged in across a bot
 * restart. Only an opaque, hashed token is kept in the browser's cookie - the actual OAuth2 user
 * attributes live in the database, keyed by that hash.
 */
@Component
public class PersistentSecurityContextRepository implements SecurityContextRepository, LogoutHandler {

    private static final String COOKIE_NAME = "AUTH_SESSION";
    private static final String REGISTRATION_ID = "discord";
    private static final String USER_NAME_ATTRIBUTE = "id";
    private static final Duration SESSION_DURATION = Duration.ofDays(30);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication authentication = readAuthentication(requestResponseHolder.getRequest());
        if (authentication != null) {
            context.setAuthentication(authentication);
        }

        return context;
    }

    @Override
    public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = context.getAuthentication();
        if (!(authentication instanceof OAuth2AuthenticationToken oauth2Authentication) || !authentication.isAuthenticated()) {
            return;
        }

        OAuth2User principal = oauth2Authentication.getPrincipal();
        long discordUserId = Long.parseLong(principal.getName());
        String attributesJson = new JSONObject(principal.getAttributes()).toString();

        String token = generateToken();
        long expiresAt = System.currentTimeMillis() + SESSION_DURATION.toMillis();
        GeyserBot.storageManager.saveAuthSession(hash(token), discordUserId, attributesJson, expiresAt);

        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie(token, request.isSecure(), SESSION_DURATION).toString());
    }

    @Override
    public boolean containsContext(HttpServletRequest request) {
        return readAuthentication(request) != null;
    }

    /**
     * Removes the persisted session on logout - registered as a {@link LogoutHandler} in
     * {@link SecurityConfig}, since the default logout flow only clears the (unused) HTTP session.
     */
    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, @Nullable Authentication authentication) {
        String token = readCookie(request);
        if (token != null) {
            GeyserBot.storageManager.deleteAuthSession(hash(token));
        }

        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie("", request.isSecure(), Duration.ZERO).toString());
    }

    private @Nullable Authentication readAuthentication(HttpServletRequest request) {
        String token = readCookie(request);
        if (token == null) {
            return null;
        }

        AuthSession session = GeyserBot.storageManager.getAuthSession(hash(token));
        if (session == null) {
            return null;
        }

        Map<String, Object> attributes = new JSONObject(session.attributesJson()).toMap();
        DefaultOAuth2User principal = new DefaultOAuth2User(Set.of(new OAuth2UserAuthority(attributes)), attributes, USER_NAME_ATTRIBUTE);
        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), REGISTRATION_ID);
    }

    private static @Nullable String readCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private static ResponseCookie sessionCookie(String value, boolean secure, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
