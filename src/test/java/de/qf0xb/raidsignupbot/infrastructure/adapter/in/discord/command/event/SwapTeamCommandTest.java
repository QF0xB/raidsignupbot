package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.event;

import de.qf0xb.raidsignupbot.application.port.EventApplicationPort;
import de.qf0xb.raidsignupbot.application.service.event.EventService;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class SwapTeamCommandTest {
  @Test
  void registersRequiredOptionsBeforeTheOptionalSlots() {
    SwapTeamCommand command = new SwapTeamCommand();

    assertDoesNotThrow(() -> Commands.slash(command.getCommandName(), command.getDescription())
        .addOptions(command.getOptions()));
  }

  @Test
  void forwardsTheSelectedBoxesToTheSwapService() {
    SwapTeamCommand command = new SwapTeamCommand();
    command.eventService = mock(EventService.class);
    command.eventApplicationPort = mock(EventApplicationPort.class);
    Channel channel = mock(Channel.class);
    when(channel.getId()).thenReturn("channel");
    command.getCachedChannelsByName().put("raid", channel);
    when(command.eventApplicationPort.findByChannelId("channel")).thenReturn(Optional.of(
        Event.builder().id(7L).guild(new Guild("Guild", "server")).build()));
    SlashCommandInteractionEvent interaction = mock(SlashCommandInteractionEvent.class, RETURNS_DEEP_STUBS);
    when(interaction.getGuild().getId()).thenReturn("server");
    when(interaction.getOption("event").getAsString()).thenReturn("raid");
    when(interaction.getOption("left_member").getAsUser().getId()).thenReturn("dual");
    when(interaction.getOption("right_member").getAsUser().getId()).thenReturn("single");
    when(interaction.getOption("left_slot").getAsString()).thenReturn("1:2");
    when(interaction.getOption("right_slot")).thenReturn(null);

    command.execute(interaction);

    verify(command.eventService).swapTeamMembers(7L, "dual", "single", "1:2", null);
  }
}
