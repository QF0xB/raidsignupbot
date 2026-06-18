package de.qf0xb.raidsignupbot.template.domain.model;

import de.qf0xb.raidsignupbot.shared.domain.model.EventRole;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Locale;

@Getter
@Setter
public class EventStrategyTemplate {
  private String stub;
  private String name;

  private final List<EventRole> roles;
  private final List<EventRole> utilityRoles;

  public EventStrategyTemplate(String stub, String name, List<EventRole> roles, List<EventRole> utilityRoles) {
    this.stub = stub;
    this.name = name;
    this.roles = roles;
    this.utilityRoles = utilityRoles;
  }

  public EventStrategyTemplate(String name, List<EventRole> roles, List<EventRole> utilityRoles) {
    this(name.toLowerCase(Locale.ROOT).strip(), name, roles, utilityRoles);
  }

  public void addRole(EventRole role) {
    roles.add(role);
  }

  public void removeRole(EventRole role) {
    roles.remove(role);
  }

  public void addUtilityRole(EventRole role) {
    utilityRoles.add(role);
  }

  public void removeUtilityRole(EventRole role) {
    utilityRoles.remove(role);
  }
}
