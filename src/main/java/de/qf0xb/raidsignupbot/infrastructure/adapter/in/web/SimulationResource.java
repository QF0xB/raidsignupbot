package de.qf0xb.raidsignupbot.infrastructure.adapter.in.web;

import de.qf0xb.raidsignupbot.application.service.generator.TeamSimulationService;
import de.qf0xb.raidsignupbot.application.service.template.TemplateService;
import de.qf0xb.raidsignupbot.domain.model.event.EventMemberSignup;
import de.qf0xb.raidsignupbot.domain.model.event.EventSlot;
import de.qf0xb.raidsignupbot.domain.model.event.EventTeam;
import de.qf0xb.raidsignupbot.domain.model.event.EventUtilityAssignment;
import de.qf0xb.raidsignupbot.domain.model.guild.SimulatedPlayer;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/** Previews the teams a template produces for many made-up signups. Nothing is saved or sent to Discord. */
@Path("/servers/{serverId}/templates/{key}/simulate")
@Authenticated
@Blocking
public class SimulationResource {
  @CheckedTemplate
  static class Templates {
    static native TemplateInstance simulation(
        String userName, WebAccessService.ServerAccess server, EventTemplate template, Form form, Report report,
        String error);
  }

  /** The submitted settings, echoed back into the form. */
  public record Form(int players, int minRoles, int maxRoles, int utilityPercent, int backupPercent,
      int dualBoxPercent, boolean randomPriority, long seed, int maxPlayers) {}

  public record Report(
      EventView teams, List<RoleDemand> roles, List<Player> unassigned, int players, int assignedPlayers,
      int filledSlots, int openSlots, int dualBoxers, int backupsPlaced, long millis) {}

  /** How many simulated players reacted with a role, and how many of its slots were filled. */
  public record RoleDemand(String name, String kind, int signups, int slots, int filled) {
    public int missing() {
      return slots - filled;
    }
  }

  public record Player(String name, String roles) {}

  private final CurrentUser currentUser;
  private final WebAccessService accessService;
  private final TemplateService templateService;
  private final TeamSimulationService simulationService;
  private final EventViewFactory eventViewFactory;

