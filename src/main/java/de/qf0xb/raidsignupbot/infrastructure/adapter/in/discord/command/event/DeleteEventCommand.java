package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.event;

import de.qf0xb.raidsignupbot.application.port.EventApplicationPort;
import de.qf0xb.raidsignupbot.application.service.event.EventService;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.CommandAccessLevel;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.RaidCommand;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@ApplicationScoped
public class DeleteEventCommand extends RaidCommand {
  private static final String OPTION_NAME = "event";
  private static final int MAX_CHOICES = 25;
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")
      .withZone(ZoneId.systemDefault());

  @Inject
  EventService eventService;

  @Inject
  EventApplicationPort eventApplicationPort;

  public DeleteEventCommand() {
    super("deleteevent", "Permanently deletes an event, its signup and teams. Admins only.", CommandAccessLevel.ADMIN);
    addOption(new OptionData(OptionType.STRING, OPTION_NAME, "The event to delete", true, true));
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    if (event.getGuild() == null) {
      event.reply("This command can only be used in a server.").setEphemeral(true).queue();
      return;
    }
    event.deferReply(true).queue();

    String serverId = event.getGuild().getId();
    String value = Objects.requireNonNull(event.getOption(OPTION_NAME), "Missing required option: " + OPTION_NAME)
        .getAsString();
    Long eventId = parseEventId(value);
    Event raidEvent = eventId == null ? null : eventService.findEventByIdOptional(eventId)
        .filter(candidate -> belongsToServer(candidate, serverId))
        .orElse(null);
    if (raidEvent == null) {
      event.getHook().sendMessage("Event not found. Pick an event from the list.").queue();
      return;
    }

    Event deleted = eventService.deleteEventEverywhere(raidEvent.getId());

    event.getHook().sendMessage("Deleted **" + describe(deleted) + "**. Its calendar entry is free again.").queue();
  }

  @Override
  public void autoComplete(CommandAutoCompleteInteractionEvent event) {
    if (event.getGuild() == null || !event.getFocusedOption().getName().equals(OPTION_NAME)) {
      event.replyChoices(List.of()).queue();
      return;
    }

    String serverId = event.getGuild().getId();
    String typed = event.getFocusedOption().getValue().toLowerCase(Locale.ROOT);
    List<Command.Choice> choices = eventApplicationPort.findAllEvents().stream()
        .filter(candidate -> belongsToServer(candidate, serverId))
        .sorted(Comparator.comparing(Event::getStart, Comparator.nullsLast(Comparator.naturalOrder())))
        .map(candidate -> new Command.Choice(truncate(describe(candidate)), String.valueOf(candidate.getId())))
        .filter(choice -> choice.getName().toLowerCase(Locale.ROOT).contains(typed))
        .limit(MAX_CHOICES)
        .toList();
    event.replyChoices(choices).queue();
  }

  private boolean belongsToServer(Event candidate, String serverId) {
    return candidate.getGuild() != null && serverId.equals(candidate.getGuild().getServerId());
  }

  private String describe(Event candidate) {
    String start = candidate.getStart() == null ? "no date" : DATE_FORMAT.format(candidate.getStart());
    String name = candidate.getTemplate() == null ? "Event " + candidate.getId() : candidate.getTemplate().getName();
    return String.format("%s · %s · %s", name, candidate.getGuild().getName(), start);
  }

  // Discord limits choice names to 100 characters.
  private String truncate(String name) {
    return name.length() <= 100 ? name : name.substring(0, 99) + "…";
  }

  private Long parseEventId(String value) {
    try {
      return Long.parseLong(value.trim());
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
