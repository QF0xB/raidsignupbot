package de.qf0xb.raidsignupbot.application.service.generator;

import de.qf0xb.raidsignupbot.application.TeamGeneratorResult;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventMemberSignup;
import de.qf0xb.raidsignupbot.domain.model.event.EventSlot;
import de.qf0xb.raidsignupbot.domain.model.event.EventTeam;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventUtilityRole;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@ApplicationScoped
public class ExactTeamGeneratorService implements TeamGeneratorService {
  private static final Logger log = Logger.getLogger(ExactTeamGeneratorService.class);

  public TeamGeneratorResult generateTeams(Event event, List<EventMemberSignup> signups) {
    List<EventMemberSignup> remainingSignups = new ArrayList<>(signups);
    log.infof(
        "Team generation started: signups=%d, strategies=%d, hoster=%s",
        remainingSignups.size(),
        event.getTemplate() == null ? 0 : event.getTemplate().getStrategies().size(),
        event.getHoster() == null ? "<none>" : event.getHoster().getUserId()
    );
    if (event.getTemplate() == null || event.getTemplate().getStrategies().isEmpty()) {
      log.info("Team generation skipped: no template strategies configured");
      return new TeamGeneratorResult(List.of(), remainingSignups);
    }

    boolean hosterSignedUp = remainingSignups.stream()
        .anyMatch(signup -> sameMember(signup.getGuildMember(), event.getHoster()));

    SearchResult bestResult = searchBestResult(
        event,
        event.getTemplate().getStrategies(),
        remainingSignups,
        new HashMap<>(),
        hosterSignedUp,
        new AtomicLong()
    );

    if (hosterSignedUp && !bestResult.hosterMatched()) {
      log.info("Team generation failed hoster constraint; returning no teams");
      return new TeamGeneratorResult(List.of(), remainingSignups);
    }

    log.infof(
        "Team generation finished: teams=%d, unmatched=%d",
        bestResult.teams().size(),
        bestResult.unmatchedSignups().size()
    );
    return new TeamGeneratorResult(bestResult.teams(), bestResult.unmatchedSignups());
  }

