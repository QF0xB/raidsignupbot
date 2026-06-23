package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command;

import lombok.Getter;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Getter
public abstract class Command {
  private final String commandName;
  private final String description;

  private final Set<OptionData> commandOptions = new HashSet<>();

  private final CommandType type;

  public Command(String commandName, String description) {
    this(commandName, description, CommandType.SLASH_COMMAND);
  }

  public Command(String commandName, String description, CommandType type) {
    this.commandName = commandName;
    this.description = description;
    this.type = type;
  }

  public abstract void execute(SlashCommandInteractionEvent event);

  public void addOption(OptionData option) {
    commandOptions.add(option);
  }

  public boolean hasName(String name) {
    return commandName.equalsIgnoreCase(name.strip());
  }

  public Collection<? extends OptionData> getOptions() {
    return commandOptions;
  }

  public enum CommandType {
    SLASH_COMMAND
  }
}
