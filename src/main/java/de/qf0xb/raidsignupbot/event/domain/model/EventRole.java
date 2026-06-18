package de.qf0xb.raidsignupbot.event.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EventRole {
  @EqualsAndHashCode.Include
  private String name;
  @EqualsAndHashCode.Include
  private int requiredCount;

  @EqualsAndHashCode.Include
  private boolean isUtility = false;

  private final Set<EventRole> eligibleRoles = new HashSet<>();

  public EventRole(String name, int requiredCount) {
    this.name = name;
    this.requiredCount = requiredCount;
  }

  public EventRole(String name, int requiredCount, boolean isUtility) {
    this.name = name;
    this.requiredCount = requiredCount;
    this.isUtility = isUtility;
  }

  public boolean isEligible(EventRole role) {
    return eligibleRoles.isEmpty()
        || eligibleRoles.stream().anyMatch(eligibleRole -> eligibleRole.sameRoleAs(role));
  }

  public void addEligibleRole(EventRole role) {
    eligibleRoles.add(role);
  }

  public void removeEligibleRole(EventRole role) {
    eligibleRoles.remove(role);
  }

  public boolean sameRoleAs(EventRole role) {
    return role != null
        && name.equals(role.name)
        && isUtility == role.isUtility;
  }
}