  private SearchResult searchBestResult(
      Event event,
      Set<EventStrategy> strategies,
      List<EventMemberSignup> remainingSignups,
      Map<String, SearchResult> memo,
      boolean hosterSignedUp,
      AtomicLong statesVisited
  ) {
    long visited = statesVisited.incrementAndGet();
    if (visited == 1 || visited % 500 == 0) {
      log.infof(
          "Team search checkpoint: visited=%d, remainingSignups=%d, memoSize=%d",
          visited,
          remainingSignups.size(),
          memo.size()
      );
    }

    String cacheKey = buildCacheKey(remainingSignups);
    SearchResult cachedResult = memo.get(cacheKey);
    if (cachedResult != null) {
      return cachedResult;
    }

    SearchResult bestResult = new SearchResult(
        List.of(),
        List.copyOf(remainingSignups),
        false,
        0,
        0,
        Integer.MAX_VALUE
    );

    boolean foundCandidate = false;
    for (EventStrategy strategy : strategies) {
      for (TeamCandidate candidate : buildTeamCandidates(strategy, remainingSignups)) {
        foundCandidate = true;

        List<EventMemberSignup> nextRemainingSignups = new ArrayList<>(remainingSignups);
        nextRemainingSignups.removeAll(candidate.usedSignups());

        SearchResult tailResult = searchBestResult(
            event,
            strategies,
            nextRemainingSignups,
            memo,
            hosterSignedUp,
            statesVisited
        );

        List<EventTeam> combinedTeams = new ArrayList<>(1 + tailResult.teams().size());
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

    if (!foundCandidate) {
      memo.put(cacheKey, bestResult);
      return bestResult;
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
    return candidate.flexibilityScore() < currentBest.flexibilityScore();
  }

  private String buildCacheKey(List<EventMemberSignup> remainingSignups) {
    return remainingSignups.stream()
        .map(signup -> signup.getGuildMember().getUserId())
        .sorted()
        .collect(Collectors.joining("|"));
  }

  private List<TeamCandidate> buildTeamCandidates(
      EventStrategy strategy,
      List<EventMemberSignup> remainingSignups
  ) {
    List<TeamCandidate> candidates = new ArrayList<>();
    List<EventSlot> slots = new EventTeam(strategy).getSlots();
    assignSlots(
        strategy,
        slots,
        remainingSignups,
        0,
        new ArrayList<>(),
        new HashSet<>(),
        candidates
    );
    return candidates;
  }

  private void assignSlots(
      EventStrategy strategy,
      List<EventSlot> slots,
      List<EventMemberSignup> remainingSignups,
      int slotIndex,
      List<RoleAssignment> assignments,
      Set<EventMemberSignup> usedSignups,
      List<TeamCandidate> candidates
  ) {
    int remainingSlots = slots.size() - slotIndex;
    int availableSignups = remainingSignups.size() - usedSignups.size();
    if (availableSignups < remainingSlots) {
      return;
    }

    if (slotIndex == slots.size()) {
      assignUtilities(
          strategy,
          assignments,
          0,
          new ArrayList<>(),
          candidates
      );
      return;
    }

    EventSlot slot = slots.get(slotIndex);
    List<RoleAssignment> eligibleAssignments = eligibleAssignmentsFor(slot, remainingSignups, usedSignups);
    if (eligibleAssignments.isEmpty()) {
      return;
    }

    for (RoleAssignment assignment : eligibleAssignments) {
      assignments.add(assignment);
      usedSignups.add(assignment.signup());
      assignSlots(strategy, slots, remainingSignups, slotIndex + 1, assignments, usedSignups, candidates);
      usedSignups.remove(assignment.signup());
      assignments.remove(assignments.size() - 1);
    }
  }

  private void assignUtilities(
      EventStrategy strategy,
      List<RoleAssignment> primaryAssignments,
      int utilityIndex,
      List<RoleAssignment> utilityAssignments,
      List<TeamCandidate> candidates
  ) {
    List<EventUtilityRole> utilityRoles = expandUtilityRoles(strategy);
    if (utilityIndex == utilityRoles.size()) {
      EventTeam team = buildTeam(strategy, primaryAssignments, utilityAssignments);
      candidates.add(new TeamCandidate(team, primaryAssignments.stream().map(RoleAssignment::signup).distinct().toList()));
      return;
    }

    EventUtilityRole utilityRole = utilityRoles.get(utilityIndex);
    List<RoleAssignment> eligibleAssignments = primaryAssignments.stream()
        .filter(assignment -> utilityRole.isEligibleFor(assignment.roleKey()))
        .filter(assignment -> assignment.signup().wantsRole(utilityRole.getKey()))
        .filter(assignment -> !alreadyAssignedUtility(utilityAssignments, utilityRole, assignment.signup()))
        .sorted(Comparator
            .comparingInt((RoleAssignment assignment) -> assignment.signup().getRoleKeys().size())
            .thenComparing(assignment -> -assignment.signup().getEffectivePriority())
            .thenComparing(assignment -> assignment.signup().getGuildMember().getUserId()))
        .toList();

    for (RoleAssignment assignment : eligibleAssignments) {
      utilityAssignments.add(new RoleAssignment(utilityRole.getKey(), assignment.signup()));
      assignUtilities(strategy, primaryAssignments, utilityIndex + 1, utilityAssignments, candidates);
      utilityAssignments.remove(utilityAssignments.size() - 1);
    }
  }

  private List<EventUtilityRole> expandUtilityRoles(EventStrategy strategy) {
    List<EventUtilityRole> utilityRoles = new ArrayList<>();
    for (EventUtilityRole utilityRole : strategy.getUtilityRoles()) {
      for (int i = 0; i < utilityRole.getRequiredCount(); i++) {
        utilityRoles.add(utilityRole);
      }
    }
    return utilityRoles;
  }

  private boolean alreadyAssignedUtility(
      List<RoleAssignment> utilityAssignments,
      EventUtilityRole utilityRole,
      EventMemberSignup signup
  ) {
    return utilityAssignments.stream()
        .anyMatch(assignment -> assignment.roleKey().equals(utilityRole.getKey())
            && sameMember(assignment.signup().getGuildMember(), signup.getGuildMember()));
  }

  private List<RoleAssignment> eligibleAssignmentsFor(
      EventSlot slot,
      List<EventMemberSignup> remainingSignups,
      Set<EventMemberSignup> usedSignups
  ) {
    return remainingSignups.stream()
        .filter(signup -> !usedSignups.contains(signup))
        .flatMap(signup -> signup.getRoleKeys().stream()
            .filter(slot::matches)
            .map(roleKey -> new RoleAssignment(roleKey, signup)))
        .sorted(Comparator
            .comparingInt((RoleAssignment assignment) -> assignment.roleKey().equals(slot.getKey()) ? 0 : 1)
            .thenComparingInt(assignment -> assignment.signup().getRoleKeys().size())
            .thenComparing(assignment -> -assignment.signup().getEffectivePriority())
            .thenComparing(assignment -> assignment.signup().getGuildMember().getUserId()))
        .collect(Collectors.collectingAndThen(
            Collectors.toMap(
                RoleAssignment::signup,
                assignment -> assignment,
                (left, right) -> left,
                java.util.LinkedHashMap::new
            ),
            map -> new ArrayList<>(map.values())
        ));
  }

  private EventTeam buildTeam(
      EventStrategy strategy,
      List<RoleAssignment> primaryAssignments,
      List<RoleAssignment> utilityAssignments
  ) {
    if (primaryAssignments.isEmpty()) {
      throw new IllegalStateException("Cannot build an empty team");
    }

    EventTeam team = new EventTeam(strategy);
    primaryAssignments.forEach(assignment ->
        team.assign(assignment.roleKey(), assignment.signup().getGuildMember()));
    utilityAssignments.forEach(assignment ->
        team.assignUtility(assignment.roleKey(), assignment.signup().getGuildMember()));
    return team;
  }

  private static boolean sameMember(GuildMember left, GuildMember right) {
    return left != null
        && right != null
        && left.getUserId().equals(right.getUserId());
  }

  private record RoleAssignment(String roleKey, EventMemberSignup signup) {
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
          && usedSignups.stream().anyMatch(signup -> sameMember(signup.getGuildMember(), hoster));
    }

    private int priorityScore() {
      return usedSignups.stream()
          .mapToLong(EventMemberSignup::getEffectivePriority)
          .mapToInt(priority -> Math.toIntExact(priority))
          .sum();
    }

    private int flexibilityScore() {
      return usedSignups.stream()
          .mapToInt(signup -> signup.getRoleKeys().size())
          .sum();
    }
  }
}
