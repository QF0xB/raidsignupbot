package de.qf0xb.raidsignupbot.domain.model.template;

import lombok.AccessLevel;
import lombok.Getter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Getter
public class EventUtilityRole extends EventRole {
  @Getter(AccessLevel.NONE)
  private final Set<String> eligiblePrimaryRoleKeys = new HashSet<>();

  public EventUtilityRole(String key, String name, String emojiSlug, int requiredCount) {
    super(key, name, emojiSlug, requiredCount);
  }

  public void addEligiblePrimaryRole(EventRole role) {
    addEligiblePrimaryRole(role.getKey());
  }

  public void addEligiblePrimaryRole(String roleKey) {
    eligiblePrimaryRoleKeys.add(roleKey);
  }

  public boolean isEligibleFor(EventRole role) {
    return isEligibleFor(role.getKey());
  }

  public boolean isEligibleFor(String roleKey) {
    return eligiblePrimaryRoleKeys.isEmpty() || eligiblePrimaryRoleKeys.contains(roleKey);
  }

  public Set<String> getEligiblePrimaryRoleKeys() {
    return Collections.unmodifiableSet(eligiblePrimaryRoleKeys);
  }

  public void removeEligiblePrimaryRole(String roleKey) {
    eligiblePrimaryRoleKeys.remove(roleKey);
  }

  public void clearEligiblePrimaryRoles() {
    eligiblePrimaryRoleKeys.clear();
  }
}
