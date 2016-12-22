package com.almightyalpaca.discord.jdabutler.commands;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.MoreExecutors;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.commands.commands.*;

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class Dispatcher extends ListenerAdapter {

	private final Set<Command>		commands	= ConcurrentHashMap.newKeySet();
	private final ExecutorService	pool		= Executors.newCachedThreadPool();

	{
		this.registerCommand(new BuildGradleCommand());
		this.registerCommand(new ChangelogCommand());
		this.registerCommand(new DocsCommand());
		this.registerCommand(new EvalCommand());
		this.registerCommand(new GradleCommand());
		this.registerCommand(new GradleProjectCommand());
		this.registerCommand(new GuildCommand());
		this.registerCommand(new HelpCommand());
		this.registerCommand(new JarCommand());
		this.registerCommand(new MavenCommand());
		this.registerCommand(new NotifyCommand());
		this.registerCommand(new PingCommand());
		this.registerCommand(new ShutdownCommand());
		this.registerCommand(new UptimeCommand());
		this.registerCommand(new VersionsCommand());
	}

	public Set<Command> getCommands() {
		return Collections.unmodifiableSet(new HashSet<>(this.commands));
	}

	@Override
	public void onGuildMessageReceived(final GuildMessageReceivedEvent event) {
		final String prefix = Bot.config.getString("prefix");
		final String message = event.getMessage().getRawContent();
		final TextChannel channel = event.getChannel();

		// In DAPI only listen to messages in #java_jda
		if (channel.getGuild().getId().equals("81384788765712384") && !channel.getId().equals("129750718931271681")) {
			return;
		}

		if (message.toLowerCase().startsWith(prefix.toLowerCase())) {
			for (final Command c : this.getCommands()) {
				if (message.toLowerCase().startsWith(prefix.toLowerCase() + c.getName().toLowerCase() + ' ') || message.equalsIgnoreCase(prefix + c.getName())) {
					executeCommand(c, c.getName(), prefix, event);
					return;
				} else {
					for (final String alias : c.getAliases()) {
						if (message.toLowerCase().startsWith(prefix.toLowerCase() + alias.toLowerCase() + ' ') || message.equalsIgnoreCase(prefix + alias)) {
							executeCommand(c, alias, prefix, event);
							return;
						}
					}
				}
			}
		}
	}

	private void executeCommand(Command c, String alias, String prefix, GuildMessageReceivedEvent event) {
		this.pool.submit(() -> {
			try {
				final String content = this.removePrefix(alias, prefix, event);
				Bot.LOG.info("Dispatching command '" + c.getName().toLowerCase() + "' with: " + content);
				c.dispatch(event.getAuthor(), event.getChannel(), event.getMessage(), content, event);
			} catch (final Exception e) {
				event.getChannel().sendMessage(String.format("**There was an error processing your command!**")).queue();
				Bot.LOG.log(e);
			}
		});
	}

	@Override
	public void onShutdown(final ShutdownEvent event) {
		MoreExecutors.shutdownAndAwaitTermination(this.pool, 10, TimeUnit.SECONDS);
	}

	public boolean registerCommand(final Command command) {
		if (command.getName().contains(" ")) {
			throw new IllegalArgumentException("Name must not have spaces!");
		}
		if (this.commands.stream().map(Command::getName).anyMatch(c -> command.getName().equalsIgnoreCase(c))) {
			return false;
		}
		this.commands.add(command);
		return true;
	}

	private String removePrefix(final String c, final String prefix, final GuildMessageReceivedEvent event) {
		String content = event.getMessage().getRawContent();
		content = content.substring(c.length() + prefix.length());
		if (content.startsWith(" ")) {
			content = content.substring(1);
		}
		return content;
	}
}
