package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord;

import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.PingCommand;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.Command;
import de.qf0xb.raidsignupbot.infrastructure.config.DiscordConfig;
import io.quarkus.runtime.LaunchMode;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jspecify.annotations.NonNull;

import java.util.*;

@Log
@ApplicationScoped
public class CommandManager extends ListenerAdapter {
  private final DiscordConfig discordConfig;

  private final Set<Command> commands = new java.util.HashSet<>();

  public CommandManager(DiscordConfig discordConfig) {
    this.discordConfig = discordConfig;
    addCommand(new PingCommand());
  }

  public void addCommand(Command command) {
    commands.add(command);
  }

  public void addCommandAndRegister(Command command) {
    commands.add(command);
    // no-op here; caller supplies JDA when it is available
  }

  public void registerCommands(net.dv8tion.jda.api.JDA api) {
    Set<CommandData> cmds = new HashSet<>();
    commands.stream().filter(command -> command.getType() == Command.CommandType.SLASH_COMMAND).forEach(command -> {
        cmds.add(Commands.slash(command.getCommandName(), command.getDescription())
            .addOptions(command.getOptions()));
    });

    if (LaunchMode.current() == LaunchMode.DEVELOPMENT) {
      discordConfig.getGuildId()
          .map(api::getGuildById)
          .ifPresent(guild -> guild.updateCommands().addCommands(cmds).queue());
      log.info("Registered commands to guild " + discordConfig.getGuildId().orElseThrow() + ": " + cmds.stream().map(CommandData::getName).toList());
    } else {
      api.updateCommands().addCommands(cmds).queue();
      log.info("Registered commands globally: " + cmds);
    }
  }

  public void unregisterCommand(Command command) {
    commands.remove(command);
  }

  @Override
  public void onSlashCommandInteraction(@NonNull SlashCommandInteractionEvent event) {
    commands.stream().filter(command -> command.hasName(event.getName())).forEach(command -> command.execute(event));
  }
}
