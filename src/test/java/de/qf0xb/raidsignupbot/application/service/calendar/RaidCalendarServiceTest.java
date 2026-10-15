package de.qf0xb.raidsignupbot.application.service.calendar;

import de.qf0xb.raidsignupbot.application.port.guild.GuildApplicationPort;
import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.application.service.template.TemplateService;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class RaidCalendarServiceTest {
  private static final String SERVER_ID = "calendar-server";

  @Inject
  RaidCalendarService calendarService;

  @Inject
  ServerService serverService;

  @Inject
  TemplateService templateService;

  @Inject
  GuildApplicationPort guildApplicationPort;

  @Test
  void rendersScheduledEntryAfterCreation() {
    serverService.updateCalendarSettings(SERVER_ID, "calendar-channel");
    Guild guild = new Guild("Calendar Guild", SERVER_ID);
    guild.setServer(serverService.findServerById(SERVER_ID));
    guildApplicationPort.save(guild);
    templateService.createTemplate(SERVER_ID, "calendar-template", "Calendar Raid", "Details", "coin");

    Instant start = LocalDate.now(ZoneId.systemDefault()).plusDays(1)
        .atStartOfDay(ZoneId.systemDefault()).toInstant();
    calendarService.createEntry(
        SERVER_ID, "Calendar Guild", "hoster-1", "calendar-template", start, start.plus(Duration.ofHours(2)));

    String rendered = calendarService.render(SERVER_ID);

    assertTrue(rendered.contains("Calendar Raid"), rendered);
    assertTrue(rendered.contains("<@hoster-1>"), rendered);
  }

  @Test
  void rejectsEntriesInThePast() {
    serverService.updateCalendarSettings(SERVER_ID, "calendar-channel");
    Instant yesterday = LocalDate.now(ZoneId.systemDefault()).minusDays(1)
        .atStartOfDay(ZoneId.systemDefault()).toInstant();

    IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> calendarService.createEntry(
        SERVER_ID, "Calendar Guild", "hoster-1", "calendar-template", yesterday, yesterday.plus(Duration.ofHours(2))));

    assertTrue(error.getMessage().contains("in the past"), error.getMessage());
  }
}
