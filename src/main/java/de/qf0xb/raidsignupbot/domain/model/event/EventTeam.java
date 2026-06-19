package de.qf0xb.raidsignupbot.domain.model.event;

import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventUtilityRole;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@ToString
public class EventTeam {
  private final EventStrategy strategy;
  @Getter(AccessLevel.NONE)
  private final List<EventSlot> slots = new ArrayList<>();
  @Getter(AccessLevel.NONE)
  private final List<EventUtilityAssignment> utilityAssignments = new ArrayList<>();

  public EventTeam(EventStrategy strategy) {
    this.strategy = strategy;
    strategy.getPrimaryRoles().forEach(this::addRoleSlot);
    strategy.getUtilityRoles().forEach(this::addUtilityAssignment);
  }

  public void assign(String roleKey, GuildMember member) {
    EventSlot slot = slots.stream()
        .filter(candidate -> candidate.matches(roleKey))
        .filter(candidate -> !candidate.isAssigned())
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No free slot for role: " + roleKey));

    slot.assign(member);
  }

  public void assignUtility(String utilityRoleKey, GuildMember member) {
    EventSlot primarySlot = assignedPrimarySlotFor(member);
    EventUtilityAssignment utilityAssignment = utilityAssignments.stream()
        .filter(candidate -> candidate.matches(utilityRoleKey))
        .filter(candidate -> !candidate.isAssigned())
        .filter(candidate -> candidate.isEligibleFor(primarySlot))
        .filter(candidate -> !hasUtilityAssignedToMember(utilityRoleKey, member))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No free utility assignment for role: " + utilityRoleKey));

    utilityAssignment.assign(member);
  }

  public List<EventSlot> freeSlotsFor(String roleKey) {
    return slots.stream()
        .filter(slot -> slot.matches(roleKey))
        .filter(slot -> !slot.isAssigned())
        .toList();
  }

  public boolean isFilled() {
    return slots.stream().allMatch(EventSlot::isAssigned)
        && utilityAssignments.stream().allMatch(EventUtilityAssignment::isAssigned);
  }

  public void unassign(EventSlot slot) {
    GuildMember member = slot.getMember();
    slot.unassign();
    utilityAssignments.stream()
        .filter(assignment -> sameMember(assignment.getMember(), member))
        .forEach(EventUtilityAssignment::unassign);
  }

  public List<EventSlot> getSlots() {
    return Collections.unmodifiableList(slots);
  }

  public List<EventUtilityAssignment> getUtilityAssignments() {
    return Collections.unmodifiableList(utilityAssignments);
  }

  private void addRoleSlot(EventRole eventRole) {
    for (int i = 0; i < eventRole.getRequiredCount(); i++) {
      slots.add(new EventSlot(eventRole));
    }
  }

  private void addUtilityAssignment(EventUtilityRole eventUtilityRole) {
    for (int i = 0; i < eventUtilityRole.getRequiredCount(); i++) {
      utilityAssignments.add(new EventUtilityAssignment(eventUtilityRole));
    }
  }

  private EventSlot assignedPrimarySlotFor(GuildMember member) {
    return slots.stream()
        .filter(EventSlot::isAssigned)
        .filter(slot -> sameMember(slot.getMember(), member))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Utility member is not assigned to a primary slot"));
  }

  private boolean hasUtilityAssignedToMember(String utilityRoleKey, GuildMember member) {
    return utilityAssignments.stream()
        .filter(assignment -> assignment.matches(utilityRoleKey))
        .filter(EventUtilityAssignment::isAssigned)
        .anyMatch(assignment -> sameMember(assignment.getMember(), member));
  }

  private boolean sameMember(GuildMember left, GuildMember right) {
    return left != null
        && right != null
        && left.getUserId().equals(right.getUserId());
  }
}
