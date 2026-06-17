package de.qf0xb.raidsignupbot.signup.domain.service;

import java.util.List;

public class TeamGeneratorResult {
  private final List<Team> teams;
  private final List<PlayerSignup> unmatchedPlayers;

  public TeamGeneratorResult(List<Team> teams, List<PlayerSignup> unmatchedPlayers) {
    this.teams = List.copyOf(teams);
    this.unmatchedPlayers = List.copyOf(unmatchedPlayers);
  }

  public List<Team> getTeams() {
    return teams;
  }

  public List<PlayerSignup> getUnmatchedPlayers() {
    return unmatchedPlayers;
  }

  public List<PlayerSignup> getMatchedPlayers() {
    return teams.stream()
        .flatMap(team -> team.getSlotAssignments().stream())
        .map(SlotAssignment::playerSignup)
        .distinct()
        .toList();
  }

  public void applyPriorityUpdates() {
    getMatchedPlayers().forEach(playerSignup ->
        playerSignup.getPlayerProfile().recordMatchedSignup());
    unmatchedPlayers.forEach(playerSignup ->
        playerSignup.getPlayerProfile().recordUnmatchedSignup());
  }
}
