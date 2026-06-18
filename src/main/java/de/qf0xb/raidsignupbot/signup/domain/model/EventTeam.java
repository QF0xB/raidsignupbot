package de.qf0xb.raidsignupbot.signup.domain.model;

import de.qf0xb.raidsignupbot.template.domain.model.EventStrategyTemplate;

import java.util.ArrayList;
import java.util.List;

public class EventTeam {
  private Long id;
  private Long eventId;
  private EventStrategyTemplate strategy;
  private final List<EventAssignedRole> assignedRoles = new ArrayList<>();

  public EventTeam(EventStrategyTemplate strategy) {
    this.strategy = strategy;
  }

  public EventTeam(Long eventId, EventStrategyTemplate strategy) {
    this.eventId = eventId;
    this.strategy = strategy;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getEventId() {
    return eventId;
  }

  public void setEventId(Long eventId) {
    this.eventId = eventId;
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
