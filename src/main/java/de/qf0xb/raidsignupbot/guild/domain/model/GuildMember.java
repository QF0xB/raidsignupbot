package de.qf0xb.raidsignupbot.guild.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GuildMember {
  private String playerId;
  private int priority = 0;

  public void increasePriority() {
    priority++;
  }

  public void assignedRole() {
    priority = 0;
  }

  public long getTotalDonations() {
    return 0; // TODO: implement
  }

  public long getTotalEventParticipations() {
    return 0; // TODO: implement
  }
}
