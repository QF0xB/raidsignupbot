package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class PingCommand extends Command {
  public PingCommand() {
    super("ping", "Sends a ping message");
    addOption(new OptionData(OptionType.STRING, "message", "The message to send"));
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    event.reply("Pong!").setEphemeral(true).queue();
  }
}
