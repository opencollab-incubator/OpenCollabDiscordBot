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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Remembers the page an anonymous visitor was trying to reach in a short-lived cookie, so
 * {@link SecurityConfig}'s OAuth2 success handler can send them back there once they sign in.
 * The app authenticates via the cookie-backed {@link PersistentSecurityContextRepository} rather
 * than the HTTP session, so Spring Security's default (session-based) SavedRequest/RequestCache
 * "return to the page you came from" mechanism never kicks in here - this is the cookie
 * equivalent of it, checked directly against that repository instead of the session.
 */
public class RedirectAfterLoginFilter extends OncePerRequestFilter {

    static final String COOKIE_NAME = "POST_LOGIN_REDIRECT";
    private static final Duration COOKIE_DURATION = Duration.ofMinutes(10);

    // Pages that don't require sign-in (or are already part of the login flow itself) never need
    // to be remembered as a "come back here" target.
    private static final RequestMatcher SKIP = new OrRequestMatcher(
            PathPatternRequestMatcher.pathPattern("/"),
            PathPatternRequestMatcher.pathPattern("/login"),
            PathPatternRequestMatcher.pathPattern("/oauth2/**"),
            PathPatternRequestMatcher.pathPattern("/login/oauth2/**"),
            PathPatternRequestMatcher.pathPattern("/error"),
            PathPatternRequestMatcher.pathPattern("/*/leaderboard"),
            PathPatternRequestMatcher.pathPattern("/api/**")
    );

    private final PersistentSecurityContextRepository securityContextRepository;

    public RedirectAfterLoginFilter(PersistentSecurityContextRepository securityContextRepository) {
        this.securityContextRepository = securityContextRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (shouldRemember(request)) {
            String target = request.getRequestURI();
            String query = request.getQueryString();
            if (query != null) target += "?" + query;

            response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(COOKIE_NAME, encode(target))
                    .httpOnly(true)
                    .secure(request.isSecure())
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(COOKIE_DURATION)
                    .build().toString());
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldRemember(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) return false;
        if (SKIP.matches(request)) return false;

        return !securityContextRepository.containsContext(request);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
