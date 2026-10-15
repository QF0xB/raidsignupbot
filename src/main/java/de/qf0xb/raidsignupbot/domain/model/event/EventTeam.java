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
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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

  public EventSlot findAssignedPrimarySlotFor(GuildMember member) {
    return slots.stream()
        .filter(EventSlot::isAssigned)
        .filter(slot -> sameMember(slot.getMember(), member))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Member is not assigned to a primary slot"));
  }

  public void assignUtility(String utilityRoleKey, GuildMember member) {
    EventUtilityAssignment utilityAssignment = utilityAssignments.stream()
        .filter(candidate -> candidate.matches(utilityRoleKey))
        .filter(candidate -> !candidate.isAssigned())
        .filter(candidate -> slots.stream()
            .filter(EventSlot::isAssigned)
            .filter(slot -> sameMember(slot.getMember(), member))
            .anyMatch(candidate::isEligibleFor))
        .filter(candidate -> !hasUtilityAssignedToMember(utilityRoleKey, member))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No free utility assignment for role: " + utilityRoleKey));

    utilityAssignment.assign(member);
  }

  /** Utilities belong to the first eligible primary slot occupied by their player. */
  public List<EventUtilityAssignment> utilitiesForSlot(EventSlot slot) {
    if (!slots.contains(slot) || !slot.isAssigned()) {
      throw new IllegalArgumentException("Select an assigned primary slot in this team.");
    }
    return utilityAssignments.stream().filter(EventUtilityAssignment::isAssigned)
        .filter(utility -> sameMember(utility.getMember(), slot.getMember()))
        .filter(utility -> slots.stream().filter(EventSlot::isAssigned)
            .filter(primary -> sameMember(primary.getMember(), utility.getMember()))
            .filter(utility::isEligibleFor).findFirst().orElse(null) == slot)
        .toList();
  }

  public void swapSlots(EventSlot slot, EventTeam otherTeam, EventSlot otherSlot) {
    List<EventUtilityAssignment> ownUtilities = utilitiesForSlot(slot);
    List<EventUtilityAssignment> otherUtilities = otherTeam.utilitiesForSlot(otherSlot);
    GuildMember ownMember = slot.getMember();
    GuildMember otherMember = otherSlot.getMember();
    Map<EventUtilityAssignment, GuildMember> changes = new HashMap<>();
    ownUtilities.forEach(utility -> changes.put(utility, otherMember));
    otherUtilities.forEach(utility -> changes.put(utility, ownMember));
    validateUtilityChanges(changes);
    if (otherTeam != this) {
      otherTeam.validateUtilityChanges(changes);
    }
    slot.assign(otherMember);
    otherSlot.assign(ownMember);
    changes.forEach(EventUtilityAssignment::assign);
  }

  public void replaceSlot(EventSlot slot, GuildMember replacement) {
    if (replacement == null) {
      throw new IllegalArgumentException("Replacement member is required.");
    }
    Map<EventUtilityAssignment, GuildMember> changes = new HashMap<>();
    utilitiesForSlot(slot).forEach(utility -> changes.put(utility, replacement));
    validateUtilityChanges(changes);
    slot.assign(replacement);
    changes.forEach(EventUtilityAssignment::assign);
  }

  private void validateUtilityChanges(Map<EventUtilityAssignment, GuildMember> changes) {
    Set<String> assignments = new HashSet<>();
    for (EventUtilityAssignment utility : utilityAssignments) {
      if (utility.isAssigned()) {
        GuildMember member = changes.getOrDefault(utility, utility.getMember());
        if (!assignments.add(utility.getKey() + ":" + member.getUserId())) {
          throw new IllegalArgumentException("This swap would assign the same utility role twice to one player in a team.");
        }
      }
    }
  }

  public int swapMembers(GuildMember left, GuildMember right) {
    if (left == null || right == null) {
      throw new IllegalArgumentException("Members to swap must not be null");
    }
    if (sameMember(left, right)) {
      return 0;
    }

    int swappedAssignments = 0;

    for (EventSlot slot : slots) {
      if (sameMember(slot.getMember(), left)) {
        slot.assign(right);
        swappedAssignments++;
      } else if (sameMember(slot.getMember(), right)) {
        slot.assign(left);
        swappedAssignments++;
      }
    }

    for (EventUtilityAssignment assignment : utilityAssignments) {
      if (sameMember(assignment.getMember(), left)) {
        assignment.assign(right);
        swappedAssignments++;
      } else if (sameMember(assignment.getMember(), right)) {
        assignment.assign(left);
        swappedAssignments++;
      }
    }

    return swappedAssignments;
  }

  public int replaceMember(GuildMember existing, GuildMember replacement) {
    if (existing == null || replacement == null) {
      throw new IllegalArgumentException("Members to replace must not be null");
    }
    if (sameMember(existing, replacement)) {
      return 0;
    }

    int replacedAssignments = 0;

    for (EventSlot slot : slots) {
      if (sameMember(slot.getMember(), existing)) {
        slot.assign(replacement);
        replacedAssignments++;
      }
    }

    for (EventUtilityAssignment assignment : utilityAssignments) {
      if (sameMember(assignment.getMember(), existing)) {
        assignment.assign(replacement);
        replacedAssignments++;
      }
    }

    return replacedAssignments;
  }

  public boolean containsMember(GuildMember member) {
    return slots.stream().anyMatch(slot -> sameMember(slot.getMember(), member))
        || utilityAssignments.stream().anyMatch(assignment -> sameMember(assignment.getMember(), member));
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
