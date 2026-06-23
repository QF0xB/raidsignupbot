package de.qf0xb.raidsignupbot.infrastructure.jpa.guild;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "guild")
public class GuildEntity {
  @Id
  @Column(name = "guild_id", nullable = false)
  private String guildId;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "bot_admin", nullable = false)
  private Boolean botAdmin = false;

  @Column(name = "raider_roles_enabled", nullable = false)
  private Boolean raiderRolesEnabled = true;

  @Column(name = "raider_role_id")
  private String raiderRoleId;

  @Column(name = "manage_raid_channels")
  private Boolean manageRaidChannels = true;

  @Column(name = "event_role_id")
  private String eventRoleId;

  @Column(name = "event_templates_enabled", nullable = false)
  private Boolean eventTemplatesEnabled = true;
}
