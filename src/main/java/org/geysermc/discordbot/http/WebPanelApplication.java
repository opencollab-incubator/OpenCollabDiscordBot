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

import org.geysermc.discordbot.GeyserBot;
import org.geysermc.discordbot.util.PropertiesManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.net.InetSocketAddress;
import java.util.Properties;

/**
 * Entry point for the bot's web panel backend. Only the org.geysermc.discordbot.http package
 * (and its config/controller sub-packages) is component-scanned, so this stays isolated from
 * the JDA side of the bot.
 */
@SpringBootApplication
public class WebPanelApplication {

    /**
     * Boots the Spring context on the host/port configured in bot.properties.
     *
     * @return the running application context, so it can be closed again on shutdown
     */
    public static ConfigurableApplicationContext start() {
        InetSocketAddress address = PropertiesManager.getWebAddress();

        GeyserBot.LOGGER.info("Starting panel on {}.", address);

        Properties defaultProperties = new Properties();
        defaultProperties.setProperty("server.address", address.getHostString());
        defaultProperties.setProperty("server.port", String.valueOf(address.getPort()));
        defaultProperties.setProperty("spring.main.banner-mode", "off");

        SpringApplication app = new SpringApplication(WebPanelApplication.class);
        app.setDefaultProperties(defaultProperties);
        return app.run();
    }
}
