/*
 * Copyright (c) 2020-2026 GeyserMC. http://geysermc.org
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
 * @author GeyserMC
 * @link https://github.com/GeyserMC/GeyserDiscordBot
 */

package org.geysermc.discordbot;

import com.algolia.api.SearchClient;
import com.jagrosh.jdautilities.command.*;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import io.sentry.Sentry;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.messages.MessageRequest;
import org.geysermc.discordbot.commands.BotSlashCommand;
import org.geysermc.discordbot.health_checker.HealthCheckerManager;
import org.geysermc.discordbot.http.WebPanelApplication;
import org.geysermc.discordbot.listeners.*;
import org.geysermc.discordbot.storage.AbstractStorageManager;
import org.geysermc.discordbot.storage.BanData;
import org.geysermc.discordbot.storage.SlowModeInfo;
import org.geysermc.discordbot.storage.StorageType;
import org.geysermc.discordbot.tags.TagsListener;
import org.geysermc.discordbot.tags.TagsManager;
import org.geysermc.discordbot.util.*;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GeyserBot {
    // Instance Variables
    public static final Logger LOGGER = LoggerFactory.getLogger(GeyserBot.class);
    public static final List<Command> COMMANDS;
    public static final Map<String, List<SlashCommand>> GUILD_SLASH_COMMANDS;
    public static final List<SlashCommand> GLOBAL_SLASH_COMMANDS;
    public static final List<ContextMenu> CONTEXT_MENUS;

    public static AbstractStorageManager storageManager;

    private static ScheduledExecutorService generalThreadPool;

    private static JDA jda;
    private static GitHub github;
    private static ConfigurableApplicationContext webPanelContext;
    private static SearchClient algolia;

    static {
        // Gathers all commands from "commands" package.
        List<Command> commands = new ArrayList<>();
        List<SlashCommand> slashCommands = new ArrayList<>();
        try {
            Reflections reflections = new Reflections("org.geysermc.discordbot.commands");
            Set<Class<? extends Command>> subTypes = reflections.getSubTypesOf(Command.class);
            for (Class<? extends Command> theClass : subTypes) {
                // Don't load SubCommands
                if (theClass.getName().contains("SubCommand")) {
                    continue;
                }
                try {
                    commands.add(theClass.getDeclaredConstructor().newInstance());
                    LoggerFactory.getLogger(theClass).debug("Loaded Command Successfully!");
                } catch (InstantiationException e) {
                    // Safe to ignore, we probably tried to load a Slash Command
                }
            }

            Set<Class<? extends SlashCommand>> slashSubTypes = reflections.getSubTypesOf(SlashCommand.class);
            for (Class<? extends SlashCommand> theClass : slashSubTypes) {
                // Don't load SubCommands
                if (theClass.getName().contains("SubCommand")) {
                    continue;
                }
                // Don't load abstract classes
                if (Modifier.isAbstract(theClass.getModifiers())) {
                    continue;
                }

                slashCommands.add(theClass.getDeclaredConstructor().newInstance());
                LoggerFactory.getLogger(theClass).debug("Loaded SlashCommand Successfully!");
            }
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            LOGGER.error("Unable to load commands", e);
        }
        COMMANDS = commands;

        GLOBAL_SLASH_COMMANDS = new ArrayList<>();
        GUILD_SLASH_COMMANDS = new HashMap<>();

        for (SlashCommand command : slashCommands) {
            if (command instanceof BotSlashCommand botCommand) {
                if (botCommand.getValidServers().length == 0) {
                    GLOBAL_SLASH_COMMANDS.add(botCommand);
                    continue;
                }

                for (BotServers servers : botCommand.getValidServers()) {
                    GUILD_SLASH_COMMANDS.computeIfAbsent(servers.getServerId(), key -> new ArrayList<>()).add(botCommand);
                }
            } else {
                GLOBAL_SLASH_COMMANDS.add(command);
            }
        }

        // Gathers all context menu items from "context_menu" package.
        List<ContextMenu> contextMenus = new ArrayList<>();
        try {
            Reflections reflections = new Reflections("org.geysermc.discordbot.context_menus");
            Set<Class<? extends ContextMenu>> subTypes = reflections.getSubTypesOf(ContextMenu.class);
            for (Class<? extends ContextMenu> theClass : subTypes) {
                if (!theClass.getPackageName().startsWith("org.geysermc.discordbot.context_menus")) {
                    continue;
                }
                contextMenus.add(theClass.getDeclaredConstructor().newInstance());
                LoggerFactory.getLogger(theClass).debug("Loaded ContextMenu Successfully!");
            }
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            LOGGER.error("Unable to load context menus", e);
        }
        CONTEXT_MENUS = contextMenus;
    }

    public static void main(String[] args) throws IOException {
        // Load properties into the PropertiesManager
        Properties prop = new Properties();
        prop.load(new FileInputStream("bot.properties"));
        PropertiesManager.loadProperties(prop);

        // Setup sentry.io
        if (PropertiesManager.getSentryDsn() != null) {
            LOGGER.info("Loading sentry.io...");
            Sentry.init(options -> {
                options.setDsn(PropertiesManager.getSentryDsn());
                options.setEnvironment(PropertiesManager.getSentryEnv());
                options.setEnabled(true);
                LOGGER.info("Sentry.io loaded");
            });
        }

        // Connect to github
        github = new GitHubBuilder().withOAuthToken(PropertiesManager.getGithubToken()).build();

        // Connect to Algolia
        algolia = new SearchClient(PropertiesManager.getAlgoliaApplicationId(), PropertiesManager.getAlgoliaSearchApiKey());

        // Initialize the waiter
        EventWaiter waiter = new EventWaiter();

        // Load filters
        SwearHandler.loadFilters();

        // Load the db
        StorageType storageType = StorageType.getByName(PropertiesManager.getDatabaseType());
        if (storageType == StorageType.UNKNOWN) {
            LOGGER.error("Invalid database type! '" + PropertiesManager.getDatabaseType() + "'");
            System.exit(1);
        }

        try {
            storageManager = storageType.getStorageManager().getDeclaredConstructor().newInstance();
            storageManager.setupStorage();
        } catch (Exception e) {
            LOGGER.error("Unable to create database link!", e);
            System.exit(1);
        }

        CommandClient[] clients = new CommandClient[GUILD_SLASH_COMMANDS.size() + 2];

        // Setup the global command client
        CommandClientBuilder client = new CommandClientBuilder();
        client.setActivity(null);
        client.setOwnerId("0"); // No owner
        client.setPrefix(PropertiesManager.getPrefix());
        client.useHelpBuilder(false);
        client.addCommands(COMMANDS.toArray(new Command[0]));
        client.addSlashCommands(GLOBAL_SLASH_COMMANDS.toArray(new SlashCommand[0]));
        client.addContextMenus(CONTEXT_MENUS.toArray(new ContextMenu[0]));
        client.setListener(new CommandErrorHandler());
        client.setCommandPreProcessBiFunction((event, command) -> !SwearHandler.filteredMessages.contains(event.getMessage().getIdLong()));
        clients[0] = client.build();

        // Then setup the tag client, this can be global and just filtered per-server
        CommandClientBuilder tagClient = new CommandClientBuilder();
        tagClient.setActivity(null);
        tagClient.setOwnerId("0"); // No owner
        String tagPrefix = PropertiesManager.getPrefix() + PropertiesManager.getPrefix();
        tagClient.setPrefix(tagPrefix);
        tagClient.setPrefixes(new String[] {"!tag "});
        tagClient.useHelpBuilder(false);
        tagClient.addCommands(TagsManager.getTags().toArray(new Command[0]));
        tagClient.setListener(new TagsListener());
        tagClient.setCommandPreProcessBiFunction((event, command) -> !SwearHandler.filteredMessages.contains(event.getMessage().getIdLong()));
        tagClient.setManualUpsert(true);
        clients[1] = tagClient.build();

        // Now setup server-specific commands
        int index = 2;
        for (Map.Entry<String, List<SlashCommand>> entry : GUILD_SLASH_COMMANDS.entrySet()) {
            CommandClientBuilder guildClient = new CommandClientBuilder();
            guildClient.forceGuildOnly(entry.getKey());
            guildClient.setActivity(null);
            guildClient.setOwnerId("0"); // No owner
            guildClient.setPrefix(PropertiesManager.getPrefix());
            guildClient.useHelpBuilder(false);
            guildClient.addSlashCommands(entry.getValue().toArray(new SlashCommand[0]));
            guildClient.setListener(new CommandErrorHandler());
            guildClient.setCommandPreProcessBiFunction((event, command) -> !SwearHandler.filteredMessages.contains(event.getMessage().getIdLong()));
            clients[index++] = guildClient.build();
        }

        // Disable pings on replies
        MessageRequest.setDefaultMentionRepliedUser(false);

        // Setup the thread pool
        generalThreadPool = Executors.newScheduledThreadPool(5);

        // Register JDA
        try {
            jda = JDABuilder.createDefault(PropertiesManager.getToken())
                    .setChunkingFilter(ChunkingFilter.ALL)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES, GatewayIntent.MESSAGE_CONTENT)
                    .enableCache(CacheFlag.ACTIVITY)
                    .enableCache(CacheFlag.ROLE_TAGS)
                    .setStatus(OnlineStatus.ONLINE)
                    .setActivity(Activity.playing("Booting..."))
                    .setEnableShutdownHook(true)
                    .setEventManager(new SentryEventManager())
                    .setMaxReconnectDelay(60)
                    .addEventListeners(waiter,
                            new LogHandler(),
                            new SwearHandler(),
                            new PersistentRoleHandler(),
                            new FileHandler(),
                            new ErrorAnalyzer(),
                            new ShutdownHandler(),
                            new BadLinksHandler(),
                            new HelpHandler(),
                            new DeleteHandler(),
                            new AutoModHandler(),
                            new WebhookLinkPreviewHandler(),
                            new ReadyListener(),
                            new HoneyPotHandler(),
                            new QuarantineHandler(),
                            new SpamHandler(),
                            new RolesMessageHandler(),
                            new ConfigurationModalHandler(),
                            new TicketHandlers.Create(),
                            new TicketHandlers.Action(),
                            new TicketHandlers.Logs(),
                            new ExploitMessageHandler(),
                            new GeyserDumpHandler(),
                            new LevelHandler(),
                            new PreviewHandler()
                    )
                    .addEventListeners((Object[]) clients)
                    .build();
        } catch (IllegalArgumentException exception) {
            LOGGER.error("Failed to initialize JDA!", exception);
            System.exit(1);
        }

        // Register listeners
        jda.addEventListener();

        // Setup the web panel (Spring Boot, replaces the old raw HttpServer)
        if (PropertiesManager.enableWeb()) {
            try {
                webPanelContext = WebPanelApplication.start();
            } catch (Exception e) {
                LOGGER.error("Failed to start the web panel!", e);
            }
        }

        // Setup the health check scheduler
        HealthCheckerManager.setup();

        // Setup the rss feed check scheduler
        RssFeedManager.setup();

        // Setup all slow mode handlers
        generalThreadPool.schedule(() -> {
            for (Guild guild : jda.getGuilds()) {
                for (SlowModeInfo info : storageManager.getSlowModeChannels(guild)) {
                    jda.addEventListener(new SlowmodeHandler(info.getChannel(), info.getDelay()));
                }
            }
        }, 5, TimeUnit.SECONDS);

        generalThreadPool.scheduleAtFixedRate(() -> {
            for (BanData banData : storageManager.getBans()) {
                if (banData.expiryTime() == null) continue;
                if (Instant.now().isAfter(banData.expiryTime())) {
                    storageManager.removeBan(banData.userId());
                    ModerationHelper.revokeBan(banData.userId(), null, "Ban has expired.");
                }
            }
        }, 1, 1, TimeUnit.MINUTES);

        jda.getPresence().setActivity(null);
    }

    public static JDA getJDA() {
        return jda;
    }

    public static GitHub getGithub() {
        return github;
    }

    public static SearchClient getAlgolia() {
        return algolia;
    }

    public static ScheduledExecutorService getGeneralThreadPool() {
        return generalThreadPool;
    }

    public static void shutdown() {
        GeyserBot.LOGGER.info("Shutting down storage...");
        storageManager.closeStorage();
        GeyserBot.LOGGER.info("Shutting down thread pool...");
        generalThreadPool.shutdown();
        if (webPanelContext != null) {
            GeyserBot.LOGGER.info("Shutting down web panel...");
            webPanelContext.close();
        }
        GeyserBot.LOGGER.info("Finished shutdown, exiting!");
        System.exit(0);
    }
}
