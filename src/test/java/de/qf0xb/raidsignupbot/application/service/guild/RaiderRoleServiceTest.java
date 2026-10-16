package de.qf0xb.raidsignupbot.application.service.guild;

import de.qf0xb.raidsignupbot.application.port.guild.MemberRolePort;
import de.qf0xb.raidsignupbot.application.port.guild.RaiderRoleGrantApplicationPort;
import de.qf0xb.raidsignupbot.application.service.event.EventBackupService;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventTeam;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.guild.RaiderRoleGrant;
import de.qf0xb.raidsignupbot.domain.model.guild.SimulatedPlayer;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class RaiderRoleServiceTest {
  private static final String SERVER = "server";
  private static final String ROLE = "raider";

  private final FakeGrants grants = new FakeGrants();
  private final FakeDiscord discord = new FakeDiscord();
  private final EventBackupService backups = new EventBackupService();
  private final RaiderRoleService service = new RaiderRoleService(grants, discord, backups);

  @Test
  void givesThePublishedTeamTheRoleButLeavesPlayersWhoAlreadyHadIt() {
    discord.roles.add("veteran");
    Event raid = raid(1L, "alice", "veteran");

    service.syncEvent(raid);

    assertEquals(Set.of("alice", "veteran"), discord.roles);
    assertEquals(List.of("alice"), grants.users(1L));

    service.releaseEvent(1L);

    assertEquals(Set.of("veteran"), discord.roles, "only the role the bot added is removed");
  }

  @Test
  void waitsForApprovalBeforeGivingTheRole() {
    Event raid = raid(1L, "alice");
    raid.setTeamsGenerated(false);
    raid.setApprovalPending(true);

    service.syncEvent(raid);

    assertTrue(discord.roles.isEmpty());
  }

  @Test
  void takesTheRoleFromPlayersSwappedOutOfTheTeam() {
    Event raid = raid(1L, "alice", "bob");
    service.syncEvent(raid);

    raid.getTeams().getFirst().swapMembers(new GuildMember("bob"), new GuildMember("carol"));
    service.syncEvent(raid);

    assertEquals(Set.of("alice", "carol"), discord.roles);
  }

  @Test
  void keepsTheRoleWhileAnotherRaidStillNeedsIt() {
    service.syncEvent(raid(1L, "alice"));
    service.syncEvent(raid(2L, "alice"));

    service.releaseEvent(1L);
    assertTrue(discord.roles.contains("alice"));

    service.releaseEvent(2L);
    assertFalse(discord.roles.contains("alice"));
  }

  @Test
  void acceptedBackupsKeepTheirRoleAfterTheBackupRequestIsForgotten() {
    Event raid = raid(1L, "alice");
    // Accepted backup requests live in memory only; after a restart only the recorded grant remains.
    grants.save(RaiderRoleGrant.builder().eventId(1L).serverId(SERVER).userId("backup").roleId(ROLE)
        .backup(true).releaseAfter(raid.getEnd()).build());
    discord.roles.add("backup");

    service.syncEvent(raid);

    assertTrue(discord.roles.contains("backup"));
    assertEquals(List.of("backup", "alice"), grants.users(1L));
  }

  @Test
  void releasesRolesAnHourAfterTheRaid() {
    Event finished = raid(1L, "alice");
    finished.setEnd(Instant.now().minus(Duration.ofHours(2)));
    service.syncEvent(finished);
    service.syncEvent(raid(2L, "bob"));

    service.releaseFinishedRaids();

    assertEquals(Set.of("bob"), discord.roles);
  }

  @Test
  void skipsSimulatedPlayers() {
    service.syncEvent(raid(1L, "alice", SimulatedPlayer.userId(1)));

    assertEquals(Set.of("alice"), discord.roles);
    assertEquals(List.of("alice"), grants.users(1L));
  }

  @Test
  void doesNothingWhenRaiderRolesAreDisabled() {
    Event raid = raid(1L, "alice");
    raid.getGuild().setRaiderRolesEnabled(false);

    service.syncEvent(raid);

    assertTrue(discord.roles.isEmpty());
    assertTrue(grants.users(1L).isEmpty());
  }

  @Test
  void aFailedDiscordCallIsRetriedOnTheNextSync() {
    discord.failing = true;
    Event raid = raid(1L, "alice");

    service.syncEvent(raid);
    assertTrue(grants.users(1L).isEmpty());

    discord.failing = false;
    service.syncEvent(raid);
    assertEquals(Set.of("alice"), discord.roles);
  }

  private Event raid(Long id, String... players) {
    EventStrategy strategy = new EventStrategy("main", "Main");
    strategy.addRole(new EventRole("fire", "Fire", "🔥", players.length));
    EventTeam team = new EventTeam(strategy);
    for (String player : players) {
      team.assign("fire", new GuildMember(player));
    }
    Guild guild = new Guild("Onyx", SERVER);
    guild.setRaiderRoleId(ROLE);
    Instant start = Instant.now().plus(Duration.ofDays(1));
    Event event = Event.builder().id(id).guild(guild).start(start).end(start.plus(Duration.ofHours(2)))
        .teamsGenerated(true).build();
    event.addTeam(team);
    return event;
  }

  /** Members of one server who currently have the raider role. */
  private static final class FakeDiscord implements MemberRolePort {
    private final Set<String> roles = new HashSet<>();
    private boolean failing;

    @Override
    public boolean hasRole(String serverId, String userId, String roleId) {
      return roles.contains(userId);
    }

    @Override
    public void addRole(String serverId, String userId, String roleId) {
      if (failing) {
        throw new IllegalStateException("Missing Manage Roles permission");
      }
      roles.add(userId);
    }

    @Override
    public void removeRole(String serverId, String userId, String roleId) {
      roles.remove(userId);
    }
  }

  private static final class FakeGrants implements RaiderRoleGrantApplicationPort {
    private final List<RaiderRoleGrant> grants = new ArrayList<>();
    private final AtomicLong ids = new AtomicLong();

    List<String> users(Long eventId) {
      return findByEventId(eventId).stream().map(RaiderRoleGrant::getUserId).toList();
    }

    @Override
    public List<RaiderRoleGrant> findByEventId(Long eventId) {
      return grants.stream().filter(grant -> grant.getEventId().equals(eventId)).toList();
    }

    @Override
    public List<RaiderRoleGrant> findReleasableBefore(Instant now) {
      return grants.stream().filter(grant -> grant.getReleaseAfter().isBefore(now)).toList();
    }

    @Override
    public long countGrants(String serverId, String userId, String roleId) {
      return grants.stream().filter(grant -> grant.getServerId().equals(serverId)
          && grant.getUserId().equals(userId) && grant.getRoleId().equals(roleId)).count();
    }

    @Override
    public RaiderRoleGrant save(RaiderRoleGrant grant) {
      if (grant.getId() != null) {
        return grant;
      }
      RaiderRoleGrant saved = RaiderRoleGrant.builder().id(ids.incrementAndGet()).eventId(grant.getEventId())
          .serverId(grant.getServerId()).userId(grant.getUserId()).roleId(grant.getRoleId())
          .backup(grant.isBackup()).releaseAfter(grant.getReleaseAfter()).build();
      grants.add(saved);
      return saved;
    }

    @Override
    public void deleteById(Long id) {
      grants.removeIf(grant -> grant.getId().equals(id));
    }
  }
}
