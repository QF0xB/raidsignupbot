package de.qf0xb.raidsignupbot.application.service.guild;

import de.qf0xb.raidsignupbot.application.port.guild.MemberRolePort;
import de.qf0xb.raidsignupbot.application.port.guild.RaiderRoleGrantApplicationPort;
import de.qf0xb.raidsignupbot.application.service.event.EventBackupService;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventBackupOffer;
import de.qf0xb.raidsignupbot.domain.model.event.EventSlot;
import de.qf0xb.raidsignupbot.domain.model.event.EventUtilityAssignment;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.domain.model.guild.RaiderRoleGrant;
import de.qf0xb.raidsignupbot.domain.model.guild.SimulatedPlayer;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Gives the guild's raider role to players of published teams and to accepted backups, and takes it
 * away an hour after the raid. Only roles the bot added are removed: players who already had the role
 * keep it, and a player keeps a granted role while another raid still needs it.
 */
@Slf4j
@ApplicationScoped
public class RaiderRoleService {
  static final Duration RELEASE_DELAY = Duration.ofHours(1);

  private final RaiderRoleGrantApplicationPort grantPort;
  private final MemberRolePort memberRolePort;
  private final EventBackupService eventBackupService;

  public RaiderRoleService(
      RaiderRoleGrantApplicationPort grantPort,
      MemberRolePort memberRolePort,
      EventBackupService eventBackupService) {
    this.grantPort = grantPort;
    this.memberRolePort = memberRolePort;
    this.eventBackupService = eventBackupService;
  }

  /**
   * Brings the granted roles of one raid in line with its published teams and accepted backups. Runs in
   * its own transaction: Discord role changes cannot be rolled back, so their records must be kept even
   * if the caller fails, and a grant problem must never roll back the raid change itself.
   */
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void syncEvent(Event event) {
    Map<String, RaiderRoleGrant> grants = grantPort.findByEventId(event.getId()).stream()
        .collect(Collectors.toMap(RaiderRoleGrant::getUserId, Function.identity()));
    Guild guild = event.getGuild();
    String roleId = guild == null || !guild.isRaiderRolesEnabled() || isBlank(guild.getRaiderRoleId())
        ? null
        : guild.getRaiderRoleId();
    boolean published = event.isTeamsGenerated() && !event.isApprovalPending() && event.getEnd() != null;

    Set<String> players = published && roleId != null ? teamMembers(event) : Set.of();
    // Accepted backups are only known until a restart, so recorded backup grants count as accepted too.
    Set<String> backups = new HashSet<>();
    if (published && roleId != null) {
      eventBackupService.findAcceptedOffers(event.getId()).stream().map(EventBackupOffer::userId).forEach(backups::add);
      grants.values().stream().filter(RaiderRoleGrant::isBackup).map(RaiderRoleGrant::getUserId).forEach(backups::add);
      backups.removeAll(players);
    }

    for (RaiderRoleGrant grant : grants.values()) {
      boolean wanted = players.contains(grant.getUserId()) || backups.contains(grant.getUserId());
      if (!wanted || !grant.getRoleId().equals(roleId)) {
        release(grant);
      }
    }
    if (roleId == null) {
      return;
    }

    Instant releaseAfter = event.getEnd().plus(RELEASE_DELAY);
    Set<String> wanted = new LinkedHashSet<>(players);
    wanted.addAll(backups);
    for (String userId : wanted) {
      boolean backup = backups.contains(userId);
      RaiderRoleGrant existing = grants.get(userId);
      if (existing != null && existing.getRoleId().equals(roleId)) {
        if (existing.isBackup() != backup || !existing.getReleaseAfter().equals(releaseAfter)) {
          existing.setBackup(backup);
          existing.setReleaseAfter(releaseAfter);
          grantPort.save(existing);
        }
      } else {
        grant(event.getId(), guild.getServerId(), userId, roleId, backup, releaseAfter);
      }
    }
  }

  /** Releases all roles granted for a raid, e.g. when it is closed or deleted. */
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void releaseEvent(Long eventId) {
    grantPort.findByEventId(eventId).forEach(this::release);
  }

  @Scheduled(every = "5m", identity = "raider-role-release")
  @Transactional
  void releaseFinishedRaids() {
    grantPort.findReleasableBefore(Instant.now()).forEach(this::release);
  }

  private void grant(Long eventId, String serverId, String userId, String roleId, boolean backup, Instant releaseAfter) {
    try {
      boolean grantedForAnotherRaid = grantPort.countGrants(serverId, userId, roleId) > 0;
      if (!grantedForAnotherRaid) {
        if (memberRolePort.hasRole(serverId, userId, roleId)) {
          return; // The player already has the role independently of the bot.
        }
        memberRolePort.addRole(serverId, userId, roleId);
      }
      grantPort.save(RaiderRoleGrant.builder()
          .eventId(eventId)
          .serverId(serverId)
          .userId(userId)
          .roleId(roleId)
          .backup(backup)
          .releaseAfter(releaseAfter)
          .build());
    } catch (RuntimeException ex) {
      log.warn("Could not give raider role {} to {} on server {}", roleId, userId, serverId, ex);
    }
  }

  private void release(RaiderRoleGrant grant) {
    grantPort.deleteById(grant.getId());
    if (grantPort.countGrants(grant.getServerId(), grant.getUserId(), grant.getRoleId()) > 0) {
      return;
    }
    try {
      memberRolePort.removeRole(grant.getServerId(), grant.getUserId(), grant.getRoleId());
    } catch (RuntimeException ex) {
      log.warn("Could not remove raider role {} from {} on server {}",
          grant.getRoleId(), grant.getUserId(), grant.getServerId(), ex);
    }
  }

  private static Set<String> teamMembers(Event event) {
    Set<String> members = new LinkedHashSet<>();
    event.getTeams().forEach(team -> {
      team.getSlots().stream().filter(EventSlot::isAssigned).map(slot -> slot.getMember().getUserId())
          .forEach(members::add);
      team.getUtilityAssignments().stream().filter(EventUtilityAssignment::isAssigned)
          .map(assignment -> assignment.getMember().getUserId()).filter(Objects::nonNull)
          .forEach(members::add);
    });
    members.removeIf(SimulatedPlayer::isSimulated);
    return members;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
