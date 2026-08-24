package de.qf0xb.raidsignupbot.application.service.event;

import de.qf0xb.raidsignupbot.application.TeamGeneratorResult;
import de.qf0xb.raidsignupbot.application.dto.CreateEventCommand;
import de.qf0xb.raidsignupbot.application.port.EventApplicationPort;
import de.qf0xb.raidsignupbot.application.port.EventUIApplicationPort;
import de.qf0xb.raidsignupbot.application.port.template.EventTemplateApplicationPort;
import de.qf0xb.raidsignupbot.application.service.generator.OrToolsTeamGeneratorService;
import de.qf0xb.raidsignupbot.application.service.guild.GuildMemberService;
import de.qf0xb.raidsignupbot.application.service.guild.GuildService;
import de.qf0xb.raidsignupbot.application.service.calendar.RaidCalendarService;
import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.application.service.template.TemplateService;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventMemberSignup;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.adapter.DiscordEventUIApplicationAdapter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@ApplicationScoped
public class EventService {
  private final EventApplicationPort eventApplicationPort;
  private final GuildService guildService;
  private final ServerService serverService;
  private final GuildMemberService guildMemberService;
  private final TemplateService templateService;
  private final OrToolsTeamGeneratorService teamGeneratorService = new OrToolsTeamGeneratorService();
  private final EventUIApplicationPort eventUIApplicationPort;
  private final RaidCalendarService raidCalendarService;
  private final EventBackupService eventBackupService;
  private final Map<Long, TeamGeneratorResult> pendingApprovalResults = new ConcurrentHashMap<>();

  public EventService(
      EventApplicationPort eventApplicationPort,
      GuildService guildService,
      ServerService serverService,
      GuildMemberService guildMemberService,
      TemplateService templateService,
      EventUIApplicationPort eventUIApplicationPort,
      RaidCalendarService raidCalendarService,
      EventBackupService eventBackupService) {
    this.eventApplicationPort = eventApplicationPort;
    this.guildService = guildService;
    this.serverService = serverService;
    this.guildMemberService = guildMemberService;
    this.templateService = templateService;
    this.eventUIApplicationPort = eventUIApplicationPort;
    this.raidCalendarService = raidCalendarService;
    this.eventBackupService = eventBackupService;
  }

  @Transactional
  public Event createEvent(CreateEventCommand command) {
    raidCalendarService.validateEventCreation(
        command.getServerId(),
        command.getCreatorUserId(),
        command.isCreatorIsGuildManager(),
        command.getCalendarEntryId(),
        command.getGuildName(),
        command.getHosterUserId(),
        command.getTemplateKey(),
        command.getStart(),
        command.getEnd());
    Guild guild = guildService.findGuildByServerIdAndName(command.getServerId(), command.getGuildName());
    boolean manageSignupChannels = serverService.findServerById(guild.getServerId()).isManageSignupChannels();
    boolean manageVoiceChannels = serverService.findServerById(guild.getServerId()).isManageVoiceChannels();
    GuildMember hoster = guildMemberService.findGuildMemberById(command.getHosterUserId());
    Event event =
        Event.builder()
            .guild(guild)
            .hoster(hoster)
            .eventAdmin(resolveEventAdmin(command.getEventAdminUserId(), hoster))
            .donationPerWin(command.isDonationPerWin())
            .donation(command.getDonation())
            .start(command.getStart())
            .end(command.getEnd())
            .template(templateService.findTemplateByKey(guild.getServerId(), command.getTemplateKey()))
            .categoryId(command.getCategoryId())
            .voiceChannelId(command.getVoiceChannelId())
            .signUpChannelId(command.getSignUpChannelId())
            .build();
    event.setTeamsRequireApproval(command.isTeamsRequireApproval());

    if (manageSignupChannels || manageVoiceChannels) {
      event.setCategoryId(command.getCategoryId());
    }
    if (!manageSignupChannels) {
      event.setSignUpChannelId(command.getSignUpChannelId());
    }
    if (!manageVoiceChannels) {
      event.setVoiceChannelId(command.getVoiceChannelId());
    }

    eventUIApplicationPort.addEvent(event);
    Event savedEvent = eventApplicationPort.save(event);
    raidCalendarService.markEventCreated(command.getCalendarEntryId(), savedEvent.getId());
    return savedEvent;
  }

