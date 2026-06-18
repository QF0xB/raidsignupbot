package de.qf0xb.raidsignupbot.event.domain.model;

import de.qf0xb.raidsignupbot.guild.domain.model.GuildMember;
import lombok.Getter;

@Getter
public class EventRoleAssignment {
  private final EventRole role;
  private GuildMember member;

  public EventRoleAssignment(EventRole role) {
    this.role = role;
  }

  public EventRoleAssignment(EventRole role, GuildMember member) {
    this.role = role;
    this.member = member;
  }

  public boolean isAssigned() {
    return member != null;
  }

  public boolean isPrimaryRole() {
    return !role.isUtility();
  }

  public boolean isUtility() {
    return role.isUtility();
  }

  public void assign(GuildMember member) {
    this.member = member;
  }

  public void unassign() {
    this.member = null;
  }
}
