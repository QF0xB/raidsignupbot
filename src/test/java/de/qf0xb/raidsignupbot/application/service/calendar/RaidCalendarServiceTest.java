package de.qf0xb.raidsignupbot.application.service.calendar;

import de.qf0xb.raidsignupbot.application.port.guild.GuildApplicationPort;
import de.qf0xb.raidsignupbot.application.port.template.EventTemplateApplicationPort;
import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.application.service.template.TemplateService;
import de.qf0xb.raidsignupbot.domain.model.calendar.RaidCalendarEntry;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
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
  private static final String GUILD = "Calendar Guild";
  private static final String TEMPLATE = "calendar-template";

  @Inject
  RaidCalendarService calendarService;

  @Inject
  ServerService serverService;

  @Inject
  TemplateService templateService;

  @Inject
  GuildApplicationPort guildApplicationPort;

  @Inject
  EventTemplateApplicationPort templatePort;

  @BeforeEach
  void setUp() {
    serverService.updateCalendarSettings(SERVER_ID, "calendar-channel");
    if (guildApplicationPort.findByServerIdAndName(SERVER_ID, GUILD).isEmpty()) {
      Guild guild = new Guild(GUILD, SERVER_ID);
      guild.setServer(serverService.findServerById(SERVER_ID));
      guildApplicationPort.save(guild);
    }
    if (templatePort.findByServerIdAndKey(SERVER_ID, TEMPLATE).isEmpty()) {
      templateService.createTemplate(SERVER_ID, TEMPLATE, "Calendar Raid", "Details", "coin");
    }
  }

  @Test
  void listsScheduledEntryAfterCreation() {
    Instant start = daysFromToday(1);
    calendarService.createEntry(SERVER_ID, GUILD, "hoster-1", TEMPLATE, start, start.plus(Duration.ofHours(2)));

    assertTrue(calendarService.findUpcomingEntries(SERVER_ID).stream()
        .map(RaidCalendarEntry::getHosterUserId)
        .anyMatch("hoster-1"::equals));
  }

  @Test
  void rejectsEntriesInThePast() {
    Instant yesterday = daysFromToday(-1);

    IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> calendarService.createEntry(
        SERVER_ID, GUILD, "hoster-1", TEMPLATE, yesterday, yesterday.plus(Duration.ofHours(2))));

    assertTrue(error.getMessage().contains("in the past"), error.getMessage());
  }

  @Test
  void rejectsSecondEntryForSameGuildOnSameDay() {
    Instant start = daysFromToday(2);
    calendarService.createEntry(SERVER_ID, GUILD, "hoster-1", TEMPLATE, start, start.plus(Duration.ofHours(2)));

    IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> calendarService.createEntry(
        SERVER_ID, GUILD, "hoster-2", TEMPLATE, start, start.plus(Duration.ofHours(3))));

    assertTrue(error.getMessage().contains("already has a raid"), error.getMessage());
  }

  private Instant daysFromToday(int days) {
    return LocalDate.now(ZoneId.systemDefault()).plusDays(days).atStartOfDay(ZoneId.systemDefault()).toInstant();
  }
}
