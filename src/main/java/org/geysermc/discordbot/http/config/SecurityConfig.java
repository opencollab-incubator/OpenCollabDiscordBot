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
import org.geysermc.discordbot.util.PropertiesManager;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, OAuth2AuthorizationRequestResolver authorizationRequestResolver,
                                                     PersistentSecurityContextRepository securityContextRepository) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/oauth2/**", "/login/oauth2/**", "/error", "/*/leaderboard").permitAll()
                        .anyRequest().authenticated()
                )
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .addFilterBefore(new RedirectAfterLoginFilter(securityContextRepository), ExceptionTranslationFilter.class)
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/oauth2/authorization/discord")
                        .authorizationEndpoint(endpoint -> endpoint
                                .authorizationRequestResolver(authorizationRequestResolver)
                                .authorizationRequestRepository(new CookieAuthorizationRequestRepository()))
                        .successHandler(postLoginSuccessHandler())
                        .failureHandler((request, response, exception) -> {
                            new DefaultRedirectStrategy().sendRedirect(request, response, "/?login_error=" + URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8));
                        })
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                        })
                        .logoutSuccessUrl("/?logout")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .addLogoutHandler(securityContextRepository)
                )
                //.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .exceptionHandling(exceptions -> exceptions
                        // The web panel talks to /api/** as JSON - return 401 there instead of
                        // redirecting to the HTML login page like the default entry point does.
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                PathPatternRequestMatcher.pathPattern("/api/**")
                        )
                );

        return http.build();
    }

    private String defaultRedirectUrl() {
        String origin = PropertiesManager.getWebPanelOrigin();
        return origin != null ? origin : "/";
    }

    /**
     * Sends a freshly-logged-in user back to the page {@link RedirectAfterLoginFilter} saw them
     * try to visit before they signed in, if any; otherwise falls back to the usual default.
     */
    private AuthenticationSuccessHandler postLoginSuccessHandler() {
        return (request, response, authentication) -> {
            String redirectTo = consumeRedirectCookie(request, response);
            new DefaultRedirectStrategy().sendRedirect(request, response, redirectTo != null ? redirectTo : defaultRedirectUrl());
        };
    }

    private static @Nullable String consumeRedirectCookie(HttpServletRequest request, HttpServletResponse response) {
        String value = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (RedirectAfterLoginFilter.COOKIE_NAME.equals(cookie.getName())) {
                    value = cookie.getValue();
                    break;
                }
            }
        }

        // Clear it either way so it can't be replayed on a later, unrelated login.
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(RedirectAfterLoginFilter.COOKIE_NAME, "")
                .path("/")
                .maxAge(Duration.ZERO)
                .build().toString());

        if (value == null) return null;

        String decoded = URLDecoder.decode(value, StandardCharsets.UTF_8);
        return isSafeRedirect(decoded) ? decoded : null;
    }

    /**
     * Only ever redirect to a same-origin path outside the login flow itself, since the cookie
     * value is attacker-controllable input.
     */
    private static boolean isSafeRedirect(String path) {
        return path.startsWith("/") && !path.startsWith("//") && !path.contains("\\")
                && !path.startsWith("/oauth2") && !path.startsWith("/login");
    }

    /**
     * Discord supports (and requires, for public clients) PKCE on the authorization code flow.
     * This customizer adds the code_challenge/code_challenge_method params to the outgoing
     * authorization request and verifies the code_verifier on the token exchange.
     */
    @Bean
    public OAuth2AuthorizationRequestResolver oauth2AuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        DefaultOAuth2AuthorizationRequestResolver resolver = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, DefaultOAuth2AuthorizationRequestResolver.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        return resolver;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        String panelOrigin = PropertiesManager.getWebPanelOrigin();
        configuration.setAllowedOrigins(panelOrigin != null ? List.of(panelOrigin) : List.of());
        configuration.setAllowedMethods(List.of("GET", "POST"));
        configuration.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
