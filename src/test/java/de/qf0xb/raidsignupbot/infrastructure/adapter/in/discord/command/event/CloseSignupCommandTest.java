package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.event;

import de.qf0xb.raidsignupbot.application.port.EventApplicationPort;
import de.qf0xb.raidsignupbot.application.service.event.EventService;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CloseSignupCommandTest {
  @Test
  void generatesTeamsFromTheEventsOwnSignups() {
    EventService eventService = mock(EventService.class);
    EventApplicationPort eventApplicationPort = mock(EventApplicationPort.class);
    Channel channel = mock(Channel.class);
    OptionMapping option = mock(OptionMapping.class);
    SlashCommandInteractionEvent interaction = mock(SlashCommandInteractionEvent.class, RETURNS_DEEP_STUBS);

    when(channel.getId()).thenReturn("channel-1");
    when(option.getAsString()).thenReturn("raid-signup");
    when(interaction.getOption("event")).thenReturn(option);
    when(eventApplicationPort.findByChannelId("channel-1"))
        .thenReturn(Optional.of(Event.builder().id(42L).build()));

    CloseSignupCommand command = new CloseSignupCommand();
    command.eventService = eventService;
    command.eventApplicationPort = eventApplicationPort;
    command.getCachedChannelsByName().put("raid-signup", channel);

    command.execute(interaction);

    // A null signup list makes EventService read the signups from the event's Discord message.
    verify(eventService).generateTeamsAndCloseSignup(eq(42L), isNull());
    verify(eventService, never()).generateTeamsAndCloseSignup(anyLong(), anyList());
  }
}
