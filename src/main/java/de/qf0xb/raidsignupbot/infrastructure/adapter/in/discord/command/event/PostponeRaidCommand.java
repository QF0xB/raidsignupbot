package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.event;

import de.qf0xb.raidsignupbot.application.port.EventApplicationPort;
import de.qf0xb.raidsignupbot.application.service.event.EventService;
import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.calendar.RaidCalendarPublisher;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.CommandAccessLevel;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.RaidCommand;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Slf4j
@ApplicationScoped
public class PostponeRaidCommand extends RaidCommand {
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")
      .withZone(ZoneId.systemDefault());

  @Inject
  EventService eventService;
  @Inject
  EventApplicationPort eventApplicationPort;
  @Inject
  ServerService serverService;
  @Inject
  RaidCalendarPublisher calendarPublisher;

  public PostponeRaidCommand() {
    super("postponeraid", "Delays a raid while preserving its signups, teams, and duration.", CommandAccessLevel.MANAGE_EVENTS);
    addOption(new OptionData(OptionType.STRING, "event", "The raid to postpone", true, true));
    addOption(new OptionData(OptionType.INTEGER, "minutes", "How many minutes to delay the raid", true)
        .setRequiredRange(1, Integer.MAX_VALUE));
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    if (event.getGuild() == null) {
      event.reply("This command can only be used in a server.").setEphemeral(true).queue();
      return;
    }
    event.deferReply(true).queue();
    String serverId = event.getGuild().getId();
    Event postponed;
    try {
      long eventId = Long.parseLong(Objects.requireNonNull(event.getOption("event")).getAsString().trim());
      int minutes = Objects.requireNonNull(event.getOption("minutes")).getAsInt();
      postponed = eventService.postponeEvent(serverId, eventId, minutes);
    } catch (NumberFormatException ex) {
      event.getHook().sendMessage("Pick a raid from the event list.").queue();
      return;
    } catch (IllegalArgumentException ex) {
      event.getHook().sendMessage(ex.getMessage()).queue();
      return;
    }

    String calendarNotice = "";
    if (serverService.findServerById(serverId).getRaidCalendarChannelId() != null) {
      try {
        calendarPublisher.publish(serverId);
      } catch (RuntimeException ex) {
        log.warn("Raid {} was postponed but its calendar message could not be refreshed", postponed.getId(), ex);
        calendarNotice = " The calendar message could not be refreshed; use /calendar to retry.";
      }
    }
    event.getHook().sendMessage(String.format("Postponed **%s** to <t:%d:F> (<t:%d:R>).%s",
        postponed.getTemplate().getName(), postponed.getStart().getEpochSecond(),
        postponed.getStart().getEpochSecond(), calendarNotice)).queue();
  }

  @Override
  public void autoComplete(CommandAutoCompleteInteractionEvent event) {
    if (event.getGuild() == null || !event.getFocusedOption().getName().equals("event")) {
      event.replyChoices(List.of()).queue();
      return;
    }
    String serverId = event.getGuild().getId();
    String typed = event.getFocusedOption().getValue().toLowerCase(Locale.ROOT);
    Instant now = Instant.now();
    List<Command.Choice> choices = eventApplicationPort.findAllEvents().stream()
        .filter(candidate -> candidate.getGuild() != null && serverId.equals(candidate.getGuild().getServerId()))
        .filter(candidate -> candidate.getStart() != null && candidate.getStart().isAfter(now))
        .sorted(Comparator.comparing(Event::getStart))
        .map(candidate -> new Command.Choice(describe(candidate), String.valueOf(candidate.getId())))
        .filter(choice -> choice.getName().toLowerCase(Locale.ROOT).contains(typed))
        .limit(25).toList();
    event.replyChoices(choices).queue();
  }

  private String describe(Event event) {
    String name = event.getTemplate().getName() + " · " + event.getGuild().getName() + " · " + DATE_FORMAT.format(event.getStart());
    return name.length() <= 100 ? name : name.substring(0, 99) + "…";
  }
}
