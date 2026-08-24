package de.qf0xb.raidsignupbot.domain.model.calendar;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;

@Getter
@Builder
public class RaidCalendarEntry {
  private final Long id;
  private final String serverId;
  private final String guildName;
  private final String hosterUserId;
  private final String templateKey;
  private final Instant start;
  private final Instant end;

  @Setter
  private Long eventId;

  public String getDuration() {
    Duration duration = Duration.between(start, end).abs();
    return switch ((int) duration.toMinutes()) {
      case 60 -> "1h";
      case 90 -> "1,5h";
      case 120 -> "2h";
      case 180 -> "3h";
      default -> duration.toMinutes() + "m";
    };
  }
}
