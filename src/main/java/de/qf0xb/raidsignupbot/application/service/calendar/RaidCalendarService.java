package de.qf0xb.raidsignupbot.application.service.calendar;

import de.qf0xb.raidsignupbot.application.port.RaidCalendarApplicationPort;
import de.qf0xb.raidsignupbot.application.port.EventApplicationPort;
import de.qf0xb.raidsignupbot.application.port.template.EventTemplateApplicationPort;
import de.qf0xb.raidsignupbot.application.service.guild.GuildService;
import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.domain.model.calendar.RaidCalendarEntry;
import de.qf0xb.raidsignupbot.domain.model.server.Server;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class RaidCalendarService {
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE dd.MM.yy")
      .withZone(ZoneId.systemDefault());

  private final RaidCalendarApplicationPort calendarPort;
  private final EventApplicationPort eventPort;
  private final ServerService serverService;
  private final GuildService guildService;
  private final EventTemplateApplicationPort templatePort;

  public RaidCalendarService(
      RaidCalendarApplicationPort calendarPort,
      EventApplicationPort eventPort,
      ServerService serverService,
      GuildService guildService,
      EventTemplateApplicationPort templatePort) {
    this.calendarPort = calendarPort;
    this.eventPort = eventPort;
    this.serverService = serverService;
    this.guildService = guildService;
    this.templatePort = templatePort;
  }

  @Transactional
  public RaidCalendarEntry createEntry(
      String serverId,
      String guildName,
      String hosterUserId,
      String templateKey,
      Instant start,
      Instant end) {
    Server server = serverService.findServerById(serverId);
    requireCalendarConfigured(server);
    guildService.findGuildByServerIdAndName(serverId, guildName);
    templatePort.findByServerIdAndKey(serverId, templateKey)
        .orElseThrow(() -> new IllegalArgumentException("Template " + templateKey + " not found"));
    validateTimes(start, end);
    java.time.Duration duration = java.time.Duration.between(start, end);
    Instant calendarStart = start.atZone(ZoneId.systemDefault()).toLocalDate()
        .atStartOfDay(ZoneId.systemDefault()).toInstant();
    ensureGuildHasNoRaidOnDay(serverId, guildName, calendarStart, null, null);

    return calendarPort.save(RaidCalendarEntry.builder()
        .serverId(serverId)
        .guildName(guildName)
        .hosterUserId(hosterUserId)
        .templateKey(templateKey)
        .start(calendarStart)
        .end(calendarStart.plus(duration))
        .build());
  }

  @Transactional
  public List<RaidCalendarEntry> findUpcomingEntries(String serverId) {
    LocalDate today = LocalDate.now(ZoneId.systemDefault());
    return calendarPort.findByServerId(serverId).stream()
        .filter(entry -> !entry.getStart().atZone(ZoneId.systemDefault()).toLocalDate().isBefore(today))
        .sorted(Comparator.comparing(RaidCalendarEntry::getStart))
        .toList();
  }

  @Transactional
  public List<RaidCalendarEntry> findUpcomingAvailableEntries(String serverId) {
    return findUpcomingEntries(serverId).stream()
        .filter(entry -> entry.getEventId() == null)
        .toList();
  }

  @Transactional
  public List<RaidCalendarEntry> findEntries(String serverId) {
    return calendarPort.findByServerId(serverId).stream()
        .sorted(Comparator.comparing(RaidCalendarEntry::getStart))
        .toList();
  }

  @Transactional
  public RaidCalendarEntry findEntry(Long entryId) {
    return calendarPort.findById(entryId)
        .orElseThrow(() -> new IllegalArgumentException("Calendar entry " + entryId + " not found"));
  }

  @Transactional
  public void deleteEntry(String serverId, Long entryId) {
    RaidCalendarEntry entry = findEntry(entryId);
    if (!serverId.equals(entry.getServerId())) {
      throw new IllegalArgumentException("Calendar entry does not belong to this server");
    }
    if (entry.getEventId() != null) {
      throw new IllegalStateException("Calendar entries with a created event cannot be removed");
    }
    calendarPort.deleteById(entryId);
  }

  @Transactional
  public void validateEventCreation(
      String serverId,
      String creatorUserId,
      boolean creatorIsGuildManager,
      Long entryId,
      String guildName,
      String hosterUserId,
      String templateKey,
      Instant start,
      Instant end) {
    Server server = serverService.findServerById(serverId);
    requireCalendarConfigured(server);
    RaidCalendarEntry entry = findEntry(entryId);
    if (!serverId.equals(entry.getServerId())) {
      throw new IllegalArgumentException("Calendar entry does not belong to this server");
    }
    if (entry.getEventId() != null) {
      throw new IllegalStateException("This calendar entry already has an event");
    }
    validateTimes(start, end);
    ensureGuildHasNoRaidOnDay(serverId, guildName, start, entryId, null);
    if (!creatorIsGuildManager && !Objects.equals(entry.getHosterUserId(), creatorUserId)) {
      throw new IllegalArgumentException("Only the event owner or a guild manager can create this event");
    }
    if (!Objects.equals(entry.getGuildName(), guildName)
        || !Objects.equals(entry.getHosterUserId(), hosterUserId)
        || !Objects.equals(entry.getTemplateKey(), templateKey)
        || !sameCalendarDay(entry.getStart(), start)
        || !Objects.equals(
            java.time.Duration.between(entry.getStart(), entry.getEnd()),
            java.time.Duration.between(start, end))) {
      throw new IllegalArgumentException(
          "The event guild, day, host, template, and duration must match the selected calendar entry");
    }
  }

  @Transactional
  public void markEventCreated(Long entryId, Long eventId) {
    RaidCalendarEntry entry = findEntry(entryId);
    if (entry.getEventId() != null) {
      throw new IllegalStateException("This calendar entry already has an event");
    }
    entry.setEventId(eventId);
    calendarPort.save(entry);
  }

  @Transactional
  public void validateEventUpdate(String serverId, Long eventId, String guildName, Instant start) {
    if (start == null) {
      throw new IllegalArgumentException("Calendar event start is required");
    }
    ensureGuildHasNoRaidOnDay(serverId, guildName, start, null, eventId);
  }

  public String render(String serverId) {
    List<RaidCalendarEntry> entries = findUpcomingEntries(serverId);
    if (entries.isEmpty()) {
      return "No upcoming raids scheduled.";
    }

    StringBuilder result = new StringBuilder();
    for (RaidCalendarEntry entry : entries) {
      EventTemplate template = templatePort.findByServerIdAndKey(serverId, entry.getTemplateKey())
          .orElseThrow(() -> new IllegalArgumentException("Template " + entry.getTemplateKey() + " not found"));
      if (!result.isEmpty()) {
        result.append("\n\n");
      }
      result.append(DATE_FORMAT.format(entry.getStart())).append('\n')
          .append("Guild: ").append(entry.getGuildName()).append('\n')
          .append("Host: <@").append(entry.getHosterUserId()).append(">\n")
          .append("Event: ").append(formatEmoji(template.getEmojiSlug())).append(' ')
          .append(template.getName()).append(" (").append(entry.getDuration()).append(')');
    }
    return result.toString();
  }

  public Server requireCalendarConfigured(String serverId) {
    Server server = serverService.findServerById(serverId);
    requireCalendarConfigured(server);
    return server;
  }

  private void requireCalendarConfigured(Server server) {
    if (server.getRaidCalendarChannelId() == null || server.getRaidCalendarChannelId().isBlank()) {
      throw new IllegalStateException("Raid calendar channel has not been configured");
    }
  }

  private void validateTimes(Instant start, Instant end) {
    if (start == null || end == null || !end.isAfter(start)) {
      throw new IllegalArgumentException("Calendar event end must be after its start");
    }
  }

  private void ensureGuildHasNoRaidOnDay(
      String serverId,
      String guildName,
      Instant start,
      Long ignoredCalendarEntryId,
      Long ignoredEventId) {
    var raidDay = start.atZone(ZoneId.systemDefault()).toLocalDate();

    boolean calendarConflict = calendarPort.findByServerId(serverId).stream()
        .filter(entry -> !Objects.equals(entry.getId(), ignoredCalendarEntryId))
        .filter(entry -> !Objects.equals(entry.getEventId(), ignoredEventId))
        .anyMatch(entry -> sameGuildAndDay(
            serverId, guildName, raidDay, entry.getServerId(), entry.getGuildName(), entry.getStart()));
    if (calendarConflict) {
      throw new IllegalArgumentException(
          "Guild " + guildName + " already has a raid scheduled on " + raidDay
              + "; only one raid per guild per day is allowed");
    }

    boolean eventConflict = eventPort.findAllEvents().stream()
        .anyMatch(event -> event.getGuild() != null
            && sameGuildAndDay(
                serverId,
                guildName,
                raidDay,
                event.getGuild().getServerId(),
                event.getGuild().getName(),
                event.getStart())
            && !Objects.equals(event.getId(), ignoredEventId));
    if (eventConflict) {
      throw new IllegalArgumentException(
          "Guild " + guildName + " already has a raid event on " + raidDay
              + "; only one raid per guild per day is allowed");
    }
  }

  private boolean sameGuildAndDay(
      String serverId,
      String guildName,
      java.time.LocalDate raidDay,
      String existingServerId,
      String existingGuildName,
      Instant existingStart) {
    return Objects.equals(serverId, existingServerId)
        && Objects.equals(guildName, existingGuildName)
        && existingStart != null
        && Objects.equals(
            raidDay,
            existingStart.atZone(ZoneId.systemDefault()).toLocalDate());
  }

  private boolean sameCalendarDay(Instant left, Instant right) {
    return left != null
        && right != null
        && left.atZone(ZoneId.systemDefault()).toLocalDate()
            .equals(right.atZone(ZoneId.systemDefault()).toLocalDate());
  }

  private String formatEmoji(String emojiSlug) {
    if (emojiSlug == null || emojiSlug.isBlank()) {
      return "";
    }
    if (emojiSlug.startsWith(":") && emojiSlug.endsWith(":")) {
      return emojiSlug;
    }
    return emojiSlug.matches("[A-Za-z0-9_]+") ? ":" + emojiSlug + ":" : emojiSlug;
  }
}
