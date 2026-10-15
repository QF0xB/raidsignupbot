package de.qf0xb.raidsignupbot.infrastructure.mapper.jpa;

import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventTeam;
import de.qf0xb.raidsignupbot.infrastructure.jpa.guild.GuildEntity;
import de.qf0xb.raidsignupbot.infrastructure.jpa.guild.GuildEntityRepository;
import de.qf0xb.raidsignupbot.infrastructure.jpa.event.EventEntity;
import de.qf0xb.raidsignupbot.infrastructure.jpa.event.EventTeamEntity;
import de.qf0xb.raidsignupbot.infrastructure.jpa.template.EventStrategyEntity;
import de.qf0xb.raidsignupbot.infrastructure.jpa.template.EventTemplateEntityRepository;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import jakarta.inject.Inject;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.JAKARTA_CDI,
    uses = {GuildEntityMapper.class, GuildMemberEntityMapper.class, EventTemplateEntityMapper.class, EventTeamEntityMapper.class})
public abstract class EventEntityMapper implements JpaEntityMapper<EventEntity, Event> {
  @Inject
  GuildEntityMapper guildEntityMapper;

  @Inject
  GuildEntityRepository guildEntityRepository;

  @Inject
  GuildMemberEntityMapper guildMemberEntityMapper;

  @Inject
  EventTemplateEntityMapper eventTemplateEntityMapper;

  @Inject
  EventTemplateEntityRepository eventTemplateEntityRepository;

  @Inject
  EventTeamEntityMapper eventTeamEntityMapper;

  @Override
  @Mapping(target = "categoryId", source = "categoryId")
  @Mapping(target = "signUpChannelId", source = "signUpChannelId")
  @Mapping(target = "messageId", source = "messageId")
  @Mapping(target = "teamsMessageId", source = "teamsMessageId")
  @Mapping(target = "voiceChannelId", source = "voiceChannelId")
  @Mapping(target = "eventAdmin", source = "eventAdmin")
  @Mapping(target = "approvalPending", source = "approvalPending")
  @Mapping(target = "teamsRequireApproval", source = "teamsRequireApproval")
  @Mapping(target = "eventTemplate", source = "template")
  @Mapping(target = "eventTeams", ignore = true)
  public abstract EventEntity toEntity(Event event);

  @Override
  @Mapping(target = "categoryId", source = "categoryId")
  @Mapping(target = "signUpChannelId", source = "signUpChannelId")
  @Mapping(target = "messageId", source = "messageId")
  @Mapping(target = "teamsMessageId", source = "teamsMessageId")
  @Mapping(target = "voiceChannelId", source = "voiceChannelId")
  @Mapping(target = "eventAdmin", source = "eventAdmin")
  @Mapping(target = "approvalPending", source = "approvalPending")
  @Mapping(target = "teamsRequireApproval", source = "teamsRequireApproval")
  @Mapping(target = "template", source = "eventTemplate")
  public abstract Event toDomain(EventEntity eventEntity);

