package com.almightyalpaca.discord.jdabutler;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import com.almightyalpaca.discord.jdabutler.commands.Dispatcher;
import com.almightyalpaca.discord.jdabutler.commands.commands.NotifyCommand;
import com.almightyalpaca.discord.jdabutler.config.Config;
import com.almightyalpaca.discord.jdabutler.config.ConfigFactory;
import com.almightyalpaca.discord.jdabutler.config.exception.KeyNotFoundException;
import com.almightyalpaca.discord.jdabutler.config.exception.WrongTypeException;
import com.almightyalpaca.discord.jdabutler.util.MiscUtils;
import com.almightyalpaca.discord.jdabutler.util.gradle.GradleProjectDropboxUtil;
import com.almightyalpaca.discord.jdabutler.util.logging.WebhookAppender;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.kantenkugel.discordbot.jdocparser.JDoc;
import com.kantenkugel.discordbot.versioncheck.VersionCheckerRegistry;
import com.kantenkugel.discordbot.versioncheck.items.VersionedItem;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.internal.JDAImpl;
import okhttp3.OkHttpClient;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Bot
{
    public static Config config;
    public static Dispatcher dispatcher;
    public static final String INVITE_LINK = "https://discord.gg/7fAabrTJZW";
    public static final String PTERODACTYL_INVITE_LINK = "https://discord.gg/pterodactyl";
    public static JDAImpl jda;
    public static boolean isStealth = false;

    public static OkHttpClient httpClient;

    public static EventListener listener;

    public static final Logger LOG = (Logger) LoggerFactory.getLogger(Bot.class);

    public static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(MiscUtils.newThreadFactory("main-executor"));

    public static Guild getGuildP4J()
    {
        return Bot.jda.getGuildById("780230961035608064");
    }

    public static Role getRoleBots()
    {
        return Bot.getGuildP4J().getRoleById("870826527749726208");
    }

    public static Role getRoleStaff()
    {
        return Bot.getGuildP4J().getRoleById("781255691214127116");
    }

    public static boolean isAdmin(final User user)
    {
        final Member member = Bot.getGuildP4J().getMember(user);
        return member != null && member.getRoles().contains(Bot.getRoleStaff());
    }

    public static Role getRoleHelper()
    {
        return Bot.getGuildP4J().getRoleById("781255798306373655");
    }

    public static boolean isHelper(final User user)
    {
        if(isAdmin(user))
            return true;
        final Member member = Bot.getGuildP4J().getMember(user);
        return member != null && member.getRoles().contains(Bot.getRoleHelper());
    }

    public static void main(final String[] args) throws JsonIOException, JsonSyntaxException, WrongTypeException, KeyNotFoundException, IOException, LoginException, IllegalArgumentException, InterruptedException, SecurityException
    {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                LOG.error("There was an uncaught exception in thread {}", thread.getName(), throwable));
        Bot.httpClient = new OkHttpClient();

        EXECUTOR.submit(JDoc::init);

        Bot.config = ConfigFactory.getConfig(new File("config.json"));

        final String token = Bot.config.getString("discord.token", "Your token");
        final JDABuilder builder = JDABuilder.createDefault(token);
        builder.enableIntents(GatewayIntent.GUILD_MEMBERS); // used for join listener
        builder.setMemberCachePolicy(MemberCachePolicy.OWNER.or((member) ->
            // Cache elevated members for specific whitelists
            // this is required for isAdmin and isHelper to work properly
            member.getGuild().getIdLong() == 780230961035608064L //P4J Guild
                && member.getRoles().stream().mapToLong(Role::getIdLong).anyMatch(role ->
                    role == 781255691214127116L || // staff
                    role == 781255798306373655L)   // helper
        ));
        builder.setBulkDeleteSplittingEnabled(false);

        Bot.config.save();
        Bot.listener = new EventListener();
        builder.addEventListeners(Bot.listener);
        builder.addEventListeners(Bot.dispatcher = new Dispatcher());

        builder.setActivity(Activity.playing("Pterodactyl4J"));

        Bot.jda = (JDAImpl) builder.build().awaitReady();

        if(Bot.config.getBoolean("webhook.enabled", false)) {
            LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

            ThresholdFilter filter = new ThresholdFilter();
            filter.setLevel(Bot.config.getString("webhook.level"));
            filter.setContext(lc);
            filter.start();

            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setPattern(Bot.config.getString("webhook.pattern"));
            encoder.setContext(lc);
            encoder.start();

            WebhookAppender appender = new WebhookAppender();
            appender.setEncoder(encoder);
            appender.addFilter(filter);
            appender.setWebhookUrl(Bot.config.getString("webhook.webhookurl"));
            appender.setName("ERROR_WH");
            appender.setContext(lc);
            appender.start();

            Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            root.addAppender(appender);
        }

        NotifyCommand.reloadBlacklist(null);

        EXECUTOR.submit(() ->
        {
            VersionCheckerRegistry.init();
            VersionedItem jdaItem = VersionCheckerRegistry.getItem("pterodactyl4j");
            if(jdaItem.getVersion() != null && jdaItem.parseVersion().build != config.getInt("p4j.version.build"))
            {
                //do not announce here as that might cause duplicate announcements when a new instance is fired up (or a very old one)
                jdaItem.getUpdateHandler().onUpdate(jdaItem, config.getString("p4j.version.name"), false);
            }
            else
            {
//                GradleProjectDropboxUtil.fetchUrl();
            }
        });
        System.out.println("Bot is ready!");

        Scanner scanner = new Scanner(System.in);

        while(scanner.hasNext()) {
            String command = scanner.nextLine();
            if (command.equals("!end"))
                shutdown();
        }
    }

    public static void shutdown()
    {
        shutdown(0);
    }

    public static void shutdown(int code)
    {
        Bot.jda.getRegisteredListeners().forEach(Bot.jda::removeEventListener);

        try
        {
            TimeUnit.SECONDS.sleep(2);
        }
        catch (final InterruptedException ignored)
        {}

        Bot.jda.shutdownNow();
        System.exit(code);
    }
}
