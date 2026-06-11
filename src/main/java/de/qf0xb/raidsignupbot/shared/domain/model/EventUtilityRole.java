package de.qf0xb.raidsignupbot.shared.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EventUtilityRole extends EventRole {
  private int reqCount;
  private final List<EventRole> eligableRoles = new ArrayList<>();

  public EventUtilityRole(String name, int reqCount) {
    super(name);
    this.reqCount = reqCount;
  }

  public List<EventRole> getEligableRoles() {
    return eligableRoles;
  }

  public void addEligableRole(EventRole role) {
    eligableRoles.add(role);
  }

  public void addEligableRoles(EventRole... roles) {
    eligableRoles.addAll(List.of(roles));
  }

  public void removeEligableRole(EventRole role) {
    eligableRoles.remove(role);
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
    return reqCount == that.reqCount && Objects.equals(eligableRoles, that.eligableRoles);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), reqCount, eligableRoles);
  }
}
