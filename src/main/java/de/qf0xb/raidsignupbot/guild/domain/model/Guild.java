package de.qf0xb.raidsignupbot.guild.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class Guild {
  private final String guildId;
  private String guildName;

  private boolean createEvents = true;
  private boolean addRaiderRole = true;
  private String raiderRoleId;
}