  @Override
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "categoryId", source = "categoryId")
  @Mapping(target = "signUpChannelId", source = "signUpChannelId")
  @Mapping(target = "messageId", source = "messageId")
  @Mapping(target = "teamsMessageId", source = "teamsMessageId")
  @Mapping(target = "voiceChannelId", source = "voiceChannelId")
  @Mapping(target = "eventAdmin", source = "eventAdmin")
  @Mapping(target = "approvalPending", source = "approvalPending")
  @Mapping(target = "teamsRequireApproval", source = "teamsRequireApproval")
  @Mapping(target = "eventTemplate", source = "template")
  @Mapping(target = "eventTeams", ignore = true)
  public abstract EventEntity partialUpdate(Event event, @MappingTarget EventEntity eventEntity);

  @AfterMapping
  protected void populateDomain(EventEntity source, @MappingTarget Event target) {
    source.getEventTeams().stream()
        .map(eventTeamEntityMapper::toDomain)
        .forEach(target::addTeam);

    target.setDonation(source.getDonation());
    target.setDonationPerWin(Boolean.TRUE.equals(source.getDonationPerWin()));
    target.setCategoryId(source.getCategoryId());
    target.setSignUpChannelId(source.getSignUpChannelId());
    target.setMessageId(source.getMessageId());
    target.setTeamsMessageId(source.getTeamsMessageId());
    target.setVoiceChannelId(source.getVoiceChannelId());
    target.setEventAdmin(guildMemberEntityMapper.toDomain(source.getEventAdmin()));
    target.setApprovalPending(Boolean.TRUE.equals(source.getApprovalPending()));
    target.setTeamsRequireApproval(Boolean.TRUE.equals(source.getTeamsRequireApproval()));
    target.setStart(source.getStartAt());
    target.setEnd(source.getEndAt());
    target.setSignupOpen(Boolean.TRUE.equals(source.getSignupOpen()));
    target.setTeamsGenerated(Boolean.TRUE.equals(source.getTeamsGenerated()));
    target.setChannelsExist(Boolean.TRUE.equals(source.getChannelsExist()));
    target.setOneHourBeforeEmitted(Boolean.TRUE.equals(source.getOneHourBeforeEmitted()));
    target.setThirtyMinutesBeforeEmitted(Boolean.TRUE.equals(source.getThirtyMinutesBeforeEmitted()));
    target.setFifteenMinutesBeforeEmitted(Boolean.TRUE.equals(source.getFifteenMinutesBeforeEmitted()));
    target.setOneHourAfterEmitted(Boolean.TRUE.equals(source.getOneHourAfterEmitted()));
  }

  @AfterMapping
  protected void populateEntity(Event source, @MappingTarget EventEntity target) {
    GuildEntity guildEntity = guildEntityRepository.find(
            "server.serverId = ?1 and name = ?2",
            source.getGuild().getServerId(),
            source.getGuild().getName())
        .firstResultOptional()
        .orElseThrow(() -> new IllegalArgumentException(
            "Guild with name " + source.getGuild().getName() + " not found for server " + source.getGuild().getServerId()));
    target.setGuild(guildEntity);
    target.setHoster(guildMemberEntityMapper.toEntity(source.getHoster()));
    target.setEventTemplate(
        eventTemplateEntityRepository.findByIdOptional(source.getTemplate().getKey())
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "EventTemplate with key " + source.getTemplate().getKey() + " not found")));
    target.setDonation(source.getDonation());
    target.setDonationPerWin(source.isDonationPerWin());
    target.setCategoryId(source.getCategoryId());
    target.setSignUpChannelId(source.getSignUpChannelId());
    target.setMessageId(source.getMessageId());
    target.setTeamsMessageId(source.getTeamsMessageId());
    target.setVoiceChannelId(source.getVoiceChannelId());
    target.setEventAdmin(guildMemberEntityMapper.toEntity(source.getEventAdmin()));
    target.setApprovalPending(source.isApprovalPending());
    target.setTeamsRequireApproval(source.isTeamsRequireApproval());
    target.setSignupOpen(source.isSignupOpen());
    target.setTeamsGenerated(source.isTeamsGenerated());
    target.setChannelsExist(source.isChannelsExist());
    target.setOneHourBeforeEmitted(source.isOneHourBeforeEmitted());
    target.setThirtyMinutesBeforeEmitted(source.isThirtyMinutesBeforeEmitted());
    target.setFifteenMinutesBeforeEmitted(source.isFifteenMinutesBeforeEmitted());
    target.setOneHourAfterEmitted(source.isOneHourAfterEmitted());
    target.setStartAt(source.getStart());
    target.setEndAt(source.getEnd());
    syncTeams(target, source);
  }

  private void syncTeams(EventEntity target, Event source) {
    Map<String, List<EventTeamEntity>> existingTeamsByStrategyKey = new LinkedHashMap<>();
    for (EventTeamEntity teamEntity : target.getEventTeams()) {
      existingTeamsByStrategyKey.computeIfAbsent(teamEntity.getEventStrategy().getKey(), ignored -> new ArrayList<>())
          .add(teamEntity);
    }

    Map<String, EventStrategyEntity> templateStrategiesByKey = new LinkedHashMap<>();
    for (EventStrategyEntity strategyEntity : target.getEventTemplate().getEventStrategies()) {
      templateStrategiesByKey.put(strategyEntity.getKey(), strategyEntity);
    }

    Set<EventTeamEntity> updatedTeams = new LinkedHashSet<>();
    for (EventTeam team : source.getTeams()) {
      updatedTeams.add(syncTeam(target, existingTeamsByStrategyKey, templateStrategiesByKey, team));
    }

    target.getEventTeams().clear();
    target.getEventTeams().addAll(updatedTeams);
  }

  private EventTeamEntity syncTeam(
      EventEntity target,
      Map<String, List<EventTeamEntity>> existingTeamsByStrategyKey,
      Map<String, EventStrategyEntity> templateStrategiesByKey,
      EventTeam team) {
    String strategyKey = team.getStrategy().getKey();
    List<EventTeamEntity> existingTeams = existingTeamsByStrategyKey.get(strategyKey);
    EventTeamEntity existing = existingTeams == null || existingTeams.isEmpty() ? null : existingTeams.removeFirst();
    EventTeamEntity updated;

    if (existing != null) {
      updated = eventTeamEntityMapper.partialUpdate(team, existing);
      updated.setId(existing.getId());
    } else {
      updated = eventTeamEntityMapper.toEntity(team);
    }

    updated.setEventStrategy(templateStrategiesByKey.get(strategyKey));
    updated.setEvent(target);
    return updated;
  }
}
