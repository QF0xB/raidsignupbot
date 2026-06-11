package de.qf0xb.raidsignupbot.signup.domain.model;

import de.qf0xb.raidsignupbot.template.domain.model.EventStrategyTemplate;

import java.util.ArrayList;
import java.util.List;

public class EventTeam {
  private EventStrategyTemplate strategy;
  private final List<EventAssignedRole> assignedRoles = new ArrayList<>();

  public EventTeam(EventStrategyTemplate strategy) {
    this.strategy = strategy;
  }

  public List<EventAssignedRole> getAssignedRoles() {
    return assignedRoles;
  }

  public void addAssignedRole(EventAssignedRole role) {
    assignedRoles.add(role);
  }

  public void removeAssignedRole(EventAssignedRole role) {
    assignedRoles.remove(role);
  }

  public EventStrategyTemplate getStrategy() {
    return strategy;
  }

  public void setStrategy(EventStrategyTemplate strategy) {
    this.strategy = strategy;
  }
}
