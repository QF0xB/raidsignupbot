package de.qf0xb.raidsignupbot.application.service.generator;

import com.google.ortools.Loader;
import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.Literal;
import com.google.ortools.sat.LinearExpr;
import de.qf0xb.raidsignupbot.application.TeamGeneratorResult;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventMemberSignup;
import de.qf0xb.raidsignupbot.domain.model.event.EventTeam;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventUtilityRole;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class OrToolsTeamGeneratorService implements TeamGeneratorService {
  private static final Logger log = Logger.getLogger(OrToolsTeamGeneratorService.class);
  private static final double SOLVER_TIME_LIMIT_SECONDS = 3.0;

  private TeamGeneratorService greedyFallback = new GreedyTeamGeneratorService();

  static {
    Loader.loadNativeLibraries();
  }

  @Inject
  void setGreedyFallback(GreedyTeamGeneratorService greedyFallback) {
    this.greedyFallback = greedyFallback;
  }

  @Override
  public TeamGeneratorResult generateTeams(Event event, List<EventMemberSignup> signups) {
    List<EventMemberSignup> members = new ArrayList<>(signups);
    if (event.getTemplate() == null || event.getTemplate().getStrategies().isEmpty()) {
      return new TeamGeneratorResult(List.of(), members);
    }

    List<EventStrategy> strategies = new ArrayList<>(event.getTemplate().getStrategies());
    boolean hosterSignedUp = members.stream()
        .anyMatch(signup -> sameMember(signup.getGuildMember(), event.getHoster()));
    CpModel model = new CpModel();
    Map<String, BoolVar> y = new LinkedHashMap<>();
    Map<String, BoolVar> x = new LinkedHashMap<>();
    Map<String, BoolVar> z = new LinkedHashMap<>();
    Map<String, List<BoolVar>> primaryVarsByMember = new HashMap<>();
    Map<String, BoolVar> selectedByMember = new LinkedHashMap<>();

    Map<String, List<PrimarySlotSpec>> primarySlotsByStrategy = new LinkedHashMap<>();
    Map<String, List<UtilitySlotSpec>> utilitySlotsByStrategy = new LinkedHashMap<>();

    for (EventStrategy strategy : strategies) {
      List<PrimarySlotSpec> primarySlots = expandPrimarySlots(strategy);
      if (primarySlots.isEmpty()) {
        continue;
      }

      List<UtilitySlotSpec> utilitySlots = expandUtilitySlots(strategy);
      primarySlotsByStrategy.put(strategy.getKey(), primarySlots);
      utilitySlotsByStrategy.put(strategy.getKey(), utilitySlots);

      int maxTeams = assignmentCapacity(members) / primarySlots.size();
      for (int t = 0; t < maxTeams; t++) {
        y.put(key(strategy.getKey(), t), model.newBoolVar("y_" + strategy.getKey() + "_" + t));
      }
    }

    if (y.isEmpty()) {
      return new TeamGeneratorResult(List.of(), members);
    }

    for (EventMemberSignup signup : members) {
      String memberId = signup.getGuildMember().getUserId();

      for (EventStrategy strategy : strategies) {
        List<PrimarySlotSpec> primarySlots = primarySlotsByStrategy.get(strategy.getKey());
        if (primarySlots == null) {
          continue;
        }

        int maxTeams = assignmentCapacity(members) / primarySlots.size();
        for (int t = 0; t < maxTeams; t++) {
          for (PrimarySlotSpec primarySlot : primarySlots) {
            if (!signup.wantsRole(primarySlot.roleKey())) {
              continue;
            }

            String varKey = key(memberId, strategy.getKey(), t, primarySlot.variableKey());
            BoolVar var = model.newBoolVar("x_" + memberId + "_" + strategy.getKey() + "_" + t + "_" + primarySlot.variableKey());
            x.put(varKey, var);
            primaryVarsByMember.computeIfAbsent(memberId, ignored -> new ArrayList<>()).add(var);
          }
        }
      }
    }

    for (EventStrategy strategy : strategies) {
      List<PrimarySlotSpec> primarySlots = primarySlotsByStrategy.get(strategy.getKey());
      if (primarySlots == null) {
        continue;
      }

      int maxTeams = assignmentCapacity(members) / primarySlots.size();
      for (int t = 0; t < maxTeams; t++) {
        BoolVar teamActive = y.get(key(strategy.getKey(), t));
        for (PrimarySlotSpec primarySlot : primarySlots) {
          List<Literal> vars = new ArrayList<>();
          for (EventMemberSignup signup : members) {
            String memberId = signup.getGuildMember().getUserId();
            BoolVar var = x.get(key(memberId, strategy.getKey(), t, primarySlot.variableKey()));
            if (var != null) {
              vars.add(var);
            }
          }

          model.addEquality(LinearExpr.sum(vars.toArray(new Literal[0])), teamActive);
        }
      }
    }

    for (EventMemberSignup signup : members) {
      String memberId = signup.getGuildMember().getUserId();
      List<Literal> vars = primaryVarsByMember.getOrDefault(memberId, List.of()).stream()
          .map(var -> (Literal) var)
          .toList();
      if (!vars.isEmpty()) {
        BoolVar selected = model.newBoolVar("selected_" + memberId);
        selectedByMember.put(memberId, selected);
        LinearExpr assignments = LinearExpr.sum(vars.toArray(new Literal[0]));
        model.addGreaterOrEqual(assignments, selected);
        model.addLessOrEqual(assignments, LinearExpr.term(selected, signup.getMaxAssignments()));
      }
    }

    for (EventStrategy strategy : strategies) {
      List<PrimarySlotSpec> primarySlots = primarySlotsByStrategy.get(strategy.getKey());
      List<UtilitySlotSpec> utilitySlots = utilitySlotsByStrategy.get(strategy.getKey());
      if (primarySlots == null) {
        continue;
      }

      int maxTeams = assignmentCapacity(members) / primarySlots.size();
      for (int t = 0; t < maxTeams; t++) {
        BoolVar teamActive = y.get(key(strategy.getKey(), t));

        for (UtilitySlotSpec utilitySlot : utilitySlots) {
          List<Literal> utilityVarsForSlot = new ArrayList<>();
          for (EventMemberSignup signup : members) {
            String memberId = signup.getGuildMember().getUserId();
            if (!signup.wantsRole(utilitySlot.roleKey())) {
              continue;
            }

            String utilKey = key(memberId, strategy.getKey(), t, utilitySlot.variableKey());
            BoolVar utilVar = model.newBoolVar("z_" + memberId + "_" + strategy.getKey() + "_" + t + "_" + utilitySlot.variableKey());
            z.put(utilKey, utilVar);

            List<Literal> eligiblePrimaryAssignments = new ArrayList<>();
            for (String primaryRoleKey : utilitySlot.eligiblePrimaryRoleKeys()) {
              for (PrimarySlotSpec primarySlot : primarySlots) {
                if (!primarySlot.roleKey().equals(primaryRoleKey)) {
                  continue;
                }
                BoolVar primaryVar = x.get(key(memberId, strategy.getKey(), t, primarySlot.variableKey()));
                if (primaryVar != null) {
                  eligiblePrimaryAssignments.add(primaryVar);
                }
              }
            }

            if (eligiblePrimaryAssignments.isEmpty()) {
              model.addEquality(utilVar, 0);
            } else {
              model.addLessOrEqual(utilVar, LinearExpr.sum(eligiblePrimaryAssignments.toArray(new Literal[0])));
            }
            utilityVarsForSlot.add(utilVar);
          }

          if (utilityVarsForSlot.isEmpty()) {
            model.addEquality(teamActive, 0);
          } else {
            model.addEquality(LinearExpr.sum(utilityVarsForSlot.toArray(new Literal[0])), teamActive);
          }
        }

        for (EventMemberSignup signup : members) {
          String memberId = signup.getGuildMember().getUserId();
          List<Literal> memberUtilityVars = new ArrayList<>();
          for (UtilitySlotSpec utilitySlot : utilitySlots) {
            BoolVar utilVar = z.get(key(memberId, strategy.getKey(), t, utilitySlot.variableKey()));
            if (utilVar != null) {
              memberUtilityVars.add(utilVar);
            }
          }
          if (!memberUtilityVars.isEmpty()) {
            model.addLessOrEqual(LinearExpr.sum(memberUtilityVars.toArray(new Literal[0])), 1);
          }
        }
      }
    }

    for (EventStrategy strategy : strategies) {
      List<PrimarySlotSpec> primarySlots = primarySlotsByStrategy.get(strategy.getKey());
      if (primarySlots == null) {
        continue;
      }
      int maxTeams = assignmentCapacity(members) / primarySlots.size();
      for (int t = 0; t + 1 < maxTeams; t++) {
        model.addGreaterOrEqual(y.get(key(strategy.getKey(), t)), y.get(key(strategy.getKey(), t + 1)));
      }
    }

    List<Literal> teamVars = y.values().stream().map(var -> (Literal) var).toList();
    model.maximize(LinearExpr.sum(teamVars.toArray(new Literal[0])));

    CpSolver solver = new CpSolver();
    solver.getParameters().setMaxTimeInSeconds(SOLVER_TIME_LIMIT_SECONDS);
    solver.getParameters().setNumSearchWorkers(2);

    CpSolverStatus status = solver.solve(model);
    if (!isSolvable(status)) {
      log.warnf("OR-Tools phase 1 failed with status=%s, falling back to greedy", status);
      return greedyFallback.generateTeams(event, members);
    }

    long bestTeamCount = Math.round(solver.objectiveValue());
    model.addEquality(LinearExpr.sum(teamVars.toArray(new Literal[0])), bestTeamCount);

    BoolVar hosterVar = selectedByMember.get(event.getHoster() == null ? null : event.getHoster().getUserId());
    if (hosterSignedUp && hosterVar != null) {
      model.maximize(hosterVar);
      status = solver.solve(model);
      if (!isSolvable(status)) {
        log.warnf("OR-Tools phase 2 failed with status=%s, falling back to greedy", status);
        return greedyFallback.generateTeams(event, members);
      }
      model.addEquality(hosterVar, Math.round(solver.objectiveValue()));
    }

    if (members.stream().anyMatch(EventMemberSignup::wantsDualBox)) {
      // Prefer giving another player a place before using a dual-box player's second slot.
      Literal[] selectedVars = selectedByMember.values().toArray(new Literal[0]);
      model.maximize(LinearExpr.sum(selectedVars));
      status = solver.solve(model);
      if (!isSolvable(status)) {
        log.warnf("OR-Tools player coverage phase failed with status=%s, falling back to greedy", status);
        return greedyFallback.generateTeams(event, members);
      }
      model.addEquality(LinearExpr.sum(selectedVars), Math.round(solver.objectiveValue()));
    }

    List<Literal> fairnessVars = new ArrayList<>();
    List<Long> fairnessWeights = new ArrayList<>();
    for (EventMemberSignup signup : members) {
      String memberId = signup.getGuildMember().getUserId();
      long priority = signup.getEffectivePriority();
      if (priority == 0L) {
        continue;
      }

      BoolVar selected = selectedByMember.get(memberId);
      if (selected != null) {
        fairnessVars.add(selected);
        fairnessWeights.add(priority);
      }
    }

    if (!fairnessVars.isEmpty()) {
      LinearExpr fairnessScore = LinearExpr.weightedSum(
          fairnessVars.toArray(new com.google.ortools.sat.LinearArgument[0]),
          fairnessWeights.stream().mapToLong(Long::longValue).toArray()
      );
      model.maximize(fairnessScore);
      status = solver.solve(model);
      if (!isSolvable(status)) {
        log.warnf("OR-Tools phase 3 failed with status=%s, falling back to greedy", status);
        return greedyFallback.generateTeams(event, members);
      }
      model.addEquality(fairnessScore, Math.round(solver.objectiveValue()));
    }

    List<Literal> pairedBoxes = new ArrayList<>();
    for (EventMemberSignup signup : members) {
      if (!signup.wantsDualBox()) {
        continue;
      }
      for (EventStrategy strategy : strategies) {
        List<PrimarySlotSpec> primarySlots = primarySlotsByStrategy.get(strategy.getKey());
        if (primarySlots == null) {
          continue;
        }
        int maxTeams = assignmentCapacity(members) / primarySlots.size();
        for (int t = 0; t < maxTeams; t++) {
          List<Literal> teamAssignments = new ArrayList<>();
          for (PrimarySlotSpec primary : primarySlots) {
            BoolVar assignment = x.get(key(signup.getGuildMember().getUserId(), strategy.getKey(), t, primary.variableKey()));
            if (assignment != null) {
              teamAssignments.add(assignment);
            }
          }
          if (teamAssignments.size() < 2) {
            continue;
          }
          BoolVar paired = model.newBoolVar("paired_" + signup.getGuildMember().getUserId() + "_" + strategy.getKey() + "_" + t);
          LinearExpr count = LinearExpr.sum(teamAssignments.toArray(new Literal[0]));
          model.addEquality(count, 2).onlyEnforceIf(paired);
          model.addLessOrEqual(count, 1).onlyEnforceIf(paired.not());
          pairedBoxes.add(paired);
        }
      }
    }
    if (!pairedBoxes.isEmpty()) {
      model.maximize(LinearExpr.sum(pairedBoxes.toArray(new Literal[0])));
      status = solver.solve(model);
      if (!isSolvable(status)) {
        log.warnf("OR-Tools dual-box grouping phase failed with status=%s, falling back to greedy", status);
        return greedyFallback.generateTeams(event, members);
      }
    }

    List<EventTeam> teams = buildTeams(event, strategies, primarySlotsByStrategy, utilitySlotsByStrategy, x, z, solver, members, y);
    Set<String> usedPrimaryMembers = collectUsedPrimaryMembers(strategies, primarySlotsByStrategy, x, solver, members);
    List<EventMemberSignup> unmatchedSignups = members.stream()
        .filter(signup -> !usedPrimaryMembers.contains(signup.getGuildMember().getUserId()))
        .toList();

    log.infof("OR-Tools team generation finished: teams=%d, unmatched=%d, time=%s", teams.size(), unmatchedSignups.size(), solver.wallTime());
    return new TeamGeneratorResult(List.copyOf(teams), List.copyOf(unmatchedSignups));
  }

  private boolean isSolvable(CpSolverStatus status) {
    return status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE;
  }

  private List<EventTeam> buildTeams(
      Event event,
      List<EventStrategy> strategies,
      Map<String, List<PrimarySlotSpec>> primarySlotsByStrategy,
      Map<String, List<UtilitySlotSpec>> utilitySlotsByStrategy,
      Map<String, BoolVar> x,
      Map<String, BoolVar> z,
      CpSolver solver,
      List<EventMemberSignup> members,
      Map<String, BoolVar> y
  ) {
    Map<String, EventMemberSignup> memberById = members.stream()
        .collect(Collectors.toMap(signup -> signup.getGuildMember().getUserId(), signup -> signup, (left, right) -> left, LinkedHashMap::new));

    List<EventTeam> teams = new ArrayList<>();
    for (EventStrategy strategy : strategies) {
      List<PrimarySlotSpec> primarySlots = primarySlotsByStrategy.get(strategy.getKey());
      List<UtilitySlotSpec> utilitySlots = utilitySlotsByStrategy.get(strategy.getKey());
      if (primarySlots == null) {
        continue;
      }

      int maxTeams = assignmentCapacity(members) / primarySlots.size();
      for (int t = 0; t < maxTeams; t++) {
        BoolVar teamActive = y.get(key(strategy.getKey(), t));
        if (teamActive == null || solver.booleanValue(teamActive) == false) {
          continue;
        }

        EventTeam team = new EventTeam(strategy);

        for (PrimarySlotSpec primarySlot : primarySlots) {
          for (Map.Entry<String, EventMemberSignup> entry : memberById.entrySet()) {
            BoolVar var = x.get(key(entry.getKey(), strategy.getKey(), t, primarySlot.variableKey()));
            if (var != null && solver.booleanValue(var)) {
              team.assign(primarySlot.roleKey(), entry.getValue().getGuildMember());
              break;
            }
          }
        }

        for (UtilitySlotSpec utilitySlot : utilitySlots) {
          for (Map.Entry<String, EventMemberSignup> entry : memberById.entrySet()) {
            BoolVar var = z.get(key(entry.getKey(), strategy.getKey(), t, utilitySlot.variableKey()));
            if (var != null && solver.booleanValue(var)) {
              team.assignUtility(utilitySlot.roleKey(), entry.getValue().getGuildMember());
              break;
            }
          }
        }

        teams.add(team);
      }
    }

    return teams;
  }

  private Set<String> collectUsedPrimaryMembers(
      List<EventStrategy> strategies,
      Map<String, List<PrimarySlotSpec>> primarySlotsByStrategy,
      Map<String, BoolVar> x,
      CpSolver solver,
      List<EventMemberSignup> members
  ) {
    Set<String> usedMembers = new LinkedHashSet<>();
    for (EventStrategy strategy : strategies) {
      List<PrimarySlotSpec> primarySlots = primarySlotsByStrategy.get(strategy.getKey());
      if (primarySlots == null) {
        continue;
      }

      int maxTeams = assignmentCapacity(members) / primarySlots.size();
      for (int t = 0; t < maxTeams; t++) {
        for (EventMemberSignup signup : members) {
          String memberId = signup.getGuildMember().getUserId();
          for (PrimarySlotSpec primarySlot : primarySlots) {
            BoolVar var = x.get(key(memberId, strategy.getKey(), t, primarySlot.variableKey()));
            if (var != null && solver.booleanValue(var)) {
              usedMembers.add(memberId);
            }
          }
        }
      }
    }
    return usedMembers;
  }

  private int assignmentCapacity(List<EventMemberSignup> members) {
    return members.stream().mapToInt(EventMemberSignup::getMaxAssignments).sum();
  }

  private List<PrimarySlotSpec> expandPrimarySlots(EventStrategy strategy) {
    List<PrimarySlotSpec> slots = new ArrayList<>();
    for (EventRole role : strategy.getPrimaryRoles()) {
      for (int i = 0; i < role.getRequiredCount(); i++) {
        slots.add(new PrimarySlotSpec(role.getKey(), role.getName(), role.getEmojiSlug(), i));
      }
    }
    return slots;
  }

  private List<UtilitySlotSpec> expandUtilitySlots(EventStrategy strategy) {
    List<UtilitySlotSpec> slots = new ArrayList<>();
    for (EventUtilityRole role : strategy.getUtilityRoles()) {
      for (int i = 0; i < role.getRequiredCount(); i++) {
        slots.add(new UtilitySlotSpec(role.getKey(), role.getName(), role.getEmojiSlug(), role.getEligiblePrimaryRoleKeys(), i));
      }
    }
    return slots;
  }

  private static String key(Object... parts) {
    return Arrays.stream(parts).map(String::valueOf).collect(Collectors.joining("|"));
  }

  private static boolean sameMember(GuildMember left, GuildMember right) {
    return left != null
        && right != null
        && left.getUserId().equals(right.getUserId());
  }

  private record PrimarySlotSpec(String roleKey, String roleName, String emojiSlug, int copyIndex) {
    private String variableKey() {
      return roleKey + "#" + copyIndex;
    }
  }

  private record UtilitySlotSpec(
      String roleKey,
      String roleName,
      String emojiSlug,
      Set<String> eligiblePrimaryRoleKeys,
      int copyIndex
  ) {
    private String variableKey() {
      return roleKey + "#" + copyIndex;
    }
  }
}
