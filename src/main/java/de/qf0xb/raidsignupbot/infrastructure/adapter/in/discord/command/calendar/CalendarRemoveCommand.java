package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.calendar;

import de.qf0xb.raidsignupbot.application.service.calendar.RaidCalendarService;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.calendar.RaidCalendarPublisher;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.CommandAccessLevel;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.RaidCommand;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

@ApplicationScoped
public class CalendarRemoveCommand extends RaidCommand {
  private static final String ENTRY = "entry";

  @Inject
  RaidCalendarService calendarService;

  @Inject
  RaidCalendarPublisher publisher;

  public CalendarRemoveCommand() {
    super("calendarremove", "Remove an event from the raid calendar.", CommandAccessLevel.MANAGE_GUILDS);
    addOption(new OptionData(OptionType.INTEGER, ENTRY, "The calendar entry", true, true));
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    if (event.getGuild() == null) {
      event.reply("This command can only be used in a guild.").setEphemeral(true).queue();
      return;
    }
    try {
      long entryId = event.getOption(ENTRY).getAsLong();
      calendarService.deleteEntry(event.getGuild().getId(), entryId);
      publisher.publish(event.getGuild().getId());
      event.reply("Removed calendar entry " + entryId + ".").setEphemeral(true).queue();
    } catch (RuntimeException ex) {
      event.reply("Could not remove calendar entry: " + ex.getMessage()).setEphemeral(true).queue();
    }
  }

  @Override
  public void autoComplete(CommandAutoCompleteInteractionEvent event) {
    if (event.getGuild() == null || !ENTRY.equals(event.getFocusedOption().getName())) {
      return;
    }
    event.replyChoices(calendarService.findEntries(event.getGuild().getId()).stream()
        .filter(entry -> entry.getEventId() == null)
        .limit(25)
        .map(entry -> new Command.Choice(
            entry.getId() + " - " + entry.getGuildName() + " / " + entry.getTemplateKey(),
            entry.getId()))
        .toList()).queue();
  }
}
