package de.qf0xb.raidsignupbot.signup.domain.service;

import java.util.List;

public class Team {
  private final StrategyTemplate strategy;
  private final List<SlotAssignment> slotAssignments;
  private final List<UtilityAssignment> utilityAssignments;

  public Team(StrategyTemplate strategy, List<SlotAssignment> slotAssignments) {
    this(strategy, slotAssignments, List.of());
  }

  public Team(
      StrategyTemplate strategy,
      List<SlotAssignment> slotAssignments,
      List<UtilityAssignment> utilityAssignments
  ) {
    this.strategy = strategy;
    this.slotAssignments = List.copyOf(slotAssignments);
    this.utilityAssignments = List.copyOf(utilityAssignments);
  }

  public StrategyTemplate getStrategy() {
    return strategy;
  }

  public List<SlotAssignment> getSlotAssignments() {
    return slotAssignments;
  }

  public List<UtilityAssignment> getUtilityAssignments() {
    return utilityAssignments;
  }
}
