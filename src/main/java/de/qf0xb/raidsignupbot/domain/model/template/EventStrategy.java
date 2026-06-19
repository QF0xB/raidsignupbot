package de.qf0xb.raidsignupbot.domain.model.template;

import lombok.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Getter
@Setter
@EqualsAndHashCode(of = "key")
@RequiredArgsConstructor
@ToString
public class EventStrategy {
  private final String key;
  private String name;

  @Getter(AccessLevel.NONE)
  private final List<String> guides = new ArrayList<>();

  @Getter(AccessLevel.NONE)
  private final Set<EventRole> primaryRoles = new LinkedHashSet<>();
  @Getter(AccessLevel.NONE)
  private final Set<EventUtilityRole> utilityRoles = new LinkedHashSet<>();

  public EventStrategy(String key, String name) {
    this.key = key;
    this.name = name;
  }

  public void addGuide(String guide) {
    guides.add(guide);
  }

  public void removeGuide(String guide) {
    guides.remove(guide);
  }

  public void addRole(EventRole role) {
    if (role instanceof EventUtilityRole utilityRole) {
      utilityRoles.add(utilityRole);
    } else {
      primaryRoles.add(role);
    }
  }

  public void removeRole(String roleKey) {
    primaryRoles.removeIf(r -> r.getKey().equals(roleKey));
    utilityRoles.removeIf(r -> r.getKey().equals(roleKey));
  }

  public Optional<EventRole> findPrimaryRole(String roleKey) {
    return primaryRoles.stream().filter(role -> role.getKey().equals(roleKey)).findFirst();
  }

  public Optional<EventUtilityRole> findUtilityRole(String roleKey) {
    return utilityRoles.stream()
        .filter(role -> role.getKey().equals(roleKey))
        .findFirst();
  }

  public Optional<EventRole> findRole(String roleKey) {
    return findPrimaryRole(roleKey).or(() -> findUtilityRole(roleKey).map(role -> (EventRole) role));
  }

  public List<String> getGuides() {
    return Collections.unmodifiableList(guides);
  }

  public Set<EventRole> getPrimaryRoles() {
    return Collections.unmodifiableSet(primaryRoles);
  }

  public Set<EventUtilityRole> getUtilityRoles() {
    return Collections.unmodifiableSet(utilityRoles);
  }
}
