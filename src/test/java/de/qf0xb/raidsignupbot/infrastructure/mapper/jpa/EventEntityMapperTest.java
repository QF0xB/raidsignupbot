package de.qf0xb.raidsignupbot.infrastructure.mapper.jpa;

import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventTeam;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import de.qf0xb.raidsignupbot.domain.model.template.EventUtilityRole;
import de.qf0xb.raidsignupbot.infrastructure.jpa.event.EventEntity;
import de.qf0xb.raidsignupbot.infrastructure.jpa.template.EventTemplateEntity;
import de.qf0xb.raidsignupbot.infrastructure.jpa.template.EventTemplateEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EventEntityMapperTest {
  private final EventRoleEntityMapper eventRoleEntityMapper = Mappers.getMapper(EventRoleEntityMapper.class);
  private final EventStrategyEntityMapper eventStrategyEntityMapper = Mappers.getMapper(EventStrategyEntityMapper.class);
  private final EventTemplateEntityMapper eventTemplateEntityMapper = Mappers.getMapper(EventTemplateEntityMapper.class);
  private final GuildEntityMapper guildEntityMapper = Mappers.getMapper(GuildEntityMapper.class);
  private final GuildMemberEntityMapper guildMemberEntityMapper = Mappers.getMapper(GuildMemberEntityMapper.class);
  private final EventTeamEntityMapper eventTeamEntityMapper = Mappers.getMapper(EventTeamEntityMapper.class);
  private final EventEntityMapper eventEntityMapper = Mappers.getMapper(EventEntityMapper.class);
  private final EventTemplateEntityRepository eventTemplateEntityRepository = Mockito.mock(EventTemplateEntityRepository.class);

  @BeforeEach
  void wireMappers() {
    MapperTestSupport.setField(eventStrategyEntityMapper, "eventRoleEntityMapper", eventRoleEntityMapper);
    MapperTestSupport.setField(eventTemplateEntityMapper, "eventStrategyEntityMapper", eventStrategyEntityMapper);
    MapperTestSupport.setField(eventTeamEntityMapper, "eventStrategyEntityMapper", eventStrategyEntityMapper);
    MapperTestSupport.setField(guildMemberEntityMapper, "guildEntityMapper", guildEntityMapper);
    MapperTestSupport.setField(eventTeamEntityMapper, "guildMemberEntityMapper", guildMemberEntityMapper);

    MapperTestSupport.setField(eventEntityMapper, "guildEntityMapper", guildEntityMapper);
    MapperTestSupport.setField(eventEntityMapper, "guildMemberEntityMapper", guildMemberEntityMapper);
    MapperTestSupport.setField(eventEntityMapper, "eventTemplateEntityMapper", eventTemplateEntityMapper);
    MapperTestSupport.setField(eventEntityMapper, "eventTeamEntityMapper", eventTeamEntityMapper);
    MapperTestSupport.setField(eventEntityMapper, "eventTemplateEntityRepository", eventTemplateEntityRepository);
  }

  @Test
  void mapsEventAggregateBothDirections() {
    Guild guild = new Guild("guild-1", "Raid Guild");
    GuildMember hoster = new GuildMember("hoster-1");
    GuildMember raider = new GuildMember("member-1");

    EventTemplate template = new EventTemplate("template-1", "Template One", "Template details", "test");
    EventStrategy strategy = new EventStrategy("storm", "Storm Strategy");
    strategy.addRole(new EventRole("tank", "Tank", ":shield:", 1));
    EventUtilityRole utilityRole = new EventUtilityRole("battle-res", "Battle Res", ":crossed_swords:", 1);
    utilityRole.addEligiblePrimaryRole("tank");
    strategy.addRole(utilityRole);
    template.addStrategy(strategy);
    EventTemplateEntity templateEntity = eventTemplateEntityMapper.toEntity(template);
    Mockito.when(eventTemplateEntityRepository.findByIdOptional("template-1"))
        .thenReturn(Optional.of(templateEntity));

    Event event = Event.builder()
        .id(99L)
        .guild(guild)
        .hoster(hoster)
        .template(template)
        .build();
    event.setDonation(250);
    event.setDonationPerWin(true);
    event.setCategoryId("category-1");
    event.setSignUpChannelId("signup-channel-1");
    event.setMessageId("message-1");
    event.setVoiceChannelId("voice-channel-1");
    event.setStart(Instant.parse("2026-06-23T12:00:00Z"));
    event.setEnd(Instant.parse("2026-06-23T14:00:00Z"));
    event.closeSignup();
    event.markChannelsExist();
    event.markOneHourBeforeEmitted();
    event.markThirtyMinutesBeforeEmitted();
    event.markFifteenMinutesBeforeEmitted();
    event.markOneHourAfterEmitted();

    EventTeam team = new EventTeam(strategy);
    team.assign("tank", raider);
    team.assignUtility("battle-res", raider);
    event.addTeam(team);

    EventEntity entity = eventEntityMapper.toEntity(event);

    assertEquals("guild-1", entity.getGuild().getGuildId());
    assertEquals("hoster-1", entity.getHoster().getUserId());
    assertEquals("template-1", entity.getEventTemplate().getKey());
    assertEquals("category-1", entity.getCategoryId());
    assertEquals("signup-channel-1", entity.getSignUpChannelId());
    assertEquals("message-1", entity.getMessageId());
    assertEquals("voice-channel-1", entity.getVoiceChannelId());
    assertEquals(1, entity.getEventTeams().size());
    assertFalse(entity.getSignupOpen());
    assertTrue(entity.getTeamsGenerated());
    assertTrue(entity.getChannelsExist());
    assertTrue(entity.getOneHourBeforeEmitted());
    assertTrue(entity.getThirtyMinutesBeforeEmitted());
    assertTrue(entity.getFifteenMinutesBeforeEmitted());
    assertTrue(entity.getOneHourAfterEmitted());
    assertEquals(250, entity.getDonation());
    assertTrue(entity.getDonationPerWin());

    assertSame(
        entity.getEventTemplate().getEventStrategies().iterator().next(),
        entity.getEventTeams().iterator().next().getEventStrategy());

    Event roundTrip = eventEntityMapper.toDomain(entity);

    assertEquals(99L, roundTrip.getId());
    assertEquals("guild-1", roundTrip.getGuild().getGuildId());
    assertEquals("hoster-1", roundTrip.getHoster().getUserId());
    assertEquals("template-1", roundTrip.getTemplate().getKey());
    assertEquals("category-1", roundTrip.getCategoryId());
    assertEquals("signup-channel-1", roundTrip.getSignUpChannelId());
    assertEquals("message-1", roundTrip.getMessageId());
    assertEquals("voice-channel-1", roundTrip.getVoiceChannelId());
    assertEquals(250, roundTrip.getDonation());
    assertTrue(roundTrip.isDonationPerWin());
    assertFalse(roundTrip.isSignupOpen());
    assertTrue(roundTrip.isTeamsGenerated());
    assertTrue(roundTrip.isChannelsExist());
    assertTrue(roundTrip.isOneHourBeforeEmitted());
    assertTrue(roundTrip.isThirtyMinutesBeforeEmitted());
    assertTrue(roundTrip.isFifteenMinutesBeforeEmitted());
    assertTrue(roundTrip.isOneHourAfterEmitted());
    assertEquals(1, roundTrip.getTeams().size());

    EventTeam roundTripTeam = roundTrip.getTeams().get(0);
    assertEquals("storm", roundTripTeam.getStrategy().getKey());
    assertEquals("member-1", roundTripTeam.getSlots().stream().filter(slot -> slot.isAssigned()).findFirst().orElseThrow().getMember().getUserId());
    assertEquals("member-1", roundTripTeam.getUtilityAssignments().stream().filter(assignment -> assignment.isAssigned()).findFirst().orElseThrow().getMember().getUserId());
  }
}