  @Transactional
  public Event updateEvent(Long eventId, CreateEventCommand command) {
    Event existingEvent = findEventById(eventId);
    raidCalendarService.validateEventUpdate(
        command.getServerId(), eventId, command.getGuildName(), command.getStart());
    Guild guild = guildService.findGuildByServerIdAndName(command.getServerId(), command.getGuildName());
    GuildMember hoster = guildMemberService.findGuildMemberById(command.getHosterUserId());
    String categoryId = command.getCategoryId() != null ? command.getCategoryId() : existingEvent.getCategoryId();
    String signUpChannelId = command.getSignUpChannelId() != null ? command.getSignUpChannelId() : existingEvent.getSignUpChannelId();
    String voiceChannelId = command.getVoiceChannelId() != null ? command.getVoiceChannelId() : existingEvent.getVoiceChannelId();
    Event updatedEvent =
        Event.builder()
            .id(existingEvent.getId())
            .guild(guild)
            .hoster(hoster)
            .eventAdmin(resolveEventAdmin(command.getEventAdminUserId(), hoster))
            .donationPerWin(command.isDonationPerWin())
            .donation(command.getDonation())
            .start(command.getStart())
            .end(command.getEnd())
            .template(templateService.findTemplateByKey(guild.getServerId(), command.getTemplateKey()))
            .categoryId(categoryId)
            .voiceChannelId(voiceChannelId)
            .signUpChannelId(signUpChannelId)
            .messageId(existingEvent.getMessageId())
            .teamsMessageId(existingEvent.getTeamsMessageId())
            .build();
    updatedEvent.setTeamsRequireApproval(command.isTeamsRequireApproval());

    updatedEvent.setSignupOpen(existingEvent.isSignupOpen());
    updatedEvent.setTeamsGenerated(existingEvent.isTeamsGenerated());
    updatedEvent.setChannelsExist(existingEvent.isChannelsExist());
    updatedEvent.setApprovalPending(existingEvent.isApprovalPending());
    updatedEvent.setOneHourBeforeEmitted(existingEvent.isOneHourBeforeEmitted());
    updatedEvent.setThirtyMinutesBeforeEmitted(existingEvent.isThirtyMinutesBeforeEmitted());
    updatedEvent.setFifteenMinutesBeforeEmitted(existingEvent.isFifteenMinutesBeforeEmitted());
    updatedEvent.setOneHourAfterEmitted(existingEvent.isOneHourAfterEmitted());
    existingEvent.getTeams().forEach(updatedEvent::addTeam);

    eventUIApplicationPort.updateEvent(existingEvent, updatedEvent);
    if (updatedEvent.isTeamsGenerated() && !updatedEvent.getTeams().isEmpty()) {
      eventUIApplicationPort.updateTeams(updatedEvent, new TeamGeneratorResult(updatedEvent.getTeams(), List.of()));
    }
    return eventApplicationPort.save(updatedEvent);
  }

  @Transactional
  public Event swapTeamMembers(Long eventId, String leftUserId, String rightUserId) {
    Event event = findEventById(eventId);
    if (!event.isTeamsGenerated() || event.getTeams().isEmpty()) {
      throw new IllegalStateException("Event teams have not been generated yet");
    }

    GuildMember leftMember = guildMemberService.findGuildMemberById(leftUserId);
    GuildMember rightMember = guildMemberService.findGuildMemberById(rightUserId);
    if (leftMember.getUserId().equals(rightMember.getUserId())) {
      throw new IllegalArgumentException("Cannot swap the same member");
    }

    boolean leftAssigned = event.getTeams().stream().anyMatch(team -> team.containsMember(leftMember));
    boolean rightAssigned = event.getTeams().stream().anyMatch(team -> team.containsMember(rightMember));

    if (!leftAssigned && !rightAssigned) {
      throw new IllegalStateException(
          "Neither member is assigned to the generated teams for event " + event.getId());
    }

    int changedAssignments;
    if (leftAssigned && rightAssigned) {
      changedAssignments = event.getTeams().stream()
          .mapToInt(team -> team.swapMembers(leftMember, rightMember))
          .sum();
    } else if (leftAssigned) {
      changedAssignments = event.getTeams().stream()
          .mapToInt(team -> team.replaceMember(leftMember, rightMember))
          .sum();
    } else {
      changedAssignments = event.getTeams().stream()
          .mapToInt(team -> team.replaceMember(rightMember, leftMember))
          .sum();
    }

    if (changedAssignments == 0) {
      throw new IllegalStateException("Swap or substitution did not change any assignments for event " + event.getId());
    }

    Event savedEvent = eventApplicationPort.save(event);
    eventUIApplicationPort.updateTeams(savedEvent, new TeamGeneratorResult(savedEvent.getTeams(), List.of()));
    return eventApplicationPort.save(savedEvent);
  }

