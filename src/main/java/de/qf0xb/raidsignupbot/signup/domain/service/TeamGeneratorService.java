package de.qf0xb.raidsignupbot.signup.domain.service;

import de.qf0xb.raidsignupbot.shared.domain.model.EventRole;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class TeamGeneratorService {
  public TeamGeneratorResult generateTeams(EventSignup signup) {
    List<PlayerSignup> remainingPlayers = new ArrayList<>(signup.getPlayerSignups());
    if (signup.getTemplate() == null || signup.getTemplate().getStrategies().isEmpty()) {
      return new TeamGeneratorResult(List.of(), remainingPlayers);
    }
    boolean hosterSignedUp = remainingPlayers.stream()
        .anyMatch(playerSignup -> playerSignup.getUserId().equals(signup.getHosterUserId()));

    SearchResult bestResult = searchBestResult(
        signup.getTemplate().getStrategies(),
        remainingPlayers,
        new HashMap<>(),
        signup.getHosterUserId(),
        hosterSignedUp
    );
    if (hosterSignedUp && !bestResult.hosterMatched()) {
      return new TeamGeneratorResult(List.of(), remainingPlayers);
    }
    return new TeamGeneratorResult(bestResult.teams(), bestResult.unmatchedPlayers());
  }

  private SearchResult searchBestResult(
      List<StrategyTemplate> strategies,
      List<PlayerSignup> remainingPlayers,
      Map<String, SearchResult> memo,
      String hosterUserId,
      boolean hosterSignedUp
  ) {
    String cacheKey = buildCacheKey(remainingPlayers);
    SearchResult cachedResult = memo.get(cacheKey);
    if (cachedResult != null) {
      return cachedResult;
    }

    SearchResult bestResult = new SearchResult(List.of(), List.copyOf(remainingPlayers), false, 0, 0, 0);

    for (StrategyTemplate strategy : strategies) {
      for (TeamCandidate candidate : buildTeamCandidates(strategy, remainingPlayers)) {
        List<PlayerSignup> nextRemainingPlayers = new ArrayList<>(remainingPlayers);
        nextRemainingPlayers.removeAll(candidate.usedPlayers());

        SearchResult tailResult = searchBestResult(
            strategies,
            nextRemainingPlayers,
            memo,
            hosterUserId,
            hosterSignedUp
        );
        List<Team> combinedTeams = new ArrayList<>(tailResult.teams().size() + 1);
        combinedTeams.add(candidate.team());
        combinedTeams.addAll(tailResult.teams());

        SearchResult combinedResult = new SearchResult(
            List.copyOf(combinedTeams),
            tailResult.unmatchedPlayers(),
            candidate.includesHoster(hosterUserId) || tailResult.hosterMatched(),
            candidate.assignedPlayers() + tailResult.assignedPlayers(),
            candidate.priorityScore() + tailResult.priorityScore(),
            candidate.flexibilityScore() + tailResult.flexibilityScore()
        );

        if (isBetter(combinedResult, bestResult, hosterSignedUp)) {
          bestResult = combinedResult;
        }
      }
    }

    memo.put(cacheKey, bestResult);
    return bestResult;
  }

  private boolean isBetter(SearchResult candidate, SearchResult currentBest, boolean hosterSignedUp) {
    if (hosterSignedUp && candidate.hosterMatched() != currentBest.hosterMatched()) {
      return candidate.hosterMatched();
    }
    if (candidate.teams().size() != currentBest.teams().size()) {
      return candidate.teams().size() > currentBest.teams().size();
    }
    if (candidate.assignedPlayers() != currentBest.assignedPlayers()) {
      return candidate.assignedPlayers() > currentBest.assignedPlayers();
    }
    if (candidate.priorityScore() != currentBest.priorityScore()) {
      return candidate.priorityScore() > currentBest.priorityScore();
    }
    return candidate.flexibilityScore() > currentBest.flexibilityScore();
  }

  private String buildCacheKey(List<PlayerSignup> remainingPlayers) {
    return remainingPlayers.stream()
        .map(PlayerSignup::getUserId)
        .sorted()
        .collect(Collectors.joining("|"));
  }

  private List<TeamCandidate> buildTeamCandidates(
      StrategyTemplate strategy,
      List<PlayerSignup> remainingPlayers
  ) {
    List<TeamCandidate> candidates = new ArrayList<>();
    assignSlots(
        strategy,
        remainingPlayers,
        0,
        new ArrayList<>(),
        new HashSet<>(),
        candidates
    );
    return candidates;
  }

  private void assignSlots(
      StrategyTemplate strategy,
      List<PlayerSignup> remainingPlayers,
      int slotIndex,
      List<SlotAssignment> assignments,
      Set<PlayerSignup> usedPlayers,
      List<TeamCandidate> candidates
  ) {
    if (slotIndex == strategy.getRoles().size()) {
      Optional<List<UtilityAssignment>> utilityAssignments = assignUtilities(
          strategy.getUtilityRoles(),
          assignments
      );
      utilityAssignments.ifPresent(assignmentsForUtilities ->
          candidates.add(new TeamCandidate(
              new Team(strategy, List.copyOf(assignments), assignmentsForUtilities),
              assignments.stream().map(SlotAssignment::playerSignup).toList()
          ))
      );
      return;
    }

    EventRole role = strategy.getRoles().get(slotIndex);
    List<PlayerSignup> eligiblePlayers = remainingPlayers.stream()
        .filter(player -> !usedPlayers.contains(player))
        .filter(player -> player.canFillRole(role))
        .sorted(Comparator
            .comparingInt((PlayerSignup player) -> player.selectedSlotCountFor(strategy))
            .thenComparing(PlayerSignup::getUserId))
        .toList();

    for (PlayerSignup player : eligiblePlayers) {
      assignments.add(new SlotAssignment(role, player));
      usedPlayers.add(player);
      assignSlots(strategy, remainingPlayers, slotIndex + 1, assignments, usedPlayers, candidates);
      usedPlayers.remove(player);
      assignments.removeLast();
    }
  }

  private Optional<List<UtilityAssignment>> assignUtilities(
      List<EventRole> requirements,
      List<SlotAssignment> slotAssignments
  ) {
    List<UtilityAssignment> utilityAssignments = new ArrayList<>();

    for (EventRole requirement : requirements) {
      int requiredCount = Math.max(0, requirement.getCount());
      List<SlotAssignment> eligibleAssignments = slotAssignments.stream()
          .filter(slotAssignment -> requirement.canBeProvidedFromRole(slotAssignment.role()))
          .filter(slotAssignment -> slotAssignment.playerSignup().canProvideUtility(requirement))
          .filter(slotAssignment -> !alreadyAssignedForUtility(
              utilityAssignments,
              requirement,
              slotAssignment.playerSignup()
          ))
          .sorted(Comparator
              .comparingInt((SlotAssignment assignment) ->
                  assignment.playerSignup().selectedUtilityCount())
              .thenComparing(assignment -> assignment.playerSignup().getUserId()))
          .limit(requiredCount)
          .toList();

      if (eligibleAssignments.size() < requiredCount) {
        return Optional.empty();
      }

      eligibleAssignments.forEach(slotAssignment -> utilityAssignments.add(new UtilityAssignment(
          requirement,
          slotAssignment.playerSignup(),
          slotAssignment.role()
      )));
    }

    return Optional.of(List.copyOf(utilityAssignments));
  }

  private boolean alreadyAssignedForUtility(
      List<UtilityAssignment> utilityAssignments,
      EventRole requirement,
      PlayerSignup playerSignup
  ) {
    return utilityAssignments.stream().anyMatch(assignment ->
        assignment.utility().sameRoleAs(requirement)
            && assignment.playerSignup() == playerSignup
    );
  }

  private record SearchResult(
      List<Team> teams,
      List<PlayerSignup> unmatchedPlayers,
      boolean hosterMatched,
      int assignedPlayers,
      int priorityScore,
      int flexibilityScore
  ) {
  }

  private record TeamCandidate(Team team, List<PlayerSignup> usedPlayers) {
    private int assignedPlayers() {
      return usedPlayers.size();
    }

    private boolean includesHoster(String hosterUserId) {
      return hosterUserId != null
          && usedPlayers.stream().anyMatch(player -> player.getUserId().equals(hosterUserId));
    }

    private int priorityScore() {
      return usedPlayers.stream()
          .mapToInt(PlayerSignup::getPriorityScore)
          .sum();
    }

    private int flexibilityScore() {
      return usedPlayers.stream()
          .mapToInt(PlayerSignup::selectedRoleCount)
          .sum();
    }
  }
}
