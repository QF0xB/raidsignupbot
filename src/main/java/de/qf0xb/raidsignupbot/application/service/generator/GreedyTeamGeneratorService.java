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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class GreedyTeamGeneratorService implements TeamGeneratorService {
  private static final Logger log = Logger.getLogger(GreedyTeamGeneratorService.class);

  @Override
  public TeamGeneratorResult generateTeams(Event event, List<EventMemberSignup> signups) {
    List<EventMemberSignup> remainingSignups = new ArrayList<>(signups);
    List<EventTeam> teams = new ArrayList<>();
    Map<EventMemberSignup, Integer> assignmentCounts = new HashMap<>();

    log.infof(
        "Greedy team generation started: signups=%d, strategies=%d",
        remainingSignups.size(),
        event.getTemplate() == null ? 0 : event.getTemplate().getStrategies().size()
    );

    if (event.getTemplate() == null || event.getTemplate().getStrategies().isEmpty()) {
      return new TeamGeneratorResult(List.of(), remainingSignups);
    }

    boolean hosterSignedUp = remainingSignups.stream()
        .anyMatch(signup -> sameMember(signup.getGuildMember(), event.getHoster()));
    boolean hosterMatched = false;

    while (true) {
      TeamCandidate bestCandidate = null;
      for (EventStrategy strategy : event.getTemplate().getStrategies()) {
        TeamCandidate candidate = buildGreedyCandidate(strategy, remainingSignups, assignmentCounts);
        if (candidate != null && isBetter(candidate, bestCandidate, assignmentCounts)) {
          bestCandidate = candidate;
        }
      }

      if (bestCandidate == null) {
        break;
      }

      teams.add(bestCandidate.team());
      hosterMatched |= bestCandidate.includesHoster(event.getHoster());
      bestCandidate.usedSignups().forEach(signup -> assignmentCounts.merge(signup, 1, Integer::sum));
      remainingSignups.removeIf(signup -> assignmentCounts.getOrDefault(signup, 0) >= signup.getMaxAssignments());
    }

    if (hosterSignedUp && !hosterMatched) {
      return new TeamGeneratorResult(List.of(), List.copyOf(signups));
    }

    if (signups.stream().anyMatch(EventMemberSignup::wantsDualBox)) {
      DualBoxTeamGrouper.group(teams, signups);
    }
    List<EventMemberSignup> unmatched = signups.stream()
        .filter(signup -> assignmentCounts.getOrDefault(signup, 0) == 0).toList();
    log.infof(
        "Greedy team generation finished: teams=%d, unmatched=%d",
        teams.size(),
        unmatched.size()
    );
    return new TeamGeneratorResult(List.copyOf(teams), unmatched);
  }

  private TeamCandidate buildGreedyCandidate(EventStrategy strategy, List<EventMemberSignup> remainingSignups,
      Map<EventMemberSignup, Integer> assignmentCounts) {
    List<TeamCandidate> candidates = new ArrayList<>();
    assignSlots(
        strategy,
        new EventTeam(strategy).getSlots(),
        remainingSignups,
        0,
        new ArrayList<>(),
        new HashMap<>(assignmentCounts),
        candidates
    );
    return candidates.stream()
        .min(teamCandidateComparator(assignmentCounts))
        .orElse(null);
  }

  private void assignSlots(
      EventStrategy strategy,
      List<EventSlot> slots,
      List<EventMemberSignup> remainingSignups,
      int slotIndex,
      List<RoleAssignment> assignments,
      Map<EventMemberSignup, Integer> assignmentCounts,
      List<TeamCandidate> candidates
  ) {
    int remainingSlots = slots.size() - slotIndex;
    int availableSignups = remainingSignups.stream()
        .mapToInt(signup -> signup.getMaxAssignments() - assignmentCounts.getOrDefault(signup, 0)).sum();
    if (availableSignups < remainingSlots) {
      return;
    }

    if (slotIndex == slots.size()) {
      assignUtilities(strategy, assignments, 0, new ArrayList<>(), candidates);
      return;
    }

    EventSlot slot = slots.get(slotIndex);
    List<RoleAssignment> eligibleAssignments = eligibleAssignmentsFor(slot, remainingSignups, assignmentCounts);
    if (eligibleAssignments.isEmpty()) {
      return;
    }

    for (RoleAssignment assignment : eligibleAssignments) {
      assignments.add(assignment);
      assignmentCounts.merge(assignment.signup(), 1, Integer::sum);
      assignSlots(strategy, slots, remainingSignups, slotIndex + 1, assignments, assignmentCounts, candidates);
      assignmentCounts.merge(assignment.signup(), -1, Integer::sum);
      assignments.remove(assignments.size() - 1);
    }
  }

  private List<RoleAssignment> eligibleAssignmentsFor(
      EventSlot slot,
      List<EventMemberSignup> remainingSignups,
      Map<EventMemberSignup, Integer> assignmentCounts
  ) {
    return remainingSignups.stream()
        .filter(signup -> assignmentCounts.getOrDefault(signup, 0) < signup.getMaxAssignments())
        .flatMap(signup -> signup.getRoleKeys().stream()
            .filter(slot::matches)
            .map(roleKey -> new SignupChoice(roleKey, signup)))
        .sorted(Comparator
            .comparingInt((SignupChoice choice) -> choice.roleKey().equals(slot.getKey()) ? 0 : 1)
            .thenComparingInt(choice -> choice.signup().getRoleKeys().size())
            .thenComparing(choice -> -choice.signup().getEffectivePriority())
            .thenComparing(choice -> choice.signup().getGuildMember().getUserId()))
        .map(choice -> new RoleAssignment(choice.roleKey(), choice.signup()))
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
      candidates.add(new TeamCandidate(team, primaryAssignments.stream().map(RoleAssignment::signup).toList()));
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

  private EventTeam buildTeam(
      EventStrategy strategy,
      List<RoleAssignment> primaryAssignments,
      List<RoleAssignment> utilityAssignments
  ) {
    EventTeam team = new EventTeam(strategy);
    primaryAssignments.forEach(assignment -> team.assign(assignment.roleKey(), assignment.signup().getGuildMember()));
    utilityAssignments.forEach(assignment ->
        team.assignUtility(assignment.roleKey(), assignment.signup().getGuildMember()));
    return team;
  }

  private boolean isBetter(TeamCandidate candidate, TeamCandidate currentBest,
      Map<EventMemberSignup, Integer> assignmentCounts) {
    return currentBest == null || teamCandidateComparator(assignmentCounts).compare(candidate, currentBest) < 0;
  }

  private Comparator<TeamCandidate> teamCandidateComparator(Map<EventMemberSignup, Integer> assignmentCounts) {
    return (left, right) -> {
      if (left.assignedMembers() != right.assignedMembers()) {
        return Integer.compare(right.assignedMembers(), left.assignedMembers());
      }
      long leftNewPlayers = left.usedSignups().stream().distinct()
          .filter(signup -> assignmentCounts.getOrDefault(signup, 0) == 0).count();
      long rightNewPlayers = right.usedSignups().stream().distinct()
          .filter(signup -> assignmentCounts.getOrDefault(signup, 0) == 0).count();
      if (leftNewPlayers != rightNewPlayers) {
        return Long.compare(rightNewPlayers, leftNewPlayers);
      }
      if (left.priorityScore() != right.priorityScore()) {
        return Integer.compare(right.priorityScore(), left.priorityScore());
      }
      return Integer.compare(left.flexibilityScore(), right.flexibilityScore());
    };
  }

  private static boolean sameMember(GuildMember left, GuildMember right) {
    return left != null
        && right != null
        && left.getUserId().equals(right.getUserId());
  }

  private record SignupChoice(String roleKey, EventMemberSignup signup) {
  }

  private record RoleAssignment(String roleKey, EventMemberSignup signup) {
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
          .distinct()
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
