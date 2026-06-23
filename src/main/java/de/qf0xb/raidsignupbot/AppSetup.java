package de.qf0xb.raidsignupbot;

import de.qf0xb.raidsignupbot.application.TeamGeneratorResult;
import de.qf0xb.raidsignupbot.application.dto.CreateEventCommand;
import de.qf0xb.raidsignupbot.application.dto.CreateGuildCommand;
import de.qf0xb.raidsignupbot.application.port.EventUIApplicationPort;
import de.qf0xb.raidsignupbot.application.service.event.EventService;
import de.qf0xb.raidsignupbot.application.service.generator.OrToolsTeamGeneratorService;
import de.qf0xb.raidsignupbot.application.service.guild.GuildMemberService;
import de.qf0xb.raidsignupbot.application.service.guild.GuildService;
import de.qf0xb.raidsignupbot.application.service.template.EventStrategyService;
import de.qf0xb.raidsignupbot.application.service.template.TemplateService;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventMemberSignup;
import de.qf0xb.raidsignupbot.domain.model.event.EventSlot;
import de.qf0xb.raidsignupbot.domain.model.event.EventTeam;
import de.qf0xb.raidsignupbot.domain.model.event.EventUtilityAssignment;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.DiscordBot;
import jakarta.annotation.Priority;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Slf4j
@ApplicationScoped
//@IfBuildProfile("demo")
public class AppSetup {
  private static final Logger LOG = Logger.getLogger(AppSetup.class);

  private final OrToolsTeamGeneratorService teamGeneratorService;
  private final TemplateService templateService;
  private final EventStrategyService strategyService;
  private final EventService eventService;
  private final GuildService guildService;
  private final GuildMemberService guildMemberService;
  private final EventUIApplicationPort eventUIApplicationPort;
  private final DiscordBot discordBot;

  public AppSetup(
      OrToolsTeamGeneratorService teamGeneratorService,
      TemplateService templateService,
      EventStrategyService strategyService,
      EventService eventService,
      GuildService guildService,
      GuildMemberService guildMemberService,
      EventUIApplicationPort eventUIApplicationPort,
      DiscordBot discordBot) {
    this.teamGeneratorService = teamGeneratorService;
    this.templateService = templateService;
    this.strategyService = strategyService;
    this.eventService = eventService;
    this.guildService = guildService;
    this.guildMemberService = guildMemberService;
    this.eventUIApplicationPort = eventUIApplicationPort;
    this.discordBot = discordBot;
  }

