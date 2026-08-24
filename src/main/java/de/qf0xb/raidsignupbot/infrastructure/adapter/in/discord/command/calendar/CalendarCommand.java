package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.calendar;

import de.qf0xb.raidsignupbot.application.service.calendar.RaidCalendarService;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.calendar.RaidCalendarPublisher;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.RaidCommand;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

@ApplicationScoped
public class CalendarCommand extends RaidCommand {
  @Inject
  RaidCalendarService calendarService;

  @Inject
  RaidCalendarPublisher publisher;

  public CalendarCommand() {
    super("calendar", "Refresh and show the raid calendar.");
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    if (event.getGuild() == null) {
      event.reply("This command can only be used in a guild.").setEphemeral(true).queue();
      return;
    }
    try {
      var server = calendarService.requireCalendarConfigured(event.getGuild().getId());
      publisher.publish(event.getGuild().getId());
      event.reply("Raid calendar refreshed in <#" + server.getRaidCalendarChannelId() + ">.")
          .setEphemeral(true).queue();
    } catch (RuntimeException ex) {
      event.reply("Could not refresh raid calendar: " + ex.getMessage()).setEphemeral(true).queue();
    }
  }

  @Override
  public void autoComplete(CommandAutoCompleteInteractionEvent event) {
  }
}
