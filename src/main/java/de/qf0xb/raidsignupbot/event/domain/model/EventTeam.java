package de.qf0xb.raidsignupbot.event.domain.model;

import de.qf0xb.raidsignupbot.guild.domain.model.GuildMember;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Getter
@Setter
public class EventTeam {
  private final Event event;
  private EventStrategy strategy;

  private final List<EventRoleAssignment> assignments = new ArrayList<>();

  public EventTeam(Event event, EventStrategy strategy) {
    this.event = event;
    this.strategy = strategy;
    strategy.getPrimaryRoles().forEach(this::addAssignmentSlots);
    strategy.getUtilities().forEach(this::addAssignmentSlots);
  }

  public void assignMember(EventRole role, GuildMember member) {
    getUnassignedAssignment(role)
        .orElseThrow(() -> new IllegalStateException("No unassigned slot for role: " + role.getName()))
        .assign(member);
  }

  public void unassignMember(EventRole role) {
    getAssignment(role).ifPresent(EventRoleAssignment::unassign);
  }

  public Optional<EventRoleAssignment> getAssignment(EventRole role) {
    return assignments.stream().filter(a -> a.getRole().sameRoleAs(role)).findFirst();
  }

  public List<EventRoleAssignment> getAssignments(EventRole role) {
    return assignments.stream()
        .filter(a -> a.getRole().sameRoleAs(role))
        .toList();
  }

  private Optional<EventRoleAssignment> getUnassignedAssignment(EventRole role) {
    return assignments.stream()
        .filter(a -> a.getRole().sameRoleAs(role))
        .filter(a -> !a.isAssigned())
        .findFirst();
  }

  private void addAssignmentSlots(EventRole role) {
    if (role.getRequiredCount() < 0) {
      throw new IllegalArgumentException("Role required count cannot be negative: " + role.getName());
    }
    for (int slot = 0; slot < role.getRequiredCount(); slot++) {
      assignments.add(new EventRoleAssignment(role));
    }
  }
}