  @Transactional
  void onStart(@Observes @Priority(Integer.MAX_VALUE) StartupEvent ev) {
    LOG.info("Application initialized...");

    Guild guild = createGuild();
    createTemplate();
    GuildMember hoster = guildMemberService.createGuildMember("480285761783201823", guild.getGuildId());

    Event event = eventService.createEvent(
        CreateEventCommand.builder()
            .guildId(guild.getGuildId())
            .hosterUserId(hoster.getUserId())
            .start(Instant.now())
            .end(Instant.now().plus(2, ChronoUnit.HOURS))
            .templateKey("large-test")
            .categoryId("1519040438789472337")
            .build());

    log.info(event.getCategoryId());

    if (discordBot.isReady()) {
      eventUIApplicationPort.addEvent(event);
    } else {
      LOG.warn("Skipping Discord event UI setup because the Discord API is not initialized.");
    }

    List<EventMemberSignup> signups = List.of(
        // Owner should be selected if possible.
        new EventMemberSignup(hoster, "one", "two", "three", "four", "a", "b", "c", "d", "hex"),

        // Storm-only constrained players.
        signup(guild.getGuildId(), "storm_four_01", "four"),
        signup(guild.getGuildId(), "storm_four_02", "four"),
        signup(guild.getGuildId(), "storm_four_03", "four"),
        signup(guild.getGuildId(), "storm_one_01", "one"),
        signup(guild.getGuildId(), "storm_two_01", "two"),
        signup(guild.getGuildId(), "storm_three_01", "three"),

        // Myth-only constrained players.
        signup(guild.getGuildId(), "myth_d_01", "d"),
        signup(guild.getGuildId(), "myth_d_02", "d"),
        signup(guild.getGuildId(), "myth_d_03", "d"),
        signup(guild.getGuildId(), "myth_a_hex_01", "a", "hex"),
        signup(guild.getGuildId(), "myth_b_hex_01", "b", "hex"),
        signup(guild.getGuildId(), "myth_c_hex_01", "c", "hex"),

        // Flexible Storm/Myth players.
        signup(guild.getGuildId(), "flex_01", "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        signup(guild.getGuildId(), "flex_02", "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        signup(guild.getGuildId(), "flex_03", "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        signup(guild.getGuildId(), "flex_04", "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        signup(guild.getGuildId(), "flex_05", "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        signup(guild.getGuildId(), "flex_06", "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        signup(guild.getGuildId(), "flex_07", "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        signup(guild.getGuildId(), "flex_08", "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        signup(guild.getGuildId(), "flex_09", "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        signup(guild.getGuildId(), "flex_10", "one", "two", "three", "four", "a", "b", "c", "d", "hex"),

        // Semi-flexible players.
        signup(guild.getGuildId(), "semi_01", "one", "two", "three", "a", "b", "c"),
        signup(guild.getGuildId(), "semi_02", "one", "two", "three", "a", "b", "c"),
        signup(guild.getGuildId(), "semi_03", "one", "two", "three", "a", "b", "c"),
        signup(guild.getGuildId(), "semi_04", "one", "two", "three", "a", "b", "c"),
        signup(guild.getGuildId(), "semi_05", "one", "two", "three", "a", "b", "c"),
        signup(guild.getGuildId(), "semi_06", "one", "two", "three", "a", "b", "c"),

        // Bad utility edge case: has hex but no eligible primary role.
        signup(guild.getGuildId(), "hex_only_invalid", "hex"));

    LOG.info("Starting to generate teams...");
    Event result = eventService.generateTeamsAndCloseSignup(event.getId(), signups);
    LOG.info("Finished generating teams.");
    int count = 0;
    for (EventTeam team : result.getTeams()) {
      count++;
      LOG.info("Team " + count + " (" + team.getStrategy().getName() + "): ");

      int slotCount = 0;
      for (EventSlot slot : team.getSlots()) {
        slotCount++;
        LOG.info(String.format("Slot %s (%s): %s", slotCount, slot.getRoleName(), slot.getMember().getUserId()));
      }
      for (EventUtilityAssignment utilityAssignment : team.getUtilityAssignments()) {
        LOG.info(String.format(
            "Utility (%s): %s",
            utilityAssignment.getRoleName(),
            utilityAssignment.getMember().getUserId()));
      }
    }
  }

  private Guild createGuild() {
    return guildService.createGuild(
        CreateGuildCommand.builder()
            .guildId("oc1")
            .name("Onyx Cats")
            .raiderRoleId("theste")
            .build());
  }

  private void createTemplate() {
    templateService.createTemplate("large-test", "Takanobu", "Stress test", "coin");

    strategyService.addStrategy("large-test", "storm", "Storm Strategy");
    strategyService.addPrimaryRole("large-test", "storm", "one", "Potent Feint", ":one:", 1);
    strategyService.addPrimaryRole(
        "large-test",
        "storm",
        "two",
        "Sharpened Elemental Blade",
        ":two:",
        1);
    strategyService.addPrimaryRole(
        "large-test",
        "storm",
        "three",
        "IC Sharpened Elemental Blade",
        ":three:",
        1);
    strategyService.addPrimaryRole("large-test", "storm", "four", "Epic Bunyip", ":four:", 1);

    strategyService.addStrategy("large-test", "myth", "Myth Strategy");
    strategyService.addPrimaryRole("large-test", "myth", "a", "Potent Feint", ":regional_indicator_a:", 1);
    strategyService.addPrimaryRole(
        "large-test",
        "myth",
        "b",
        "Sharpened Spirit Blade",
        ":regional_indicator_b:",
        1);
    strategyService.addPrimaryRole(
        "large-test",
        "myth",
        "c",
        "IC Sharpened Spirit Blade",
        ":regional_indicator_c:",
        1);
    strategyService.addPrimaryRole("large-test", "myth", "d", "Epic Drop Bear", ":regional_indicator_d:", 1);
    strategyService.addUtilityRole(
        "large-test",
        "myth",
        "hex",
        "Hex",
        ":schoolSun_1:",
        1,
        Set.of("a", "b", "c"));
  }

  private EventMemberSignup signup(String guildId, String userId, String... roleKeys) {
    return new EventMemberSignup(guildMemberService.createGuildMember(userId, guildId), roleKeys);
  }

}
