package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class RaidCommand implements SlashCommandDefinition {
  private final String commandName;
  private final String description;
  private final CommandAccessLevel accessLevel;
  private final Set<OptionData> options = new HashSet<>();

  public RaidCommand(String commandName, String description) {
    this(commandName, description, CommandAccessLevel.PUBLIC);
  }

  public RaidCommand(String commandName, String description, CommandAccessLevel accessLevel) {
    this.commandName = commandName;
    this.description = description;
    this.accessLevel = accessLevel;
  }

  public void addOption(OptionData option) {
    options.add(option);
  }

  @Override
  public String getCommandName() {
    return commandName;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public Collection<? extends OptionData> getOptions() {
    return options;
  }

  @Override
  public CommandAccessLevel getAccessLevel() {
    return accessLevel;
  }

  @Override public abstract void execute(SlashCommandInteractionEvent event);

  @Override public abstract void autoComplete(CommandAutoCompleteInteractionEvent event);

  @Override
  public boolean hasName(String name) {
    return getCommandName().equalsIgnoreCase(name.strip());
  }
}
