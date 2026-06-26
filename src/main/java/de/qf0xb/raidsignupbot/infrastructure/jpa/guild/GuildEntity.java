package de.qf0xb.raidsignupbot.infrastructure.jpa.guild;

import de.qf0xb.raidsignupbot.infrastructure.jpa.server.ServerEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "guild")
public class GuildEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "server_id", referencedColumnName = "server_id", nullable = false)
  private ServerEntity server;

  @Column(name = "name", nullable = false, unique = true)
  private String name;

  @Column(name = "raider_roles_enabled", nullable = false)
  private Boolean raiderRolesEnabled = true;

  @Column(name = "raider_role_id")
  private String raiderRoleId;

  @Column(name = "event_role_id")
  private String eventRoleId;
}
