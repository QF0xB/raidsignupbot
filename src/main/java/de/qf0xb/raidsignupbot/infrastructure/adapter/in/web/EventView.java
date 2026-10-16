package de.qf0xb.raidsignupbot.infrastructure.adapter.in.web;

import java.util.List;

public record EventView(
    Long id,
    String title,
    String guildName,
    String start,
    String duration,
    String status,
    String eventAdminName,
    boolean signupOpen,
    boolean teamsGenerated,
    boolean approvalPending,
    List<TeamView> teams) {

  public record TeamView(int number, String strategy, List<SlotView> slots) {}

  /** {@code id} uses the same {@code team:slot} numbering as the Discord teams message. */
  public record SlotView(String id, String roleName, String userId, String memberName, List<String> utilities) {
    public boolean assigned() {
      return userId != null;
    }
  }

  /** CSS modifier for the status badge. */
  public String statusTone() {
    if (approvalPending) {
      return "pending";
    }
    if (teamsGenerated) {
      return "published";
    }
    return signupOpen ? "open" : "closed";
  }

  public List<SlotView> assignedSlots() {
    return teams.stream().flatMap(team -> team.slots().stream()).filter(SlotView::assigned).toList();
  }

  /** Each assigned player once, for actions that target a player rather than a box. */
  public List<SlotView> assignedPlayers() {
    return assignedSlots().stream()
        .collect(java.util.stream.Collectors.toMap(SlotView::userId, slot -> slot, (first, second) -> first,
            java.util.LinkedHashMap::new))
        .values().stream().toList();
  }
}
