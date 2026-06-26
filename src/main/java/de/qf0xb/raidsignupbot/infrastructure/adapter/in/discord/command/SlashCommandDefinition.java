package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collection;

public interface SlashCommandDefinition {
  String getCommandName();

  String getDescription();

  Collection<? extends OptionData> getOptions();

  default CommandAccessLevel getAccessLevel() {
    return CommandAccessLevel.PUBLIC;
  }

  void execute(SlashCommandInteractionEvent event);

  void autoComplete(CommandAutoCompleteInteractionEvent event);

  boolean hasName(String name);
}
