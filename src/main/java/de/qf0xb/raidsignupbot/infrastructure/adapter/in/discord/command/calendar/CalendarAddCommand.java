package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.calendar;

import de.qf0xb.raidsignupbot.application.port.guild.GuildApplicationPort;
import de.qf0xb.raidsignupbot.application.port.template.EventTemplateApplicationPort;
import de.qf0xb.raidsignupbot.application.service.calendar.RaidCalendarService;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.calendar.RaidCalendarPublisher;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.CommandAccessLevel;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.RaidCommand;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class CalendarAddCommand extends RaidCommand {
  private static final String GUILD = "guild";
  private static final String TEMPLATE = "template";
  private static final String HOSTER = "hoster";
  private static final String START = "start";
  private static final String DURATION = "duration";

  @Inject
  RaidCalendarService calendarService;

  @Inject
  RaidCalendarPublisher publisher;

  @Inject
  GuildApplicationPort guildApplicationPort;

  @Inject
  EventTemplateApplicationPort templateApplicationPort;

  public CalendarAddCommand() {
    super("calendaradd", "Add an event to the raid calendar.", CommandAccessLevel.MANAGE_GUILDS);
    addOption(new OptionData(OptionType.STRING, GUILD, "The guild hosting the event", true, true));
    addOption(new OptionData(OptionType.STRING, TEMPLATE, "The event template", true, true));
    addOption(new OptionData(OptionType.USER, HOSTER, "The event host", true));
    addOption(new OptionData(OptionType.STRING, START, "Start time as Unix epoch seconds", true));
    addOption(new OptionData(OptionType.STRING, DURATION, "Event duration", true)
        .addChoices(
            new Command.Choice("1 hour", "1h"),
            new Command.Choice("1.5 hours", "1,5h"),
            new Command.Choice("2 hours", "2h"),
            new Command.Choice("3 hours", "3h")));
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    if (event.getGuild() == null) {
      event.reply("This command can only be used in a guild.").setEphemeral(true).queue();
      return;
    }

    try {
      Instant start = Instant.ofEpochSecond(Long.parseLong(event.getOption(START).getAsString()));
      Duration duration = parseDuration(event.getOption(DURATION).getAsString());
      User hoster = event.getOption(HOSTER).getAsUser();
      var entry = calendarService.createEntry(
          event.getGuild().getId(),
          event.getOption(GUILD).getAsString(),
          hoster.getId(),
          event.getOption(TEMPLATE).getAsString(),
          start,
          start.plus(duration));
      publisher.publish(event.getGuild().getId());
      event.reply("Added calendar entry " + entry.getId() + ".").setEphemeral(true).queue();
    } catch (RuntimeException ex) {
      event.reply("Could not add calendar entry: " + ex.getMessage()).setEphemeral(true).queue();
    }
  }

  @Override
  public void autoComplete(CommandAutoCompleteInteractionEvent event) {
    if (event.getGuild() == null) {
      return;
    }
    if (GUILD.equals(event.getFocusedOption().getName())) {
      List<Command.Choice> choices = guildApplicationPort.findGuildByServerId(event.getGuild().getId()).stream()
          .map(guild -> new Command.Choice(guild.getName(), guild.getName()))
          .toList();
      event.replyChoices(choices).queue();
    } else if (TEMPLATE.equals(event.getFocusedOption().getName())) {
      List<Command.Choice> choices = templateApplicationPort.findAllByServerId(event.getGuild().getId()).stream()
          .map(template -> new Command.Choice(template.getKey(), template.getKey()))
          .toList();
      event.replyChoices(choices).queue();
    }
  }

  private Duration parseDuration(String value) {
    return switch (value) {
      case "1h" -> Duration.ofHours(1);
      case "1,5h" -> Duration.ofMinutes(90);
      case "2h" -> Duration.ofHours(2);
      case "3h" -> Duration.ofHours(3);
      default -> throw new IllegalArgumentException("Unsupported duration");
    };
  }
}
