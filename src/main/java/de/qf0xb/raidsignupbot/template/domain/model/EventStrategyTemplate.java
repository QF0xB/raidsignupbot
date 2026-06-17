package de.qf0xb.raidsignupbot.template.domain.model;

import de.qf0xb.raidsignupbot.shared.domain.model.EventRole;
import de.qf0xb.raidsignupbot.shared.domain.model.EventUtilityRole;

import java.util.List;

public class EventStrategyTemplate {
  private final Long id;
  private String name;
  private final List<EventRole> roles;
  private final List<EventUtilityRole> utilityRoles;

  public EventStrategyTemplate(String name, List<EventRole> roles, List<EventUtilityRole> utilityRoles) {
    this.id = null;
    this.name = name;
    this.roles = roles;
    this.utilityRoles = utilityRoles;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public List<EventRole> getRoles() {
    return roles;
  }

  public void addRole(EventRole role) {
    roles.add(role);
  }

  public void removeRole(EventRole role) {
    roles.remove(role);
  }

  public List<EventUtilityRole> getUtilityRoles() {
    return utilityRoles;
  }

  public void addUtilityRole(EventUtilityRole role) {
    utilityRoles.add(role);
  }

  public void removeUtilityRole(EventUtilityRole role) {
    utilityRoles.remove(role);
  }
}
