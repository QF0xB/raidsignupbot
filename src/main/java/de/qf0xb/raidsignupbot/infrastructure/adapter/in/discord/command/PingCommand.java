package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command;

import de.qf0xb.raidsignupbot.application.port.EventUIApplicationPort;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.DiscordBot;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class PingCommand extends Command {
  private final EventUIApplicationPort eventUIApplicationPort;
  private final DiscordBot discordBot;

  public PingCommand(EventUIApplicationPort eventUIApplicationPort, DiscordBot discordBot) {
    super("ping", "Sends a ping message");
    this.eventUIApplicationPort = eventUIApplicationPort;
    this.discordBot = discordBot;
    addOption(new OptionData(OptionType.STRING, "message", "The message to send"));
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    event.reply("Pong!").setEphemeral(true).queue();
  }
}
