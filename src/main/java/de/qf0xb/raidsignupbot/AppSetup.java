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
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.DiscordBot;
import jakarta.annotation.Priority;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Vetoed;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Slf4j
@ApplicationScoped
@IfBuildProfile("dev")
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

  @Getter
  private List<EventMemberSignup> signups;

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
    GuildMember hoster = guildMemberService.createGuildMember("480285761783201823", guild.getName());

    signups = List.of(
        // Owner should be selected if possible.
        new EventMemberSignup(hoster, "one", "two", "three", "four", "a", "b", "c", "d", "hex"),

        // Storm-only constrained players.
        signup(guild.getName(), "storm_four_01", "four"),
        signup(guild.getName(), "storm_four_02", "four"),
        signup(guild.getName(), "storm_four_03", "four"),
        signup(guild.getName(), "storm_one_01", "one"),
        signup(guild.getName(), "storm_two_01", "two"),
        signup(guild.getName(), "storm_three_01", "three"),

        // Myth-only constrained players.
        signup(guild.getName(), "myth_d_01", "d"),
        signup(guild.getName(), "myth_d_02", "d"),
        signup(guild.getName(), "myth_d_03", "d"),
        signup(guild.getName(), "myth_a_hex_01", "a", "hex"),
        signup(guild.getName(), "myth_b_hex_01", "b", "hex"),
        signup(guild.getName(), "myth_c_hex_01", "c", "hex"),

        // Flexible Storm/Myth players.
        signup(guild.getName(), "flex_01", "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        signup(guild.getName(), "flex_02", "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        signup(guild.getName(), "flex_03", "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        signup(guild.getName(), "flex_04", "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        signup(guild.getName(), "flex_05", "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        signup(guild.getName(), "flex_06", "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        signup(guild.getName(), "flex_07", "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        signup(guild.getName(), "flex_08", "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        signup(guild.getName(), "flex_09", "one", "two", "three", "four", "a", "b", "c", "d", "hex"),
        signup(guild.getName(), "flex_10", "one", "two", "three", "four", "a", "b", "c", "d", "hex"),

        // Semi-flexible players.
        signup(guild.getName(), "semi_01", "one", "two", "three", "a", "b", "c"),
        signup(guild.getName(), "semi_02", "one", "two", "three", "a", "b", "c"),
        signup(guild.getName(), "semi_03", "one", "two", "three", "a", "b", "c"),
        signup(guild.getName(), "semi_04", "one", "two", "three", "a", "b", "c"),
        signup(guild.getName(), "semi_05", "one", "two", "three", "a", "b", "c"),
        signup(guild.getName(), "semi_06", "one", "two", "three", "a", "b", "c"),

        // Bad utility edge case: has hex but no eligible primary role.
        signup(guild.getName(), "hex_only_invalid", "hex"));
  }

  private Guild createGuild() {
    return guildService.createGuild(
        CreateGuildCommand.builder()
            .name("Onyx Cats")
            .raiderRoleId("1519114924981354708")
            .serverId("1501862567549603961")
            .build());
  }

  private void createTemplate() {
    String serverId = guildService.findGuildByName("Onyx Cats").getServerId();
    templateService.createTemplate(serverId, "takanobu", "Takanobu", "Stress test", "coin");

    strategyService.addStrategy(serverId, "takanobu", "storm", "Storm");
    strategyService.addPrimaryRole(serverId, "takanobu", "storm", "one", "Potent Feint", ":one:", 1);
    strategyService.addPrimaryRole(
        serverId,
        "takanobu",
        "storm",
        "two",
        "Sharpened Elemental Blade",
        ":two:",
        1);
    strategyService.addPrimaryRole(
        serverId,
        "takanobu",
        "storm",
        "three",
        "IC Sharpened Elemental Blade",
        ":three:",
        1);
    strategyService.addPrimaryRole(serverId, "takanobu", "storm", "four", "Epic Bunyip", ":four:", 1);

    strategyService.addStrategy(serverId, "takanobu", "myth", "Myth");
    strategyService.addPrimaryRole(serverId, "takanobu", "myth", "a", "Potent Feint", ":regional_indicator_a:", 1);
    strategyService.addPrimaryRole(
        serverId,
        "takanobu",
        "myth",
        "b",
        "Sharpened Spirit Blade",
        ":regional_indicator_b:",
        1);
    strategyService.addPrimaryRole(
        serverId,
        "takanobu",
        "myth",
        "c",
        "IC Sharpened Spirit Blade",
        ":regional_indicator_c:",
        1);
    strategyService.addPrimaryRole(serverId, "takanobu", "myth", "d", "Epic Drop Bear", ":regional_indicator_d:", 1);
    strategyService.addUtilityRole(
        serverId,
        "takanobu",
        "myth",
        "hex",
        "Hex",
        ":schoolSun_1:",
        1,
        Set.of("a", "b", "c"));
  }

  private EventMemberSignup signup(String guildName, String userId, String... roleKeys) {
    return new EventMemberSignup(guildMemberService.createGuildMember(userId, guildName), roleKeys);
  }

}