  @Transactional
  public Event removeSignupAndRegenerateTeams(Long eventId, String userId) {
    Event event = findEventById(eventId);
    if (event.getMessageId() == null) {
      throw new IllegalStateException("Event does not have a signup message yet");
    }

    eventUIApplicationPort.removeSignup(event, userId);
    event.closeSignup();
    List<EventMemberSignup> signups = eventUIApplicationPort.closeSignup(event);
    event.clearTeams();

    TeamGeneratorResult results = generateTeams(event, signups);
    Event savedEvent = eventApplicationPort.save(event);
    publishOrRequestApproval(savedEvent, results);
    return eventApplicationPort.save(savedEvent);
  }

  @Transactional
  public Event generateTeamsAndCloseSignup(Long eventId, List<EventMemberSignup> signups) {
    Event event = findEventById(eventId);
    if (!event.isSignupOpen()) {
      return event;
    }
    event.closeSignup();

    if (signups == null) {
      log.info("Generating teams for event {}: Signups not provided, generating all signups", event.getId());
      signups = eventUIApplicationPort.closeSignup(event);
    }

    log.info("Generated teams for event {}: {}", event.getId(), signups);

    TeamGeneratorResult results = generateTeams(event, signups);
    event = eventApplicationPort.save(event);

    publishOrRequestApproval(event, results);
    event = eventApplicationPort.save(event);

    return event;
  }

  @Transactional
  public Event approveGeneratedTeams(Long eventId) {
    Event event = findEventById(eventId);
    if (!event.isApprovalPending()) {
      throw new IllegalStateException("Event " + eventId + " does not have pending team approval");
    }

    TeamGeneratorResult result = pendingApprovalResults.remove(eventId);
    if (result == null) {
      throw new IllegalStateException("No pending team result stored for event " + eventId);
    }

    event.setTeamsGenerated(true);
    event.clearApprovalPending();
    eventUIApplicationPort.publishTeams(event, result);
    eventUIApplicationPort.requestBackups(event, eventBackupService.findOffers(eventId));
    return eventApplicationPort.save(event);
  }

  @Transactional
  public Event rejectGeneratedTeams(Long eventId) {
    Event event = findEventById(eventId);
    if (!event.isApprovalPending()) {
      throw new IllegalStateException("Event " + eventId + " does not have pending team approval");
    }

    event.reopenSignup();
    event.clearTeams();
    event.setTeamsMessageId(null);
    event.setTeamsGenerated(false);
    event.clearApprovalPending();
    pendingApprovalResults.remove(eventId);
    eventBackupService.clearOffers(eventId);
    return eventApplicationPort.save(event);
  }

  @Transactional
  public TeamGeneratorResult generateTeams(Event event, List<EventMemberSignup> signups) {
    TeamGeneratorResult results = teamGeneratorService.generateTeams(event, signups);
    eventBackupService.registerOffers(event, results);
    results.teams().forEach(event::addTeam);
    results.unmatchedSignups().forEach(eventMemberSignup -> {
      guildMemberService.couldNotFindRole(eventMemberSignup.getGuildMember().getUserId());
    });

    return results;
  }

  private void publishOrRequestApproval(Event event, TeamGeneratorResult results) {
    if (event.isTeamsRequireApproval()) {
      event.setTeamsGenerated(false);
      event.markApprovalPending();
      pendingApprovalResults.put(event.getId(), results);
      eventUIApplicationPort.sendTeamsForApproval(event, results);
      return;
    }

    event.setTeamsGenerated(true);
    pendingApprovalResults.remove(event.getId());
    eventUIApplicationPort.publishTeams(event, results);
    eventUIApplicationPort.requestBackups(event, eventBackupService.findOffers(event.getId()));
  }

  private GuildMember resolveEventAdmin(String eventAdminUserId, GuildMember hoster) {
    if (eventAdminUserId == null || eventAdminUserId.isBlank()) {
      return hoster;
    }
    return guildMemberService.findGuildMemberById(eventAdminUserId);
  }

  public Event findEventById(Long eventId) {
    return eventApplicationPort.findById(eventId)
        .orElseThrow(() ->  new IllegalArgumentException("Event with id " + eventId + " not found"));
  }
}
