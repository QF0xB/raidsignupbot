package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command;

import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.domain.model.server.Server;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommandPermissionServiceTest {
  private final CommandPermissionService service = new CommandPermissionService();
  private final Member member = mock(Member.class);
  private final Server server = new Server("server");

  @BeforeEach
  void setUp() {
    service.serverService = mock(ServerService.class);
    Guild guild = mock(Guild.class);
    when(guild.getId()).thenReturn("server");
    when(member.getGuild()).thenReturn(guild);
    when(service.serverService.findServerByIdOptional("server")).thenReturn(Optional.of(server));
    server.setEventManagerRoleId("event-managers");
  }

  @Test
  void grantsEventAccessThroughTheConfiguredRole() {
    Role role = mock(Role.class);
    when(role.getId()).thenReturn("event-managers");
    when(member.getRoles()).thenReturn(List.of(role));

    assertTrue(service.isMemberAllowed(member, CommandAccessLevel.MANAGE_EVENTS));
    assertFalse(service.isMemberAllowed(member, CommandAccessLevel.MANAGE_TEMPLATES));
    assertFalse(service.isMemberAllowed(member, CommandAccessLevel.ADMIN));
  }

  @Test
  void grantsEverythingToServerManagers() {
    when(member.hasPermission(Permission.MANAGE_SERVER)).thenReturn(true);

    assertTrue(service.isMemberAllowed(member, CommandAccessLevel.ADMIN));
  }

  @Test
  void deniesMembersWithoutRoleAndUsersOutsideTheServer() {
    when(member.getRoles()).thenReturn(List.of());

    assertFalse(service.isMemberAllowed(member, CommandAccessLevel.MANAGE_EVENTS));
    assertFalse(service.isMemberAllowed(null, CommandAccessLevel.MANAGE_EVENTS));
    assertTrue(service.isMemberAllowed(null, CommandAccessLevel.PUBLIC));
  }
}
