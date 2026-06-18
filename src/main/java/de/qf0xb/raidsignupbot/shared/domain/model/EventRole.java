package de.qf0xb.raidsignupbot.shared.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EventRole {
  private String name;
  private int count;
  private final List<EventRole> eligibleRoles = new ArrayList<>();

  public EventRole(String name) {
    this.name = name;
    this.count = 1;
  }

  public EventRole(String name, int count) {
    this.name = name;
    this.count = count;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long  id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean sameRoleAs(EventRole other) {
    if (other == null) {
      return false;
    }
    if (id != null && other.id != null) {
      return Objects.equals(id, other.id);
    }
    return Objects.equals(name, other.name);
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public List<EventRole> getEligibleRoles() {
    return eligibleRoles;
  }

  public void addEligibleRole(EventRole role) {
    eligibleRoles.add(role);
  }

  public void addEligibleRoles(EventRole... roles) {
    eligibleRoles.addAll(List.of(roles));
  }

  public void removeEligibleRole(EventRole role) {
    eligibleRoles.remove(role);
  }

  public boolean canBeProvidedFromRole(EventRole role) {
    return eligibleRoles.isEmpty()
        || eligibleRoles.stream().anyMatch(eligibleRole -> eligibleRole.sameRoleAs(role));
  }

  public boolean isUtilityRole() {
    return !eligibleRoles.isEmpty();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    EventRole eventRole = (EventRole) o;
    return Objects.equals(id, eventRole.id) && Objects.equals(name, eventRole.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name);
  }
}
