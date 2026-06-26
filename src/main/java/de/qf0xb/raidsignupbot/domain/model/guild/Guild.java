package de.qf0xb.raidsignupbot.domain.model.guild;

import de.qf0xb.raidsignupbot.domain.model.server.Server;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Guild {

  private String name;

  private Server server;

  private boolean botAdmin = false;

  private boolean raiderRolesEnabled = true;
  private String raiderRoleId;

  private String eventRoleId;

  private boolean eventTemplatesEnabled = true;

  public Guild(String name, String serverId) {
    this.name = name;
    this.server = new Server(serverId);
  }

  public String getServerId() {
    return server == null ? null : server.getServerId();
  }

  public void setServerId(String serverId) {
    if (server == null) {
      server = new Server(serverId);
    } else {
      server.setServerId(serverId);
    }
  }
}
