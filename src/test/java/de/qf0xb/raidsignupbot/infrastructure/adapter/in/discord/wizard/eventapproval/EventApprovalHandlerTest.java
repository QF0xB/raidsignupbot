package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.eventapproval;

import de.qf0xb.raidsignupbot.application.service.event.EventService;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventApprovalHandlerTest {
  @Test
  void answersButtonsOfDeletedEventsInsteadOfFailing() {
    EventService eventService = mock(EventService.class);
    when(eventService.findEventByIdOptional(2L)).thenReturn(Optional.empty());
    ButtonInteractionEvent interaction = mock(ButtonInteractionEvent.class, RETURNS_DEEP_STUBS);
    when(interaction.getButton().getCustomId()).thenReturn("event-approval:2:approve");

    EventApprovalHandler handler = new EventApprovalHandler();
    handler.eventService = eventService;

    handler.onButtonInteraction(interaction);

    verify(interaction).editMessage("This event no longer exists.");
    verify(eventService, never()).approveGeneratedTeams(anyLong());
  }
}
