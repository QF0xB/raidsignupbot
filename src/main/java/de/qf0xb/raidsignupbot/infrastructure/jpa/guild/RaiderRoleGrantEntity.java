package de.qf0xb.raidsignupbot.infrastructure.jpa.guild;

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
@Table(name = "raider_role_grant")
public class RaiderRoleGrantEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "event_id", nullable = false)
  private Long eventId;

  @Column(name = "server_id", nullable = false)
  private String serverId;

  @Column(name = "user_id", nullable = false)
  private String userId;

  @Column(name = "role_id", nullable = false)
  private String roleId;

  @Column(name = "backup", nullable = false)
  private boolean backup;

  @Column(name = "release_after", nullable = false)
  private Instant releaseAfter;
}
