package de.qf0xb.raidsignupbot.signup.domain.model;

import de.qf0xb.raidsignupbot.shared.domain.model.EventRole;
import de.qf0xb.raidsignupbot.shared.domain.model.EventUtilityRole;

public class EventAssignedRole {
  private EventPlayerSignup player;
  private EventRole role;
  private EventUtilityRole utilityRole;

  public EventAssignedRole(EventPlayerSignup player, EventRole role) {
    this.player = player;
    this.role = role;
  }

  public EventAssignedRole(EventPlayerSignup player, EventRole role, EventUtilityRole utilityRole) {
    this.player = player;
    this.role = role;
    this.utilityRole = utilityRole;
  }

  public EventPlayerSignup getPlayer() {
    return player;
  }

  public void setPlayer(EventPlayerSignup player) {
    this.player = player;
  }

  public EventRole getRole() {
    return role;
  }

  public void setRole(EventRole role) {
    this.role = role;
  }

  public EventUtilityRole getUtilityRole() {
    return utilityRole;
  }

  public void setUtilityRole(EventUtilityRole utilityRole) {
    this.utilityRole = utilityRole;
  }
}
