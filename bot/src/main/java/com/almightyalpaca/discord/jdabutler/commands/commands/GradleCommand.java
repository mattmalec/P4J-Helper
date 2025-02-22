package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.commands.Command;
import com.almightyalpaca.discord.jdabutler.util.EmbedUtil;
import com.almightyalpaca.discord.jdabutler.util.MiscUtils;
import com.almightyalpaca.discord.jdabutler.util.gradle.GradleUtil;
import com.kantenkugel.discordbot.versioncheck.VersionCheckerRegistry;
import com.kantenkugel.discordbot.versioncheck.items.VersionedItem;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.List;
import java.util.stream.Collectors;

public class GradleCommand extends Command
{
    private static final String[] ALIASES = {"gradle.kts"};

    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        final EmbedBuilder eb = new EmbedBuilder().setAuthor("Gradle dependencies", null, EmbedUtil.getJDAIconUrl());

        List<VersionedItem> items = VersionCheckerRegistry.getItemsFromString(content, true).stream()
                //only allow items which use maven for versioning
                .filter(item -> item.getGroupId() != null && item.getArtifactId() != null && item.getRepoType() != null)
                .collect(Collectors.toList());

        final boolean pretty = content.contains("pretty");
        final boolean kotlin = message.getContentRaw().contains("gradle.kts");

        if (content.contains("logging"))
            items.add(MiscUtils.LOGBACK_CLASSIC);

        final String lang = kotlin ? "kotlin" : "gradle";
        String description = String.format("If you don't know gradle type `!build.gradle%s` for a complete gradle build file", kotlin ? ".kts" : "")
                + "\n\n```" + lang + "\n"
                + GradleUtil.getDependencyBlock(kotlin, items, pretty) + "\n"
                + "\n"
                + GradleUtil.getRepositoryBlock(kotlin, items) + "\n"
                + "```";

        eb.setDescription(description);
        EmbedUtil.setColor(eb);
        reply(event, eb.build());
    }

    @Override
    public String getHelp()
    {
        return "Shows the gradle `implementation ...` line";
    }

    @Override
    public String getName()
    {
        return "gradle";
    }

    @Override
    public String[] getAliases() {
        return ALIASES;
    }
}
