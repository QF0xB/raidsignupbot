package de.qf0xb.raidsignupbot.signup.domain.model;

import de.qf0xb.raidsignupbot.shared.domain.model.EventRole;
import de.qf0xb.raidsignupbot.template.domain.model.EventStrategyTemplate;

import java.util.ArrayList;
import java.util.List;

public class EventPlayerSignup {
  private final String playerIdentity;
  private final List<EventRole> roles = new ArrayList<>();
  private final List<EventRole> utilityRoles = new ArrayList<>();

  public EventPlayerSignup(String playerIdentity) {
    this.playerIdentity = playerIdentity;
  }

  public String getPlayerIdentity() {
    return playerIdentity;
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

  public void addUtilityRole(EventRole role) {
    utilityRoles.add(role);
  }

  public void removeUtilityRole(EventRole role) {
    utilityRoles.remove(role);
  }

  public boolean hasRole(EventRole role) {
    return roles.contains(role);
  }

  public long roleCountForStrategy(EventStrategyTemplate strategy) {

    return strategy.getRoles().stream().filter(this::hasRole).count();
  }
}
