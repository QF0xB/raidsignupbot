package de.qf0xb.raidsignupbot.application.service;

import de.qf0xb.raidsignupbot.application.dto.TeamGeneratorResult;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventMemberSignup;
import de.qf0xb.raidsignupbot.domain.model.event.EventSlot;
import de.qf0xb.raidsignupbot.domain.model.event.EventTeam;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.template.EventUtilityRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class BeamTeamGeneratorService implements TeamGeneratorService {
  private static final Logger log = Logger.getLogger(BeamTeamGeneratorService.class);
  private static final int MIN_BEAM_WIDTH = 24;
  private static final int REMAINING_SIGNUPS_BEAM_MULTIPLIER = 2;

  @Override
  public TeamGeneratorResult generateTeams(Event event, List<EventMemberSignup> signups) {
    List<EventMemberSignup> remainingSignups = new ArrayList<>(signups);
    if (event.getTemplate() == null || event.getTemplate().getStrategies().isEmpty()) {
      return new TeamGeneratorResult(List.of(), remainingSignups);
    }

    boolean hosterSignedUp = remainingSignups.stream()
        .anyMatch(signup -> sameMember(signup.getGuildMember(), event.getHoster()));

    List<BeamState> beam = new ArrayList<>();
    beam.add(new BeamState(List.of(), remainingSignups, false, 0, 0, Integer.MAX_VALUE));

    BeamState bestTerminal = null;
    while (!beam.isEmpty()) {
      List<BeamState> nextBeam = new ArrayList<>();
      boolean expandedAny = false;

      for (BeamState state : beam) {
        boolean expandedState = false;

        for (EventStrategy strategy : event.getTemplate().getStrategies()) {
          for (TeamCandidate candidate : buildTeamCandidates(strategy, state.remainingSignups())) {
            expandedAny = true;
            expandedState = true;

            List<EventMemberSignup> nextRemainingSignups = new ArrayList<>(state.remainingSignups());
            nextRemainingSignups.removeAll(candidate.usedSignups());

            List<EventTeam> nextTeams = new ArrayList<>(state.teams().size() + 1);
            nextTeams.addAll(state.teams());
            nextTeams.add(candidate.team());

            BeamState nextState = new BeamState(
                List.copyOf(nextTeams),
                List.copyOf(nextRemainingSignups),
                state.hosterMatched() || candidate.includesHoster(event.getHoster()),
                state.assignedMembers() + candidate.assignedMembers(),
                state.priorityScore() + candidate.priorityScore(),
                state.flexibilityScore() + candidate.flexibilityScore()
            );
            nextBeam.add(nextState);
          }
        }

        if (!expandedState) {
          bestTerminal = betterOf(bestTerminal, state, hosterSignedUp);
        }
      }

      if (!expandedAny) {
        break;
      }

      nextBeam = dedupeStates(nextBeam, hosterSignedUp);
      nextBeam.sort(stateComparator(hosterSignedUp));
      beam = nextBeam.stream().limit(beamWidth(beam)).toList();
    }

    BeamState best = bestTerminal;
    for (BeamState state : beam) {
      best = betterOf(best, state, hosterSignedUp);
    }

    if (best == null) {
      best = new BeamState(List.of(), List.copyOf(remainingSignups), false, 0, 0, Integer.MAX_VALUE);
    }

    if (hosterSignedUp && !best.hosterMatched()) {
      return new TeamGeneratorResult(List.of(), List.copyOf(signups));
    }

    log.infof(
        "Beam team generation finished: teams=%d, unmatched=%d",
        best.teams().size(),
        best.remainingSignups().size()
    );
    return new TeamGeneratorResult(best.teams(), best.remainingSignups());
  }

  private int beamWidth(List<BeamState> beam) {
    int maxRemainingSignups = beam.stream()
        .mapToInt(state -> state.remainingSignups().size())
        .max()
        .orElse(0);
    return Math.max(MIN_BEAM_WIDTH, maxRemainingSignups * REMAINING_SIGNUPS_BEAM_MULTIPLIER);
  }

  private String buildCacheKey(List<EventMemberSignup> remainingSignups) {
    return remainingSignups.stream()
        .map(signup -> signup.getGuildMember().getUserId())
        .sorted()
        .collect(Collectors.joining("|"));
  }

  private List<BeamState> dedupeStates(List<BeamState> states, boolean hosterSignedUp) {
    Map<String, BeamState> bestStates = new HashMap<>();
    for (BeamState state : states) {
      String key = buildCacheKey(state.remainingSignups());
      bestStates.merge(
          key,
          state,
          (left, right) -> stateComparator(hosterSignedUp).compare(left, right) <= 0 ? left : right
      );
    }
    return new ArrayList<>(bestStates.values());
  }

  private Comparator<BeamState> stateComparator(boolean hosterSignedUp) {
    return (left, right) -> {
      if (hosterSignedUp && left.hosterMatched() != right.hosterMatched()) {
        return left.hosterMatched() ? -1 : 1;
      }
      if (left.teams().size() != right.teams().size()) {
        return Integer.compare(right.teams().size(), left.teams().size());
      }
      if (left.assignedMembers() != right.assignedMembers()) {
        return Integer.compare(right.assignedMembers(), left.assignedMembers());
      }
      if (left.priorityScore() != right.priorityScore()) {
        return Integer.compare(right.priorityScore(), left.priorityScore());
      }
      return Integer.compare(left.flexibilityScore(), right.flexibilityScore());
    };
  }

  private BeamState betterOf(BeamState currentBest, BeamState candidate, boolean hosterSignedUp) {
    if (currentBest == null) {
      return candidate;
    }
    return stateComparator(hosterSignedUp).compare(candidate, currentBest) < 0 ? candidate : currentBest;
  }

  private List<TeamCandidate> buildTeamCandidates(
      EventStrategy strategy,
      List<EventMemberSignup> remainingSignups
  ) {
    List<TeamCandidate> candidates = new ArrayList<>();
    assignSlots(
        strategy,
        new EventTeam(strategy).getSlots(),
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

    int remainingSlots = slots.size() - slotIndex;
    int availableSignups = remainingSignups.size() - usedSignups.size();
    if (availableSignups < remainingSlots) {
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
            .thenComparing(assignment -> -assignment.signup().getGuildMember().getPriority())
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
            .thenComparing(assignment -> -assignment.signup().getGuildMember().getPriority())
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
    EventTeam team = new EventTeam(strategy);
    primaryAssignments.forEach(assignment -> team.assign(assignment.roleKey(), assignment.signup().getGuildMember()));
    utilityAssignments.forEach(assignment ->
        team.assignUtility(assignment.roleKey(), assignment.signup().getGuildMember()));
    return team;
  }

  private static boolean sameMember(GuildMember left, GuildMember right) {
    return left != null && right != null && left.getUserId().equals(right.getUserId());
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
          .map(EventMemberSignup::getGuildMember)
          .mapToLong(GuildMember::getPriority)
          .mapToInt(priority -> Math.toIntExact(priority))
          .sum();
    }

    private int flexibilityScore() {
      return usedSignups.stream()
          .mapToInt(signup -> signup.getRoleKeys().size())
          .sum();
    }
  }

  private record BeamState(
      List<EventTeam> teams,
      List<EventMemberSignup> remainingSignups,
      boolean hosterMatched,
      int assignedMembers,
      int priorityScore,
      int flexibilityScore
  ) {
  }
}
