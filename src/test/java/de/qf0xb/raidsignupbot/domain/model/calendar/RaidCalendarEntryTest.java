package de.qf0xb.raidsignupbot.domain.model.calendar;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RaidCalendarEntryTest {
  @Test
  void formatsSupportedDurations() {
    Instant start = Instant.parse("2026-08-24T18:00:00Z");
    RaidCalendarEntry entry = RaidCalendarEntry.builder()
        .serverId("server")
        .guildName("OC")
        .hosterUserId("hoster")
        .templateKey("raid")
        .start(start)
        .end(start.plus(Duration.ofMinutes(90)))
        .build();

    assertEquals("1,5h", entry.getDuration());
  }
}
