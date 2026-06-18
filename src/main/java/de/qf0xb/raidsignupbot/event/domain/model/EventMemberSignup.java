package de.qf0xb.raidsignupbot.event.domain.model;

import de.qf0xb.raidsignupbot.guild.domain.model.GuildMember;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class EventMemberSignup {
  @Getter
  private final GuildMember guildMember;
  private final List<EventRole> roles = new ArrayList<>();

  public void addRole(EventRole role) {
    roles.add(role);
  }

  public void removeRole(EventRole role) {
    roles.remove(role);
  }

  public List<EventRole> getPrimaryRoles() {
    return roles.stream().filter(r -> !r.isUtility()).toList();
  }

  public List<EventRole> getUtilities() {
    return roles.stream().filter(EventRole::isUtility).toList();
  }
}
