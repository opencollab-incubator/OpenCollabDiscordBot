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

import org.geysermc.discordbot.util.PropertiesManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

/**
 * Registers Discord as an OAuth2 client. Discord isn't one of Spring Boot's
 * "common providers" (Google/GitHub/Okta/...), so the {@link ClientRegistration}
 * is built by hand instead of via {@code application.yml} binding.
 */
@Configuration
public class DiscordOAuth2Config {

    public static final String REGISTRATION_ID = "discord";

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration discord = ClientRegistration.withRegistrationId(REGISTRATION_ID)
                .clientId(PropertiesManager.getClientId())
                .clientSecret(PropertiesManager.getClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                // Must exactly match a redirect registered on the Discord application -
                // update it there to "<web-public-address>/login/oauth2/code/discord".
                .redirectUri(PropertiesManager.getPublicWebAddress() + "/login/oauth2/code/" + REGISTRATION_ID)
                .scope("identify")
                .authorizationUri("https://discord.com/api/oauth2/authorize")
                .tokenUri("https://discord.com/api/oauth2/token")
                .userInfoUri("https://discord.com/api/users/@me")
                .userNameAttributeName("id")
                .clientName("Discord")
                .build();

        // TODO: this in-memory repository (and the default HttpSessionOAuth2AuthorizedClientRepository
        //  used to store the resulting access/refresh tokens) both live only as long as the JVM/session.
        //  If the panel needs to survive restarts or run as more than one instance, both need to be
        //  swapped for persistent (database-backed) implementations.
        return new InMemoryClientRegistrationRepository(discord);
    }
}
