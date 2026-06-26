package de.qf0xb.raidsignupbot.infrastructure.jpa.server;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "server")
public class ServerEntity {
  @Id
  @Column(name = "server_id", nullable = false)
  private String serverId;

  @Column(name = "guild_manager_role_id")
  private String guildManagerRoleId;

  @Column(name = "template_manager_role_id")
  private String templateManagerRoleId;

  @Column(name = "event_manager_role_id")
  private String eventManagerRoleId;

  @Column(name = "manage_event_channels")
  private Boolean manageSignupChannels = true;

  @Column(name = "manage_voice_channels")
  private Boolean manageVoiceChannels = false;

  @Column(name = "raid_voice_channel_category")
  private String raidVoiceChannelCategory;
}
