package de.qf0xb.raidsignupbot.signup.domain.service;

import de.qf0xb.raidsignupbot.shared.domain.model.EventRole;

import java.util.ArrayList;
import java.util.List;

public class StrategyTemplate {
  private final Long id;
  private final String name;
  private final List<EventRole> roles = new ArrayList<>();
  private final List<EventRole> utilityRoles = new ArrayList<>();

  public StrategyTemplate(Long id, String name) {
    this.id = id;
    this.name = name;
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

  public List<EventRole> getUtilityRoles() {
    return utilityRoles;
  }

  public void addUtilityRole(EventRole utilityRole) {
    utilityRoles.add(utilityRole);
  }

  public void removeUtilityRole(EventRole utilityRole) {
    utilityRoles.remove(utilityRole);
  }
}
