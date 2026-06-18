package de.qf0xb.raidsignupbot.event.domain.service;

import de.qf0xb.raidsignupbot.event.domain.model.Event;
import de.qf0xb.raidsignupbot.event.domain.model.EventMemberSignup;
import de.qf0xb.raidsignupbot.event.domain.model.EventRole;
import de.qf0xb.raidsignupbot.event.domain.model.EventStrategy;
import de.qf0xb.raidsignupbot.event.domain.model.EventTeam;
import de.qf0xb.raidsignupbot.guild.domain.model.GuildMember;

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
  public TeamGeneratorResult generateTeams(Event event, List<EventMemberSignup> signups) {
    List<EventMemberSignup> remainingSignups = new ArrayList<>(signups);
    if (event.getTemplate() == null || event.getTemplate().getStrategies().isEmpty()) {
      return new TeamGeneratorResult(List.of(), remainingSignups);
    }

    boolean hosterSignedUp = remainingSignups.stream()
        .anyMatch(signup -> sameMember(signup.getGuildMember(), event.getHoster()));

    SearchResult bestResult = searchBestResult(
        event,
        event.getTemplate().getStrategies(),
        remainingSignups,
        new HashMap<>(),
        hosterSignedUp
    );

    if (hosterSignedUp && !bestResult.hosterMatched()) {
      return new TeamGeneratorResult(List.of(), remainingSignups);
    }
    return new TeamGeneratorResult(bestResult.teams(), bestResult.unmatchedSignups());
  }

  private SearchResult searchBestResult(
      Event event,
      List<EventStrategy> strategies,
      List<EventMemberSignup> remainingSignups,
      Map<String, SearchResult> memo,
      boolean hosterSignedUp
  ) {
    String cacheKey = buildCacheKey(remainingSignups);
    SearchResult cachedResult = memo.get(cacheKey);
    if (cachedResult != null) {
      return cachedResult;
    }

    SearchResult bestResult = new SearchResult(List.of(), List.copyOf(remainingSignups), false, 0, 0, 0);

    for (EventStrategy strategy : strategies) {
      for (TeamCandidate candidate : buildTeamCandidates(event, strategy, remainingSignups)) {
        List<EventMemberSignup> nextRemainingSignups = new ArrayList<>(remainingSignups);
        nextRemainingSignups.removeAll(candidate.usedSignups());

        SearchResult tailResult = searchBestResult(
            event,
            strategies,
            nextRemainingSignups,
            memo,
            hosterSignedUp
        );
        List<EventTeam> combinedTeams = new ArrayList<>(tailResult.teams().size() + 1);
        combinedTeams.add(candidate.team());
        combinedTeams.addAll(tailResult.teams());

        SearchResult combinedResult = new SearchResult(
            List.copyOf(combinedTeams),
            tailResult.unmatchedSignups(),
            candidate.includesHoster(event.getHoster()) || tailResult.hosterMatched(),
            candidate.assignedMembers() + tailResult.assignedMembers(),
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
    if (candidate.assignedMembers() != currentBest.assignedMembers()) {
      return candidate.assignedMembers() > currentBest.assignedMembers();
    }
    if (candidate.priorityScore() != currentBest.priorityScore()) {
      return candidate.priorityScore() > currentBest.priorityScore();
    }
    return candidate.flexibilityScore() > currentBest.flexibilityScore();
  }

  private String buildCacheKey(List<EventMemberSignup> remainingSignups) {
    return remainingSignups.stream()
        .map(signup -> signup.getGuildMember().getPlayerId())
        .sorted()
        .collect(Collectors.joining("|"));
  }

  private List<TeamCandidate> buildTeamCandidates(
      Event event,
      EventStrategy strategy,
      List<EventMemberSignup> remainingSignups
  ) {
    List<TeamCandidate> candidates = new ArrayList<>();
    assignPrimarySlots(
        event,
        strategy,
        expandRoles(strategy.getPrimaryRoles()),
        remainingSignups,
        0,
        new ArrayList<>(),
        new HashSet<>(),
        candidates
    );
    return candidates;
  }

  private void assignPrimarySlots(
      Event event,
      EventStrategy strategy,
      List<EventRole> primarySlots,
      List<EventMemberSignup> remainingSignups,
      int slotIndex,
      List<RoleMemberAssignment> primaryAssignments,
      Set<EventMemberSignup> usedSignups,
      List<TeamCandidate> candidates
  ) {
    if (slotIndex == primarySlots.size()) {
      Optional<List<RoleMemberAssignment>> utilityAssignments = assignUtilities(
          expandRoles(strategy.getUtilities()),
          primaryAssignments
      );
      utilityAssignments.ifPresent(assignmentsForUtilities -> {
        EventTeam team = buildTeam(event, strategy, primaryAssignments, assignmentsForUtilities);
        candidates.add(new TeamCandidate(team, primaryAssignments.stream()
            .map(RoleMemberAssignment::signup)
            .distinct()
            .toList()));
      });
      return;
    }

    EventRole role = primarySlots.get(slotIndex);
    List<EventMemberSignup> eligibleSignups = remainingSignups.stream()
        .filter(signup -> !usedSignups.contains(signup))
        .filter(signup -> canFillPrimaryRole(signup, role))
        .sorted(Comparator
            .comparingInt((EventMemberSignup signup) -> selectedPrimaryRoleCountFor(signup, strategy))
            .thenComparing(signup -> signup.getGuildMember().getPlayerId()))
        .toList();

    for (EventMemberSignup signup : eligibleSignups) {
      primaryAssignments.add(new RoleMemberAssignment(role, signup));
      usedSignups.add(signup);
      assignPrimarySlots(
          event,
          strategy,
          primarySlots,
          remainingSignups,
          slotIndex + 1,
          primaryAssignments,
          usedSignups,
          candidates
      );
      usedSignups.remove(signup);
      primaryAssignments.removeLast();
    }
  }

  private Optional<List<RoleMemberAssignment>> assignUtilities(
      List<EventRole> utilitySlots,
      List<RoleMemberAssignment> primaryAssignments
  ) {
    List<RoleMemberAssignment> utilityAssignments = new ArrayList<>();

    for (EventRole utility : utilitySlots) {
      List<RoleMemberAssignment> eligibleAssignments = primaryAssignments.stream()
          .filter(primaryAssignment -> utility.isEligible(primaryAssignment.role()))
          .filter(primaryAssignment -> canProvideUtility(primaryAssignment.signup(), utility))
          .filter(primaryAssignment -> !alreadyAssignedForUtility(
              utilityAssignments,
              utility,
              primaryAssignment.signup().getGuildMember()
          ))
          .sorted(Comparator
              .comparingInt((RoleMemberAssignment assignment) -> assignment.signup().getUtilities().size())
              .thenComparing(assignment -> assignment.signup().getGuildMember().getPlayerId()))
          .toList();

      if (eligibleAssignments.isEmpty()) {
        return Optional.empty();
      }

      RoleMemberAssignment selectedAssignment = eligibleAssignments.getFirst();
      utilityAssignments.add(new RoleMemberAssignment(utility, selectedAssignment.signup()));
    }

    return Optional.of(List.copyOf(utilityAssignments));
  }

  private boolean alreadyAssignedForUtility(
      List<RoleMemberAssignment> utilityAssignments,
      EventRole utility,
      GuildMember member
  ) {
    return utilityAssignments.stream().anyMatch(assignment ->
        assignment.role().sameRoleAs(utility)
            && sameMember(assignment.signup().getGuildMember(), member)
    );
  }

  private EventTeam buildTeam(
      Event event,
      EventStrategy strategy,
      List<RoleMemberAssignment> primaryAssignments,
      List<RoleMemberAssignment> utilityAssignments
  ) {
    EventTeam team = new EventTeam(event, strategy);
    primaryAssignments.forEach(assignment ->
        team.assignMember(assignment.role(), assignment.signup().getGuildMember()));
    utilityAssignments.forEach(assignment ->
        team.assignMember(assignment.role(), assignment.signup().getGuildMember()));
    return team;
  }

  private List<EventRole> expandRoles(List<EventRole> roles) {
    List<EventRole> expandedRoles = new ArrayList<>();
    for (EventRole role : roles) {
      if (role.getRequiredCount() < 0) {
        throw new IllegalArgumentException("Role required count cannot be negative: " + role.getName());
      }
      for (int slot = 0; slot < role.getRequiredCount(); slot++) {
        expandedRoles.add(role);
      }
    }
    return expandedRoles;
  }

  private boolean canFillPrimaryRole(EventMemberSignup signup, EventRole role) {
    return signup.getPrimaryRoles().stream().anyMatch(selectedRole -> selectedRole.sameRoleAs(role));
  }

  private boolean canProvideUtility(EventMemberSignup signup, EventRole utility) {
    return signup.getUtilities().stream().anyMatch(selectedUtility -> selectedUtility.sameRoleAs(utility));
  }

  private int selectedPrimaryRoleCountFor(EventMemberSignup signup, EventStrategy strategy) {
    return (int) strategy.getPrimaryRoles().stream()
        .filter(role -> canFillPrimaryRole(signup, role))
        .count();
  }

  private boolean sameMember(GuildMember left, GuildMember right) {
    return left != null
        && right != null
        && left.getPlayerId().equals(right.getPlayerId());
  }

  private record RoleMemberAssignment(EventRole role, EventMemberSignup signup) {
  }

  private record SearchResult(
      List<EventTeam> teams,
      List<EventMemberSignup> unmatchedSignups,
      boolean hosterMatched,
      int assignedMembers,
      int priorityScore,
      int flexibilityScore
  ) {
  }

  private record TeamCandidate(EventTeam team, List<EventMemberSignup> usedSignups) {
    private int assignedMembers() {
      return usedSignups.size();
    }

    private boolean includesHoster(GuildMember hoster) {
      return hoster != null
          && usedSignups.stream().anyMatch(signup ->
          signup.getGuildMember().getPlayerId().equals(hoster.getPlayerId()));
    }

    private int priorityScore() {
      return usedSignups.stream()
          .map(EventMemberSignup::getGuildMember)
          .mapToInt(GuildMember::getPriority)
          .sum();
    }

    private int flexibilityScore() {
      return usedSignups.stream()
          .mapToInt(signup -> signup.getPrimaryRoles().size())
          .sum();
    }
  }
}
