package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.almightyalpaca.discord.jdabutler.util.MavenUtil;
import com.almightyalpaca.discord.jdabutler.util.MiscUtils;
import com.kantenkugel.discordbot.versioncheck.VersionCheckerRegistry;
import com.kantenkugel.discordbot.versioncheck.items.VersionedItem;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

public class MavenProjectCommand extends Command
{
    private static String POM;

    static
    {
        try
        {
            MavenProjectCommand.POM = new BufferedReader(new InputStreamReader(MavenProjectCommand.class.getResourceAsStream("/maven.pom"))).lines().collect(Collectors.joining("\n"));
        }
        catch (final Exception e)
        {
            Bot.LOG.error("Error getting template pom", e);
            MavenProjectCommand.POM = "Load failed.";
        }
    }

    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        List<VersionedItem> items = VersionCheckerRegistry.getItemsFromString(content, true).stream()
                //only allow items which use maven for versioning
                .filter(item -> item.getGroupId() != null && item.getArtifactId() != null && item.getRepoType() != null)
                .collect(Collectors.toList());

        if (content.contains("logging"))
            items.add(MiscUtils.LOGBACK_CLASSIC);

        //dependency-string:
        String dependencyString = MavenUtil.getDependencyBlock(items, "    ");

        //repo-string
        String repoString = MavenUtil.getRepositoryBlock(items, "    ");

        final String pom = String.format(MavenProjectCommand.POM, repoString, dependencyString);
        reply(event, "Here: " + MiscUtils.hastebin(pom) + ".xml");
    }

    @Override
    public String getHelp()
    {
        return "Example maven project";
    }

    @Override
    public String getName()
    {
        return "pom.xml";
    }
}
