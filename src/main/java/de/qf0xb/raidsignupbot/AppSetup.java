package de.qf0xb.raidsignupbot;

import de.qf0xb.raidsignupbot.application.dto.TeamGeneratorResult;
import de.qf0xb.raidsignupbot.application.service.OrToolsTeamGeneratorService;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventMemberSignup;
import de.qf0xb.raidsignupbot.domain.model.event.EventSlot;
import de.qf0xb.raidsignupbot.domain.model.event.EventUtilityAssignment;
import de.qf0xb.raidsignupbot.domain.model.event.EventTeam;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import de.qf0xb.raidsignupbot.domain.model.template.EventUtilityRole;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class AppSetup {
  private final Logger logger;
  private final OrToolsTeamGeneratorService teamGeneratorService;

  @Inject
  public AppSetup(Logger logger, OrToolsTeamGeneratorService teamGeneratorService) {
    this.logger = logger;
    this.teamGeneratorService = teamGeneratorService;
  }

  void onStart(@Observes StartupEvent ev) {
    logger.info("Application initialized...");

    EventTemplate template = new EventTemplate("large-test", "Large Test Raid", "Stress test");

    EventStrategy storm = new EventStrategy("storm", "Storm Strategy");
    storm.addRole(new EventRole("one", "Potent Feint", ":one:", 1));
    storm.addRole(new EventRole("two", "Sharpened Elemental Blade", ":two:", 1));
    storm.addRole(new EventRole("three", "IC Sharpened Elemental Blade", ":three:", 1));
    storm.addRole(new EventRole("four", "Epic Bunyip", ":four:", 1));
    template.addStrategy(storm);

    EventStrategy myth = new EventStrategy("myth", "Myth Strategy");
    myth.addRole(new EventRole("a", "Potent Feint", ":regional_indicator_a:", 1));
    myth.addRole(new EventRole("b", "Sharpened Spirit Blade", ":regional_indicator_b:", 1));
    myth.addRole(new EventRole("c", "IC Sharpened Spirit Blade", ":regional_indicator_c:", 1));
    myth.addRole(new EventRole("d", "Epic Drop Bear", ":regional_indicator_d:", 1));

    EventUtilityRole hex = new EventUtilityRole("hex", "Hex", ":schoolSun-1:", 1);
    hex.addEligiblePrimaryRole("a");
    hex.addEligiblePrimaryRole("b");
    hex.addEligiblePrimaryRole("c");
    myth.addRole(hex);
    template.addStrategy(myth);

    GuildMember hoster = new GuildMember("owner");

    Event event = Event.builder()
        .id(null)
        .start(Instant.parse("2026-01-01T18:00:00Z"))
        .end(Instant.parse("2026-01-01T19:00:00Z"))
        .hoster(hoster)
        .template(template)
        .build();

    List<EventMemberSignup> signups = List.of(
        // Owner should be selected if possible.
        new EventMemberSignup(hoster, "one", "two", "three", "four", "a", "b", "c", "d", "hex"),

        // Storm-only constrained players.
        new EventMemberSignup(new GuildMember("storm_four_01"), "four"),
        new EventMemberSignup(new GuildMember("storm_four_02"), "four"),
        new EventMemberSignup(new GuildMember("storm_four_03"), "four"),
        new EventMemberSignup(new GuildMember("storm_one_01"), "one"),
        new EventMemberSignup(new GuildMember("storm_two_01"), "two"),
        new EventMemberSignup(new GuildMember("storm_three_01"), "three"),

        // Myth-only constrained players.
        new EventMemberSignup(new GuildMember("myth_d_01"), "d"),
        new EventMemberSignup(new GuildMember("myth_d_02"), "d"),
        new EventMemberSignup(new GuildMember("myth_d_03"), "d"),
        new EventMemberSignup(new GuildMember("myth_a_hex_01"), "a", "hex"),
        new EventMemberSignup(new GuildMember("myth_b_hex_01"), "b", "hex"),
        new EventMemberSignup(new GuildMember("myth_c_hex_01"), "c", "hex"),

        // Flexible Storm/Myth players.
        new EventMemberSignup(new GuildMember("flex_01"), "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        new EventMemberSignup(new GuildMember("flex_02"), "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        new EventMemberSignup(new GuildMember("flex_03"), "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        new EventMemberSignup(new GuildMember("flex_04"), "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        new EventMemberSignup(new GuildMember("flex_05"), "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        new EventMemberSignup(new GuildMember("flex_06"), "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        new EventMemberSignup(new GuildMember("flex_07"), "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        new EventMemberSignup(new GuildMember("flex_08"), "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        new EventMemberSignup(new GuildMember("flex_09"), "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        new EventMemberSignup(new GuildMember("flex_10"), "one", "two", "three", "four", "a", "b", "c", "d", "hex"),

        // Semi-flexible players.
        new EventMemberSignup(new GuildMember("semi_01"), "one", "two", "three", "a", "b", "c"),
        new EventMemberSignup(new GuildMember("semi_02"), "one", "two", "three", "a", "b", "c"),
        new EventMemberSignup(new GuildMember("semi_03"), "one", "two", "three", "a", "b", "c"),
        new EventMemberSignup(new GuildMember("semi_04"), "one", "two", "three", "a", "b", "c"),
        new EventMemberSignup(new GuildMember("semi_05"), "one", "two", "three", "a", "b", "c"),
        new EventMemberSignup(new GuildMember("semi_06"), "one", "two", "three", "a", "b", "c"),

        // Bad utility edge case: has hex but no eligible primary role.
        new EventMemberSignup(new GuildMember("hex_only_invalid"), "hex")
    );

    logger.info("Starting to generate teams...");
    TeamGeneratorResult result = teamGeneratorService.generateTeams(event, signups);
    logger.info("Finished generating teams.");
    int count = 0;
    for (EventTeam t : result.teams()) {
      count++;
      logger.info("Team " + count + " (" + t.getStrategy().getName() + "): ");

      int sCount = 0;
      for (EventSlot s : t.getSlots()) {
        sCount++;
        logger.info(String.format("Slot %s (%s): %s", sCount, s.getRoleName(), s.getMember().getUserId()));
      }
      for (EventUtilityAssignment utilityAssignment : t.getUtilityAssignments()) {
        logger.info(String.format(
            "Utility (%s): %s",
            utilityAssignment.getRoleName(),
            utilityAssignment.getMember().getUserId()
        ));
      }
    }
    result.unmatchedSignups().forEach(signup -> logger.info(signup.getGuildMember().getUserId()));

  }
}
