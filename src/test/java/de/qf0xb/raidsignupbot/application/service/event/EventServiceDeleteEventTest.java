package de.qf0xb.raidsignupbot.application.service.event;

import de.qf0xb.raidsignupbot.application.port.EventApplicationPort;
import de.qf0xb.raidsignupbot.application.port.RaidCalendarApplicationPort;
import de.qf0xb.raidsignupbot.application.port.guild.GuildApplicationPort;
import de.qf0xb.raidsignupbot.application.service.calendar.RaidCalendarService;
import de.qf0xb.raidsignupbot.application.service.guild.GuildMemberService;
import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.application.service.template.TemplateService;
import de.qf0xb.raidsignupbot.domain.model.calendar.RaidCalendarEntry;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class EventServiceDeleteEventTest {
  private static final String SERVER_ID = "delete-server";
  private static final String GUILD = "Delete Guild";

  @Inject
  EventService eventService;

  @Inject
  RaidCalendarService calendarService;

  @Inject
  RaidCalendarApplicationPort calendarPort;

  @Inject
  EventApplicationPort eventPort;

  @Inject
  ServerService serverService;

  @Inject
  TemplateService templateService;

  @Inject
  GuildApplicationPort guildApplicationPort;

  @Inject
  GuildMemberService guildMemberService;

  @Test
  void deletesEventAndFreesItsCalendarEntry() {
    serverService.updateCalendarSettings(SERVER_ID, "calendar-channel");
    Guild guild = new Guild(GUILD, SERVER_ID);
    guild.setServer(serverService.findServerById(SERVER_ID));
    guild = guildApplicationPort.save(guild);
    var template = templateService.createTemplate(SERVER_ID, "delete-template", "Delete Raid", "Details", "coin");
    GuildMember hoster = guildMemberService.ensureGuildMember("delete-hoster", SERVER_ID, GUILD);

    Instant start = LocalDate.now(ZoneId.systemDefault()).plusDays(3).atStartOfDay(ZoneId.systemDefault()).toInstant();
    RaidCalendarEntry entry = calendarService.createEntry(
        SERVER_ID, GUILD, "delete-hoster", "delete-template", start, start.plus(Duration.ofHours(2)));
    Event saved = eventPort.save(Event.builder()
        .guild(guild)
        .hoster(hoster)
        .eventAdmin(hoster)
        .template(template)
        .start(start.plus(Duration.ofHours(19)))
        .end(start.plus(Duration.ofHours(21)))
        .build());
    calendarService.markEventCreated(entry.getId(), saved.getId());

    eventService.deleteEvent(saved.getId());

    assertTrue(eventPort.findById(saved.getId()).isEmpty());
    assertNull(calendarPort.findById(entry.getId()).orElseThrow().getEventId());
  }
}
