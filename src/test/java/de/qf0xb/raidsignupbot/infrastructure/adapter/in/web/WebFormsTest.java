package de.qf0xb.raidsignupbot.infrastructure.adapter.in.web;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WebFormsTest {
  @Test
  void parsesTheDiscordDurations() {
    assertEquals(Duration.ofMinutes(90), WebForms.duration("1,5h"));
    assertThrows(IllegalArgumentException.class, () -> WebForms.duration("4h"));
  }

  @Test
  void readsDateTimesInTheBotsTimeZone() {
    var start = WebForms.dateTime("2026-10-20T20:15");

    assertEquals(LocalDateTime.of(2026, 10, 20, 20, 15).atZone(ZoneId.systemDefault()).toInstant(), start);
    assertEquals("2026-10-20T20:15", WebForms.dateTimeInput(start));
    assertThrows(IllegalArgumentException.class, () -> WebForms.dateTime("tomorrow"));
  }

  @Test
  void validatesNumbersAndSplitsGuides() {
    assertEquals(10, WebForms.nonNegative(" 10 ", "donation"));
    assertThrows(IllegalArgumentException.class, () -> WebForms.nonNegative("-1", "donation"));
    assertThrows(IllegalArgumentException.class, () -> WebForms.nonNegative("ten", "donation"));
    assertEquals(List.of("first", "second"), WebForms.lines("first\r\n\n  second  \n"));
  }
}
