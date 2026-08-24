package de.qf0xb.raidsignupbot.infrastructure.jpa.calendar;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "raid_calendar_entry")
public class RaidCalendarEntryEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "server_id", nullable = false)
  private String serverId;

  @Column(name = "guild_name", nullable = false)
  private String guildName;

  @Column(name = "hoster_user_id", nullable = false)
  private String hosterUserId;

  @Column(name = "template_key", nullable = false)
  private String templateKey;

  @Column(name = "start_at", nullable = false)
  private Instant startAt;

  @Column(name = "end_at", nullable = false)
  private Instant endAt;

  @Column(name = "event_id")
  private Long eventId;
}