  public SimulationResource(
      CurrentUser currentUser,
      WebAccessService accessService,
      TemplateService templateService,
      TeamSimulationService simulationService,
      EventViewFactory eventViewFactory) {
    this.currentUser = currentUser;
    this.accessService = accessService;
    this.templateService = templateService;
    this.simulationService = simulationService;
    this.eventViewFactory = eventViewFactory;
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  public TemplateInstance simulate(
      @PathParam("serverId") String serverId,
      @PathParam("key") String key,
      @QueryParam("run") boolean run,
      @QueryParam("players") @DefaultValue("40") int players,
      @QueryParam("minRoles") @DefaultValue("1") int minRoles,
      @QueryParam("maxRoles") @DefaultValue("3") int maxRoles,
      @QueryParam("utilityPercent") @DefaultValue("30") int utilityPercent,
      @QueryParam("backupPercent") @DefaultValue("10") int backupPercent,
      @QueryParam("dualBoxPercent") @DefaultValue("5") int dualBoxPercent,
      @QueryParam("randomPriority") boolean randomPriority,
      @QueryParam("seed") Long seed) {
    WebAccessService.ServerAccess access = accessService.serverAccess(serverId, currentUser.id());
    access.require(access.manageTemplates() || access.manageEvents());
    EventTemplate template = templateService.findTemplateByKey(serverId, key);
    long usedSeed = seed == null ? ThreadLocalRandom.current().nextLong(1, 1_000_000) : seed;
    Form form = new Form(players, minRoles, maxRoles, utilityPercent, backupPercent, dualBoxPercent, randomPriority,
        usedSeed, TeamSimulationService.MAX_PLAYERS);
    if (!run) {
      return Templates.simulation(currentUser.displayName(), access, template, form, null, null);
    }

    try {
      TeamSimulationService.Simulation simulation = simulationService.simulate(template,
          new TeamSimulationService.Settings(players, minRoles, maxRoles, utilityPercent, backupPercent,
              dualBoxPercent, randomPriority, usedSeed));
      return Templates.simulation(currentUser.displayName(), access, template, form, report(template, simulation), null);
    } catch (IllegalArgumentException ex) {
      return Templates.simulation(currentUser.displayName(), access, template, form, null, ex.getMessage());
    }
  }

  private Report report(EventTemplate template, TeamSimulationService.Simulation simulation) {
    List<EventTeam> teams = simulation.result().teams();
    List<EventSlot> slots = teams.stream().flatMap(team -> team.getSlots().stream()).toList();
    List<EventUtilityAssignment> utilities = teams.stream().flatMap(team -> team.getUtilityAssignments().stream()).toList();
    Map<String, EventMemberSignup> signupsByPlayer = simulation.signups().stream()
        .collect(Collectors.toMap(signup -> signup.getGuildMember().getUserId(), signup -> signup));

    Map<String, Long> slotsPerPlayer = slots.stream().filter(EventSlot::isAssigned)
        .collect(Collectors.groupingBy(slot -> slot.getMember().getUserId(), Collectors.counting()));
    long backupsPlaced = slotsPerPlayer.keySet().stream()
        .filter(player -> signupsByPlayer.get(player).wantsBackup()).count();

    Map<String, String> roleNames = new HashMap<>();
    for (EventStrategy strategy : template.getStrategies()) {
      strategy.getPrimaryRoles().forEach(role -> roleNames.putIfAbsent(role.getKey(), role.getName()));
      strategy.getUtilityRoles().forEach(role -> roleNames.putIfAbsent(role.getKey(), role.getName()));
    }

    Map<String, RoleDemand> roles = new LinkedHashMap<>();
    for (EventStrategy strategy : template.getStrategies()) {
      for (EventRole role : strategy.getPrimaryRoles()) {
        roles.computeIfAbsent(role.getKey(), roleKey -> new RoleDemand(role.getName(), "primary",
            signups(simulation, roleKey),
            (int) slots.stream().filter(slot -> slot.matches(roleKey)).count(),
            (int) slots.stream().filter(slot -> slot.matches(roleKey) && slot.isAssigned()).count()));
      }
      for (EventRole role : strategy.getUtilityRoles()) {
        roles.computeIfAbsent(role.getKey(), roleKey -> new RoleDemand(role.getName(), "utility",
            signups(simulation, roleKey),
            (int) utilities.stream().filter(utility -> utility.matches(roleKey)).count(),
            (int) utilities.stream().filter(utility -> utility.matches(roleKey) && utility.isAssigned()).count()));
      }
    }

    List<Player> unassigned = new ArrayList<>();
    for (EventMemberSignup signup : simulation.signups()) {
      if (!slotsPerPlayer.containsKey(signup.getGuildMember().getUserId())) {
        unassigned.add(new Player(SimulatedPlayer.displayName(signup.getGuildMember().getUserId()), signup.getRoleKeys().stream()
            .map(roleKey -> switch (roleKey) {
              case EventMemberSignup.BACKUP_ROLE_KEY -> EventMemberSignup.BACKUP_EMOJI + " backup";
              case EventMemberSignup.DUAL_BOX_ROLE_KEY -> EventMemberSignup.DUAL_BOX_EMOJI + " dual box";
              default -> roleNames.getOrDefault(roleKey, roleKey);
            })
            .sorted()
            .collect(Collectors.joining(", "))));
      }
    }

    return new Report(
        eventViewFactory.create(simulation.event()),
        List.copyOf(roles.values()),
        unassigned,
        simulation.signups().size(),
        slotsPerPlayer.size(),
        (int) slots.stream().filter(EventSlot::isAssigned).count(),
        (int) slots.stream().filter(slot -> !slot.isAssigned()).count(),
        (int) slotsPerPlayer.values().stream().filter(count -> count > 1).count(),
        (int) backupsPlaced,
        simulation.took().toMillis());
  }

  private static int signups(TeamSimulationService.Simulation simulation, String roleKey) {
    return (int) simulation.signups().stream().filter(signup -> signup.wantsRole(roleKey)).count();
  }
}
