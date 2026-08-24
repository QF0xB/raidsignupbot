package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.calendar;

import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.domain.model.server.Server;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.calendar.RaidCalendarPublisher;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.CommandAccessLevel;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.RaidCommand;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

@ApplicationScoped
public class CalendarConfigCommand extends RaidCommand {
  private static final String CHANNEL = "channel";
  private static final String OWNER = "owner";

  @Inject
  ServerService serverService;

  @Inject
  RaidCalendarPublisher publisher;

  public CalendarConfigCommand() {
    super("calendarconfig", "Configure the raid calendar channel and owner.", CommandAccessLevel.MANAGE_GUILDS);
    addOption(new OptionData(OptionType.CHANNEL, CHANNEL, "The text channel for the raid calendar", false)
        .setChannelTypes(ChannelType.TEXT));
    addOption(new OptionData(OptionType.USER, OWNER, "The only member allowed to create calendar events", false));
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    if (event.getGuild() == null) {
      event.reply("This command can only be used in a guild.").setEphemeral(true).queue();
      return;
    }
    if (event.getOption(CHANNEL) == null && event.getOption(OWNER) == null) {
      event.reply("Provide a calendar channel, an owner, or both.").setEphemeral(true).queue();
      return;
    }

    String channelId = event.getOption(CHANNEL) == null ? null : event.getOption(CHANNEL).getAsChannel().getId();
    String ownerId = event.getOption(OWNER) == null ? null : event.getOption(OWNER).getAsUser().getId();
    Server server = serverService.updateCalendarSettings(event.getGuild().getId(), channelId, ownerId);
    if (server.getRaidCalendarChannelId() != null && server.getRaidCalendarOwnerId() != null) {
      publisher.publish(event.getGuild().getId());
    }
    String channel = server.getRaidCalendarChannelId() == null
        ? "not configured"
        : "<#" + server.getRaidCalendarChannelId() + ">";
    String owner = server.getRaidCalendarOwnerId() == null
        ? "not configured"
        : "<@" + server.getRaidCalendarOwnerId() + ">";
    event.reply("Raid calendar configured: channel " + channel + ", owner " + owner + ".")
        .setEphemeral(true)
        .queue();
  }

  @Override
  public void autoComplete(CommandAutoCompleteInteractionEvent event) {
  }
}
