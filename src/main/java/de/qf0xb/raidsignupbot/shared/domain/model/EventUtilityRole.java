package de.qf0xb.raidsignupbot.shared.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EventUtilityRole extends EventRole {
  private int reqCount;
  private final List<EventRole> eligibleRoles = new ArrayList<>();

  public EventUtilityRole(String name, int reqCount) {
    super(name);
    this.reqCount = reqCount;
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

  public boolean sameUtilityAs(EventUtilityRole other) {
    return sameRoleAs(other);
  }

  public int getReqCount() {
    return reqCount;
  }

  public void setReqCount(int reqCount) {
    this.reqCount = reqCount;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    EventUtilityRole that = (EventUtilityRole) o;
    return reqCount == that.reqCount && Objects.equals(eligibleRoles, that.eligibleRoles);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), reqCount, eligibleRoles);
  }
}
