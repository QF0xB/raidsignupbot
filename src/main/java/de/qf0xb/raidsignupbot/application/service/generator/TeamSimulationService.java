package de.qf0xb.raidsignupbot.application.service.generator;

import de.qf0xb.raidsignupbot.application.TeamGeneratorResult;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventMemberSignup;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.guild.SimulatedPlayer;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import de.qf0xb.raidsignupbot.domain.model.template.EventUtilityRole;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Runs the real team generator on made-up signups for a template, without saving anything or
 * talking to Discord. Lets managers check how a template behaves with many players.
 */
@ApplicationScoped
public class TeamSimulationService {
  public static final int MAX_PLAYERS = 300;

  /** Percentages are per player; the seed makes a run repeatable. */
  public record Settings(
      int players, int minRoles, int maxRoles, int utilityPercent, int backupPercent, int dualBoxPercent,
      boolean randomPriority, long seed) {
    public Settings {
      if (players < 1 || players > MAX_PLAYERS) {
        throw new IllegalArgumentException("Simulate between 1 and " + MAX_PLAYERS + " players.");
      }
      if (minRoles < 1 || maxRoles < minRoles) {
        throw new IllegalArgumentException("Roles per player need a minimum of at least 1 and a maximum not below it.");
      }
      for (int percent : new int[] {utilityPercent, backupPercent, dualBoxPercent}) {
        if (percent < 0 || percent > 100) {
          throw new IllegalArgumentException("Percentages must be between 0 and 100.");
        }
      }
    }
  }

  public record Simulation(Event event, List<EventMemberSignup> signups, TeamGeneratorResult result, Duration took) {}

  // A fresh generator per run, like EventService uses, so simulations never share solver state.
  public Simulation simulate(EventTemplate template, Settings settings) {
    List<EventMemberSignup> signups = createSignups(template, settings);
    Event event = Event.builder()
        .guild(new Guild("Simulation", template.getServerId()))
        .hoster(signups.getFirst().getGuildMember())
        .template(template)
        .teamsGenerated(true)
        .signupOpen(false)
        .build();
    long started = System.nanoTime();
    TeamGeneratorResult result = new OrToolsTeamGeneratorService().generateTeams(event, signups);
    Duration took = Duration.ofNanos(System.nanoTime() - started);
    result.teams().forEach(event::addTeam);
    return new Simulation(event, signups, result, took);
  }

  /** Made-up signups for the template's roles, with {@link SimulatedPlayer} IDs. */
  public List<EventMemberSignup> createSignups(EventTemplate template, Settings settings) {
    if (template.getStrategies().isEmpty()) {
      throw new IllegalArgumentException("The template has no strategies yet.");
    }
    Random random = new Random(settings.seed());
    List<EventRole> primaryRoles = distinctRoles(template, false);
    List<EventRole> utilityRoles = distinctRoles(template, true);
    if (primaryRoles.isEmpty()) {
      throw new IllegalArgumentException("The template has no primary roles yet.");
    }

    List<EventMemberSignup> signups = new ArrayList<>();
    for (int number = 1; number <= settings.players(); number++) {
      GuildMember member = new GuildMember(SimulatedPlayer.userId(number));
      if (settings.randomPriority()) {
        member.setPriority(random.nextInt(6));
      }
      EventMemberSignup signup = new EventMemberSignup(member);
      List<EventRole> shuffled = new ArrayList<>(primaryRoles);
      Collections.shuffle(shuffled, random);
      int roleCount = Math.min(shuffled.size(),
          settings.minRoles() + random.nextInt(settings.maxRoles() - settings.minRoles() + 1));
      shuffled.subList(0, roleCount).forEach(signup::addRole);
      for (EventRole utility : utilityRoles) {
        boolean usable = ((EventUtilityRole) utility).getEligiblePrimaryRoleKeys().stream().anyMatch(signup::wantsRole);
        if (usable && chance(random, settings.utilityPercent())) {
          signup.addRole(utility);
        }
      }
      if (chance(random, settings.backupPercent())) {
        signup.addRoleKey(EventMemberSignup.BACKUP_ROLE_KEY);
      }
      if (chance(random, settings.dualBoxPercent())) {
        signup.addRoleKey(EventMemberSignup.DUAL_BOX_ROLE_KEY);
      }
      signups.add(signup);
    }
    return signups;
  }

  private static List<EventRole> distinctRoles(EventTemplate template, boolean utility) {
    Map<String, EventRole> roles = new LinkedHashMap<>();
    for (EventStrategy strategy : template.getStrategies()) {
      (utility ? strategy.getUtilityRoles() : strategy.getPrimaryRoles())
          .forEach(role -> roles.putIfAbsent(role.getKey(), role));
    }
    return List.copyOf(roles.values());
  }

  private static boolean chance(Random random, int percent) {
    return random.nextInt(100) < percent;
  }
}
