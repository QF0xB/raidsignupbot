package de.qf0xb.raidsignupbot.domain.model.server;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Server {
  private String serverId;
  private String guildManagerRoleId;
  private String templateManagerRoleId;
  private String eventManagerRoleId;
  private boolean manageSignupChannels = true;
  private boolean manageVoiceChannels = true;
  private String raidVoiceChannelCategory;

  public Server(String serverId) {
    this.serverId = serverId;
  }
}
