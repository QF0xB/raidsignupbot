package de.qf0xb.raidsignupbot.signup.domain.service;

import de.qf0xb.raidsignupbot.shared.domain.model.EventRole;
import de.qf0xb.raidsignupbot.shared.domain.model.PlayerProfile;

import java.util.ArrayList;
import java.util.List;

public class PlayerSignup {
  private final PlayerProfile playerProfile;
  private final List<EventRole> selectedRoles = new ArrayList<>();
  private final List<EventRole> selectedUtilities = new ArrayList<>();

  public PlayerSignup(String userId) {
    this(new PlayerProfile(userId));
  }

  public PlayerSignup(PlayerProfile playerProfile) {
    this.playerProfile = playerProfile;
  }

  public PlayerSignup(
      String userId,
      List<EventRole> selectedRoles,
      List<EventRole> selectedUtilities
  ) {
    this(new PlayerProfile(userId), selectedRoles, selectedUtilities);
  }

  public PlayerSignup(
      PlayerProfile playerProfile,
      List<EventRole> selectedRoles,
      List<EventRole> selectedUtilities
  ) {
    this.playerProfile = playerProfile;
    this.selectedRoles.addAll(selectedRoles);
    this.selectedUtilities.addAll(selectedUtilities);
  }

  public PlayerSignup(
      String userId,
      List<EventRole> selectedRoles,
      List<EventRole> selectedUtilities,
      int priorityScore
  ) {
    this(userId, selectedRoles, selectedUtilities);
    playerProfile.setPriorityScore(priorityScore);
  }

  public String getUserId() {
    return playerProfile.getUserId();
  }

  public PlayerProfile getPlayerProfile() {
    return playerProfile;
  }

  public List<EventRole> getSelectedRoles() {
    return selectedRoles;
  }

  public void addSelectedRole(EventRole role) {
    selectedRoles.add(role);
  }

  public List<EventRole> getSelectedUtilities() {
    return selectedUtilities;
  }

  public void addSelectedUtility(EventRole utility) {
    selectedUtilities.add(utility);
  }

  public boolean canFillRole(EventRole role) {
    return selectedRoles.stream().anyMatch(selectedRole -> selectedRole.sameRoleAs(role));
  }

  public boolean canProvideUtility(EventRole utility) {
    return selectedUtilities.stream().anyMatch(selectedUtility -> selectedUtility.sameRoleAs(utility));
  }

  public int selectedSlotCountFor(StrategyTemplate strategy) {
    return (int) strategy.getRoles().stream()
        .filter(this::canFillRole)
        .count();
  }

  public int selectedUtilityCount() {
    return selectedUtilities.size();
  }

  public int selectedRoleCount() {
    return selectedRoles.size();
  }

  public int getPriorityScore() {
    return playerProfile.getPriorityScore();
  }

  public void setPriorityScore(int priorityScore) {
    playerProfile.setPriorityScore(priorityScore);
  }
}
