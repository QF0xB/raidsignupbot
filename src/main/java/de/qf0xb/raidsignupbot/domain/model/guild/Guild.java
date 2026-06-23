package de.qf0xb.raidsignupbot.domain.model.guild;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Guild {
  private final String guildId;

  private String name;

  private boolean botAdmin = false;

  private boolean raiderRolesEnabled = true;
  private String raiderRoleId;

  private boolean eventTemplatesEnabled = true;

  public Guild(String guildId, String name, String raiderRoleId) {
    this.guildId = guildId;
    this.name = name;
    this.raiderRoleId = raiderRoleId;
  }
}
