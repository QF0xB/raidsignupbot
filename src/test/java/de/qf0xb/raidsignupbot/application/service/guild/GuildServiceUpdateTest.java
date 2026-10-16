package de.qf0xb.raidsignupbot.application.service.guild;

import de.qf0xb.raidsignupbot.application.dto.UpdateGuildCommand;
import de.qf0xb.raidsignupbot.application.port.guild.GuildApplicationPort;
import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GuildServiceUpdateTest {
  private final GuildApplicationPort port = mock(GuildApplicationPort.class);
  private final GuildService service = new GuildService(port, mock(ServerService.class), mock(GuildMemberSyncService.class));

  @Test
  void updatesTheGuildOfTheCommandsServerWhenNamesRepeatAcrossServers() {
    Guild otherServersGuild = new Guild("Onyx", "other");
    Guild ownGuild = new Guild("Onyx", "server");
    when(port.findByName("Onyx")).thenReturn(Optional.of(otherServersGuild));
    when(port.findByServerIdAndName("server", "Onyx")).thenReturn(Optional.of(ownGuild));
    when(port.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    service.updateGuild("Onyx", UpdateGuildCommand.builder().name("Onyx Prime").serverId("server").build());

    assertEquals("Onyx Prime", ownGuild.getName());
    assertEquals("Onyx", otherServersGuild.getName());
  }
}
