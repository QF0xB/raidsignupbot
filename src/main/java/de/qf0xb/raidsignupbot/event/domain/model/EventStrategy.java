package de.qf0xb.raidsignupbot.event.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class EventStrategy {
  private String slug;
  private String name;
  private final List<String> guides = new ArrayList<>();

  private final List<EventRole> primaryRoles = new ArrayList<>();
  private final List<EventRole> utilities = new ArrayList<>();

  public void addGuide(String guide) {
    guides.add(guide);
  }

  public void removeGuide(String guide) {
    guides.remove(guide);
  }

  public void addRole(EventRole role) {
    if (role.isUtility()) {
      utilities.add(role);
    } else {
      primaryRoles.add(role);
    }
  }

  public boolean hasRole(EventRole role) {
    return primaryRoles.stream().anyMatch(primaryRole -> primaryRole.sameRoleAs(role))
        || utilities.stream().anyMatch(utility -> utility.sameRoleAs(role));
  }

  public boolean hasRole(String roleName) {
    return primaryRoles.stream().anyMatch(r -> r.getName().equals(roleName)) || utilities.stream().anyMatch(r -> r.getName().equals(roleName));
  }
}
