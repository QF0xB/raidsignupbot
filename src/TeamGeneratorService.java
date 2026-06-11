package de.qf0xb.raidsignupbot.signup.domain.service;

import de.qf0xb.raidsignupbot.shared.domain.model.StrategySlot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class TeamGeneratorService {
  public TeamGeneratorResult generateTeams(EventSignup signup) {
    List<PlayerSignup> remainingPlayers = new ArrayList<>(signup.getPlayerSignups());
    if (signup.getTemplate() == null || signup.getTemplate().getStrategies().isEmpty()) {
      return new TeamGeneratorResult(List.of(), remainingPlayers);
    }

    SearchResult bestResult = searchBestResult(
        signup.getTemplate().getStrategies(),
        remainingPlayers,
        new HashMap<>()
    );
    return new TeamGeneratorResult(bestResult.teams(), bestResult.unmatchedPlayers());
  }

  private SearchResult searchBestResult(
      List<StrategyTemplate> strategies,
      List<PlayerSignup> remainingPlayers,
      Map<String, SearchResult> memo
  ) {
    String cacheKey = buildCacheKey(remainingPlayers);
    SearchResult cachedResult = memo.get(cacheKey);
    if (cachedResult != null) {
      return cachedResult;
    }

    SearchResult bestResult = new SearchResult(List.of(), List.copyOf(remainingPlayers), 0);
    boolean foundCompleteCandidate = false;

    for (StrategyTemplate strategy : strategies) {
      TeamCandidate candidate = buildTeamCandidate(strategy, remainingPlayers);
      if (!candidate.complete()) {
        continue;
      }

      foundCompleteCandidate = true;

      List<PlayerSignup> nextRemainingPlayers = new ArrayList<>(remainingPlayers);
      nextRemainingPlayers.removeAll(candidate.usedPlayers());

      SearchResult tailResult = searchBestResult(strategies, nextRemainingPlayers, memo);
      List<Team> combinedTeams = new ArrayList<>(tailResult.teams().size() + 1);
      combinedTeams.add(candidate.team());
      combinedTeams.addAll(tailResult.teams());

      SearchResult combinedResult = new SearchResult(
          List.copyOf(combinedTeams),
          tailResult.unmatchedPlayers(),
          candidate.assignedPlayers() + tailResult.assignedPlayers()
      );

      if (isBetter(combinedResult, bestResult)) {
        bestResult = combinedResult;
      }
    }

    if (!foundCompleteCandidate) {
      memo.put(cacheKey, bestResult);
      return bestResult;
    }

    memo.put(cacheKey, bestResult);
    return bestResult;
  }

  private boolean isBetter(SearchResult candidate, SearchResult currentBest) {
    if (candidate.teams().size() != currentBest.teams().size()) {
      return candidate.teams().size() > currentBest.teams().size();
    }
    return candidate.assignedPlayers() > currentBest.assignedPlayers();
  }

  private String buildCacheKey(List<PlayerSignup> remainingPlayers) {
    return remainingPlayers.stream()
        .map(PlayerSignup::getDiscordUserId)
        .sorted()
        .collect(Collectors.joining("|"));
  }

  private TeamCandidate buildTeamCandidate(StrategyTemplate strategy, List<PlayerSignup> remainingPlayers) {
    List<StrategySlot> openSlots = new ArrayList<>(strategy.getSlots());
    List<PlayerSignup> candidatePlayers = remainingPlayers.stream()
        .filter(player -> player.selectedSlotCountFor(strategy) > 0)
        .sorted(Comparator.comparingInt(player -> player.selectedSlotCountFor(strategy)))
        .toList();
    List<SlotAssignment> assignments = new ArrayList<>();
    List<PlayerSignup> usedPlayers = new ArrayList<>();

    for (PlayerSignup player : candidatePlayers) {
      if (openSlots.isEmpty()) {
        break;
      }

      List<PlayerSignup> availablePlayers = candidatePlayers.stream()
          .filter(candidatePlayer -> !usedPlayers.contains(candidatePlayer))
          .toList();
      StrategySlot slot = chooseBestSlotForPlayer(player, openSlots, availablePlayers);
      if (slot == null) {
        continue;
      }

      assignments.add(new SlotAssignment(slot, player));
      usedPlayers.add(player);
      openSlots.remove(slot);
    }

    if (!openSlots.isEmpty()) {
      return new TeamCandidate(new Team(strategy, assignments), usedPlayers, false);
    }

    Optional<List<UtilityAssignment>> utilityAssignments = assignUtilities(
        strategy.getUtilityRequirements(),
        assignments
    );
    return utilityAssignments
        .map(assignmentsForUtilities ->
            new TeamCandidate(new Team(strategy, assignments, assignmentsForUtilities), usedPlayers, true)
        )
        .orElseGet(() -> new TeamCandidate(new Team(strategy, assignments), usedPlayers, false));
  }

  private StrategySlot chooseBestSlotForPlayer(
      PlayerSignup player,
      List<StrategySlot> openSlots,
      List<PlayerSignup> remainingPlayers
  ) {
    return openSlots.stream()
        .filter(player::canFillSlot)
        .min(Comparator.comparingLong(slot -> countEligiblePlayers(slot, remainingPlayers)))
        .orElse(null);
  }

  private long countEligiblePlayers(StrategySlot slot, List<PlayerSignup> remainingPlayers) {
    return remainingPlayers.stream()
        .filter(playerSignup -> playerSignup.canFillSlot(slot))
        .count();
  }

  private Optional<List<UtilityAssignment>> assignUtilities(
      List<StrategyUtilityRequirement> requirements,
      List<SlotAssignment> slotAssignments
  ) {
    List<UtilityAssignment> utilityAssignments = new ArrayList<>();

    for (StrategyUtilityRequirement requirement : requirements) {
      int requiredCount = Math.max(0, requirement.getRequiredCount());
      List<SlotAssignment> eligibleAssignments = slotAssignments.stream()
          .filter(slotAssignment -> requirement.canBeProvidedFromSlot(slotAssignment.slot()))
          .filter(slotAssignment -> slotAssignment.playerSignup().canProvideUtility(requirement.getUtility()))
          .filter(slotAssignment -> !alreadyAssignedForUtility(
              utilityAssignments,
              requirement,
              slotAssignment.playerSignup()
          ))
          .limit(requiredCount)
          .toList();

      if (eligibleAssignments.size() < requiredCount) {
        return Optional.empty();
      }

      eligibleAssignments.forEach(slotAssignment -> utilityAssignments.add(new UtilityAssignment(
          requirement.getUtility(),
          slotAssignment.playerSignup(),
          slotAssignment.slot()
      )));
    }

    return Optional.of(utilityAssignments);
  }

  private boolean alreadyAssignedForUtility(
      List<UtilityAssignment> utilityAssignments,
      StrategyUtilityRequirement requirement,
      PlayerSignup playerSignup
  ) {
    return utilityAssignments.stream().anyMatch(assignment ->
        assignment.utility().sameUtilityAs(requirement.getUtility())
            && assignment.playerSignup() == playerSignup
    );
  }

  private record SearchResult(List<Team> teams, List<PlayerSignup> unmatchedPlayers, int assignedPlayers) {
  }

  private record TeamCandidate(Team team, List<PlayerSignup> usedPlayers, boolean complete) {
    private int assignedPlayers() {
      return usedPlayers.size();
    }
  }
}
