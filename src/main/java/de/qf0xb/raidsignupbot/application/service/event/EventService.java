package de.qf0xb.raidsignupbot.application.service.event;

import de.qf0xb.raidsignupbot.application.TeamGeneratorResult;
import de.qf0xb.raidsignupbot.application.dto.CreateEventCommand;
import de.qf0xb.raidsignupbot.application.port.EventApplicationPort;
import de.qf0xb.raidsignupbot.application.port.EventUIApplicationPort;
import de.qf0xb.raidsignupbot.application.port.template.EventTemplateApplicationPort;
import de.qf0xb.raidsignupbot.application.service.generator.OrToolsTeamGeneratorService;
import de.qf0xb.raidsignupbot.application.service.guild.GuildMemberService;
import de.qf0xb.raidsignupbot.application.service.guild.RaiderRoleService;
import de.qf0xb.raidsignupbot.application.service.guild.GuildService;
import de.qf0xb.raidsignupbot.application.service.calendar.RaidCalendarService;
import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.application.service.template.TemplateService;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventMemberSignup;
import de.qf0xb.raidsignupbot.domain.model.event.EventSlot;
import de.qf0xb.raidsignupbot.domain.model.event.EventTeam;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.adapter.DiscordEventUIApplicationAdapter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
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
  private final RaiderRoleService raiderRoleService;
  private final Map<Long, TeamGeneratorResult> pendingApprovalResults = new ConcurrentHashMap<>();

  public EventService(
      EventApplicationPort eventApplicationPort,
      GuildService guildService,
      ServerService serverService,
      GuildMemberService guildMemberService,
      TemplateService templateService,
      EventUIApplicationPort eventUIApplicationPort,
      RaidCalendarService raidCalendarService,
      EventBackupService eventBackupService,
      RaiderRoleService raiderRoleService) {
    this.eventApplicationPort = eventApplicationPort;
    this.guildService = guildService;
    this.serverService = serverService;
    this.guildMemberService = guildMemberService;
    this.templateService = templateService;
    this.eventUIApplicationPort = eventUIApplicationPort;
    this.raidCalendarService = raidCalendarService;
    this.eventBackupService = eventBackupService;
    this.raiderRoleService = raiderRoleService;
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
    GuildMember hoster = ensureMember(command.getHosterUserId(), guild);
    Event event =
        Event.builder()
            .guild(guild)
            .hoster(hoster)
            .eventAdmin(resolveEventAdmin(command.getEventAdminUserId(), hoster, guild))
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
    GuildMember hoster = ensureMember(command.getHosterUserId(), guild);
    String categoryId = command.getCategoryId() != null ? command.getCategoryId() : existingEvent.getCategoryId();
    String signUpChannelId = command.getSignUpChannelId() != null ? command.getSignUpChannelId() : existingEvent.getSignUpChannelId();
    String voiceChannelId = command.getVoiceChannelId() != null ? command.getVoiceChannelId() : existingEvent.getVoiceChannelId();
    Event updatedEvent =
        Event.builder()
            .id(existingEvent.getId())
            .guild(guild)
            .hoster(hoster)
            .eventAdmin(resolveEventAdmin(command.getEventAdminUserId(), hoster, guild))
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
    return syncRaiderRoles(eventApplicationPort.save(updatedEvent));
  }

  @Transactional
  public Event postponeEvent(String serverId, Long eventId, int minutes) {
    if (minutes <= 0) {
      throw new IllegalArgumentException("The delay must be at least one minute.");
    }
    Event event = findEventById(eventId);
    if (event.getGuild() == null || !serverId.equals(event.getGuild().getServerId())) {
      throw new IllegalArgumentException("This raid does not belong to this server.");
    }
    Instant now = Instant.now();
    if (event.getStart() == null || event.getEnd() == null || !event.getStart().isAfter(now)) {
      throw new IllegalArgumentException("Only raids that have not started can be postponed.");
    }
    Instant oldStart = event.getStart();
    Duration delay = Duration.ofMinutes(minutes);
    Instant newStart = oldStart.plus(delay);
    Instant newEnd = event.getEnd().plus(delay);
    raidCalendarService.rescheduleEvent(serverId, eventId, event.getGuild().getName(), newStart, newEnd);
    event.setStart(newStart);
    event.setEnd(newEnd);
    // Rearm only reminders whose new deadline is still ahead, avoiding immediate duplicate pings.
    if (newStart.minusSeconds(3600).isAfter(now)) {
      event.setOneHourBeforeEmitted(false);
    }
    if (newStart.minusSeconds(1800).isAfter(now)) {
      event.setThirtyMinutesBeforeEmitted(false);
    }
    if (newStart.minusSeconds(900).isAfter(now)) {
      event.setFifteenMinutesBeforeEmitted(false);
    }
    event.setOneHourAfterEmitted(false);
    eventUIApplicationPort.updateEvent(event, event);
    if (event.getTeamsMessageId() != null) {
      eventUIApplicationPort.updateTeams(event, new TeamGeneratorResult(event.getTeams(), List.of()));
    }
    Event savedEvent = eventApplicationPort.save(event);
    eventBackupService.extendOffers(eventId, newEnd);
    eventUIApplicationPort.announcePostponement(savedEvent, oldStart);
    return syncRaiderRoles(savedEvent);
  }

  @Transactional
  public Event swapTeamMembers(Long eventId, String leftUserId, String rightUserId) {
    return swapTeamMembers(eventId, leftUserId, rightUserId, null, null);
  }

  @Transactional
  public Event swapTeamMembers(Long eventId, String leftUserId, String rightUserId, String leftSlot, String rightSlot) {
    Event event = findEventById(eventId);
    if (!event.isTeamsGenerated() || event.getTeams().isEmpty()) {
      throw new IllegalStateException("Event teams have not been generated yet");
    }

    GuildMember leftMember = guildMemberService.findGuildMemberById(leftUserId);
    GuildMember rightMember = guildMemberService.findGuildMemberById(rightUserId);
    swapMembers(event, leftMember, rightMember, leftSlot, rightSlot);

    Event savedEvent = eventApplicationPort.save(event);
    eventUIApplicationPort.updateTeams(savedEvent, new TeamGeneratorResult(savedEvent.getTeams(), List.of()));
    return syncRaiderRoles(eventApplicationPort.save(savedEvent));
  }

  @Transactional
  public Event swapTeamsForApproval(Long eventId, String leftUserId, String rightUserId) {
    return swapTeamsForApproval(eventId, leftUserId, rightUserId, null, null);
  }

  @Transactional
  public Event swapTeamsForApproval(Long eventId, String leftUserId, String rightUserId, String leftSlot, String rightSlot) {
    Event event = findEventById(eventId);
    if (!event.isApprovalPending() || event.getTeams().isEmpty()) {
      throw new IllegalStateException("Event teams do not have a pending approval proposal");
    }

    TeamGeneratorResult pendingResult = pendingApprovalResults.get(eventId);
    if (pendingResult == null) {
      throw new IllegalStateException("No pending team result stored for event " + eventId);
    }

    GuildMember leftMember = guildMemberService.findGuildMemberById(leftUserId);
    GuildMember rightMember = guildMemberService.findGuildMemberById(rightUserId);
    if (!event.getTeams().stream().anyMatch(team -> team.containsMember(leftMember))
        || !event.getTeams().stream().anyMatch(team -> team.containsMember(rightMember))) {
      throw new IllegalStateException("Approval editing can only swap members already assigned to the proposed teams");
    }
    swapMembers(event, leftMember, rightMember, leftSlot, rightSlot);

    TeamGeneratorResult updatedResult = new TeamGeneratorResult(
        List.copyOf(event.getTeams()), pendingResult.unmatchedSignups());
    pendingApprovalResults.put(eventId, updatedResult);
    Event savedEvent = eventApplicationPort.save(event);
    eventUIApplicationPort.sendTeamsForApproval(savedEvent, updatedResult);
    return savedEvent;
  }

  private void swapMembers(Event event, GuildMember leftMember, GuildMember rightMember, String leftSlotId, String rightSlotId) {
    if (leftMember.getUserId().equals(rightMember.getUserId())) {
      throw new IllegalArgumentException("Cannot swap the same member");
    }

    MemberSlot left = selectMemberSlot(event, leftMember, leftSlotId);
    MemberSlot right = selectMemberSlot(event, rightMember, rightSlotId);
    if (left == null && right == null) {
      throw new IllegalStateException(
          "Neither member is assigned to the generated teams for event " + event.getId());
    }

    if (left != null && right != null) {
      left.team().swapSlots(left.slot(), right.team(), right.slot());
    } else if (left != null) {
      left.team().replaceSlot(left.slot(), rightMember);
    } else {
      right.team().replaceSlot(right.slot(), leftMember);
    }
  }

  private MemberSlot selectMemberSlot(Event event, GuildMember member, String selectedId) {
    List<MemberSlot> matches = new ArrayList<>();
    for (int teamIndex = 0; teamIndex < event.getTeams().size(); teamIndex++) {
      EventTeam team = event.getTeams().get(teamIndex);
      for (int slotIndex = 0; slotIndex < team.getSlots().size(); slotIndex++) {
        EventSlot slot = team.getSlots().get(slotIndex);
        if (slot.isAssigned() && slot.getMember().getUserId().equals(member.getUserId())) {
          matches.add(new MemberSlot(team, slot, (teamIndex + 1) + ":" + (slotIndex + 1)));
        }
      }
    }
    if (selectedId != null && !selectedId.isBlank()) {
      return matches.stream().filter(match -> match.id().equals(selectedId.trim())).findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Slot " + selectedId + " is not assigned to the selected player."));
    }
    if (matches.size() > 1) {
      String choices = matches.stream().map(match -> match.id() + " (" + match.slot().getRoleName() + ")")
          .collect(java.util.stream.Collectors.joining(", "));
      throw new IllegalArgumentException("Player " + member.getUserId()
          + " has multiple boxes. Specify their slot from the teams message: " + choices);
    }
    return matches.isEmpty() ? null : matches.getFirst();
  }

  private record MemberSlot(EventTeam team, EventSlot slot, String id) {}

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
    return syncRaiderRoles(eventApplicationPort.save(savedEvent));
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

    return syncRaiderRoles(event);
  }

  /**
   * Test mode: reopens the event and builds teams from simulated signups, optionally plus the real
   * reactions, then publishes them or asks for approval exactly like closing the signup does.
   */
  @Transactional
  public Event simulateTeamCreation(Long eventId, List<EventMemberSignup> simulatedSignups, boolean includeRealSignups) {
    Event event = findEventById(eventId);
    List<EventMemberSignup> signups = new ArrayList<>();
    if (includeRealSignups && event.getMessageId() != null) {
      signups.addAll(eventUIApplicationPort.closeSignup(event));
    }
    for (EventMemberSignup simulated : simulatedSignups) {
      // Team assignments reference stored guild members, so simulated players are stored like real ones.
      GuildMember member = ensureMember(simulated.getGuildMember().getUserId(), event.getGuild());
      member.setPriority(simulated.getGuildMember().getPriority());
      EventMemberSignup signup = new EventMemberSignup(member);
      simulated.getRoleKeys().forEach(signup::addRoleKey);
      signups.add(signup);
    }

    event.reopenSignup();
    event.clearTeams();
    pendingApprovalResults.remove(eventId);
    eventBackupService.clearOffers(eventId);
    eventApplicationPort.save(event);
    return generateTeamsAndCloseSignup(eventId, signups);
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
    return syncRaiderRoles(eventApplicationPort.save(event));
  }

  @Transactional
  public String respondToBackupOffer(String offerId, String userId, boolean available) {
    EventBackupService.BackupResponse response = eventBackupService.respond(offerId, userId, available);
    if (!response.acceptedOffers().isEmpty()) {
      Event event = findEventById(response.acceptedOffers().getFirst().eventId());
      eventUIApplicationPort.announceBackup(event, response.acceptedOffers());
      syncRaiderRoles(event);
    }
    return response.message();
  }

  @Transactional
  public Event rejectGeneratedTeams(Long eventId, String rejectedByUserId) {
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
    Event savedEvent = eventApplicationPort.save(event);
    eventUIApplicationPort.announceTeamsRejected(savedEvent, rejectedByUserId);
    return syncRaiderRoles(savedEvent);
  }

  /** Ends the event: closes signup, suppresses remaining reminders, and removes its Discord channels. */
  @Transactional
  public Event closeEvent(Long eventId) {
    Event event = findEventById(eventId);
    event.closeSignup();
    event.markFifteenMinutesBeforeEmitted();
    event.markOneHourAfterEmitted();
    event.markOneHourBeforeEmitted();
    event.markThirtyMinutesBeforeEmitted();
    eventUIApplicationPort.deleteEvent(event);
    releaseRaiderRoles(eventId);
    return eventApplicationPort.save(event);
  }

  /** Deletes the event and removes its signup message, teams message, and managed channels from Discord. */
  @Transactional
  public Event deleteEventEverywhere(Long eventId) {
    Event deleted = deleteEvent(eventId);
    eventUIApplicationPort.removeEventFromDiscord(deleted);
    return deleted;
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

  private GuildMember resolveEventAdmin(String eventAdminUserId, GuildMember hoster, Guild guild) {
    if (eventAdminUserId == null || eventAdminUserId.isBlank()) {
      return hoster;
    }
    return ensureMember(eventAdminUserId, guild);
  }

  // Hosts and admins are picked freely in Discord, so they may not have been synced as guild members yet.
  private GuildMember ensureMember(String userId, Guild guild) {
    return guildMemberService.ensureGuildMember(userId, guild.getServerId(), guild.getName());
  }

  public Optional<Event> findEventByIdOptional(Long eventId) {
    return eventApplicationPort.findById(eventId);
  }

  /**
   * Removes the event, its teams, and pending approvals/backups, and frees its calendar entry.
   * Discord messages and channels are cleaned up separately by the caller.
   */
  @Transactional
  public Event deleteEvent(Long eventId) {
    Event event = findEventById(eventId);
    releaseRaiderRoles(eventId);
    raidCalendarService.releaseEvent(eventId);
    eventApplicationPort.deleteById(eventId);
    pendingApprovalResults.remove(eventId);
    eventBackupService.clearOffers(eventId);
    return event;
  }

  // Role problems such as a missing Manage Roles permission must not break the raid itself.
  private Event syncRaiderRoles(Event event) {
    try {
      raiderRoleService.syncEvent(event);
    } catch (RuntimeException ex) {
      log.warn("Could not update raider roles for event {}", event.getId(), ex);
    }
    return event;
  }

  private void releaseRaiderRoles(Long eventId) {
    try {
      raiderRoleService.releaseEvent(eventId);
    } catch (RuntimeException ex) {
      log.warn("Could not release raider roles for event {}", eventId, ex);
    }
  }

  public Event findEventById(Long eventId) {
    return eventApplicationPort.findById(eventId)
        .orElseThrow(() ->  new IllegalArgumentException("Event with id " + eventId + " not found"));
  }
}
