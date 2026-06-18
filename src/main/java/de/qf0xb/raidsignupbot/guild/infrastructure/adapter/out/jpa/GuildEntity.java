package de.qf0xb.raidsignupbot.guild.infrastructure.adapter.out.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

  @Column(name = "guild_name", nullable = false)
  private String guildName;

  @Column(name = "create_events", nullable = false)
  private boolean createEvents;

  @Column(name = "add_raider_role", nullable = false)
  private boolean addRaiderRole;

  @Column(name = "raider_role_id")
  private String raiderRoleId;
}
