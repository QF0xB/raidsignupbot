package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.event;

import de.qf0xb.raidsignupbot.application.port.EventApplicationPort;
import de.qf0xb.raidsignupbot.application.service.event.EventService;
import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.domain.model.server.Server;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.calendar.RaidCalendarPublisher;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.CommandAccessLevel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PostponeRaidCommandTest {
  private final PostponeRaidCommand command = new PostponeRaidCommand();

  PostponeRaidCommandTest() {
    command.eventService = mock(EventService.class);
    command.eventApplicationPort = mock(EventApplicationPort.class);
    command.serverService = mock(ServerService.class);
    command.calendarPublisher = mock(RaidCalendarPublisher.class);
  }

  @Test
  void postponesByIdWithinTheInvokingServerAndRefreshesCalendar() {
    SlashCommandInteractionEvent interaction = interaction("7");
    Event raid = raid(7L, "server", Instant.now().plusSeconds(7200));
    when(command.eventService.postponeEvent("server", 7L, 30)).thenReturn(raid);
    Server server = new Server("server");
    server.setRaidCalendarChannelId("calendar");
    when(command.serverService.findServerById("server")).thenReturn(server);

    command.execute(interaction);

    assertEquals(CommandAccessLevel.MANAGE_EVENTS, command.getAccessLevel());
    verify(command.eventService).postponeEvent("server", 7L, 30);
    verify(command.calendarPublisher).publish("server");
    verify(interaction).deferReply(true);
    verify(interaction.getHook()).sendMessage(contains("Postponed **Raid**"));
  }

  @Test
  void reportsValidationFailuresWithoutRefreshingCalendar() {
    SlashCommandInteractionEvent interaction = interaction("7");
    when(command.eventService.postponeEvent("server", 7L, 30))
        .thenThrow(new IllegalArgumentException("Only raids that have not started can be postponed."));

    command.execute(interaction);

    verify(interaction.getHook()).sendMessage("Only raids that have not started can be postponed.");
    verifyNoInteractions(command.calendarPublisher);
  }

  @Test
  void rejectsInvalidEventSelection() {
    SlashCommandInteractionEvent interaction = interaction("channel-name");
    command.execute(interaction);
    verifyNoInteractions(command.eventService);
    verify(interaction.getHook()).sendMessage("Pick a raid from the event list.");
  }

  @Test
  @SuppressWarnings("unchecked")
  void autocompleteOnlyOffersFutureRaidsFromTheCurrentServer() {
    CommandAutoCompleteInteractionEvent interaction = mock(CommandAutoCompleteInteractionEvent.class, RETURNS_DEEP_STUBS);
    when(interaction.getGuild().getId()).thenReturn("server");
    when(interaction.getFocusedOption().getName()).thenReturn("event");
    when(interaction.getFocusedOption().getValue()).thenReturn("raid");
    Event published = raid(7L, "server", Instant.now().plusSeconds(3600));
    published.closeSignup();
    when(command.eventApplicationPort.findAllEvents()).thenReturn(Set.of(
        published,
        raid(8L, "other", Instant.now().plusSeconds(3600)),
        raid(9L, "server", Instant.now().minusSeconds(3600))));

    command.autoComplete(interaction);

    ArgumentCaptor<Collection<Command.Choice>> choices = ArgumentCaptor.forClass(Collection.class);
    verify(interaction).replyChoices(choices.capture());
    assertEquals(1, choices.getValue().size());
    assertEquals("7", choices.getValue().iterator().next().getAsString());
  }

  private SlashCommandInteractionEvent interaction(String eventId) {
    SlashCommandInteractionEvent event = mock(SlashCommandInteractionEvent.class, RETURNS_DEEP_STUBS);
    when(event.getGuild().getId()).thenReturn("server");
    when(event.getOption("event").getAsString()).thenReturn(eventId);
    when(event.getOption("minutes").getAsInt()).thenReturn(30);
    return event;
  }

  private Event raid(Long id, String serverId, Instant start) {
    return Event.builder().id(id).guild(new Guild("Guild", serverId)).start(start)
        .template(new EventTemplate("raid", "Raid", "", "coin")).build();
  }
}
