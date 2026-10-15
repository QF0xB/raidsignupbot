package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.eventapproval;

import de.qf0xb.raidsignupbot.application.service.event.EventService;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventTeam;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.Component;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventApprovalHandlerTest {
  private final EventService service = mock(EventService.class);
  private final EventApprovalHandler handler = new EventApprovalHandler();

  EventApprovalHandlerTest() {
    handler.eventService = service;
    handler.api = mock(JDA.class);
  }

  @Test
  void readsRealSelectValuesAndSwapsOneSpecificBox() {
    Modal modal = openSwap(raid(3));
    ModalInteractionEvent interaction = submission(modal);
    when(interaction.getValue("left-position")).thenReturn(selection("left-position", "1:2:dual"));
    when(interaction.getValue("right-position")).thenReturn(selection("right-position", "1:3:user-2"));
    Message oldProposal = mock(Message.class, RETURNS_DEEP_STUBS);
    when(interaction.getMessage()).thenReturn(oldProposal);

    handler.onModalInteraction(interaction);

    verify(service).swapTeamsForApproval(7L, "dual", "user-2", "1:2", "1:3");
    verify(oldProposal).editMessage("This proposal was updated. Use the latest approval message.");
    verify(oldProposal.editMessage("This proposal was updated. Use the latest approval message."))
        .setComponents(List.of());
    verify(interaction.getHook()).sendMessage("Teams updated. A refreshed approval proposal was sent to you.");
  }

  @Test
  void showsEachBoxAsASeparateRosterChoice() {
    Modal modal = openSwap(raid(3));
    var options = modal.toData().getArray("components").getObject(0).getObject("component").getArray("options");
    assertEquals(3, options.length());
    assertEquals("1:1:dual", options.getObject(0).getString("value"));
    assertEquals("1:2:dual", options.getObject(1).getString("value"));
    assertEquals("1:3:user-2", options.getObject(2).getString("value"));
    assertTrue(options.getObject(0).getString("label").contains("Fire"));
  }

  @Test
  void allBoxesRemainSelectableWhenTheRosterExceedsDiscordsMenuLimit() {
    Modal modal = openSwap(raid(26));
    assertEquals(Component.Type.TEXT_INPUT.getKey(),
        modal.toData().getArray("components").getObject(0).getObject("component").getInt("type"));
    ModalInteractionEvent interaction = submission(modal);
    when(interaction.getValue("left-position")).thenReturn(text("left-position", " 1:2 "));
    when(interaction.getValue("right-position")).thenReturn(text("right-position", "1:26"));

    handler.onModalInteraction(interaction);

    verify(service).swapTeamsForApproval(7L, "dual", "user-25", "1:2", "1:26");
  }

  @Test
  void rejectsAFormIfTheTeamsChangedAfterItWasOpened() {
    Event raid = raid(3);
    Modal modal = openSwap(raid);
    raid.getTeams().getFirst().getSlots().get(2).assign(new GuildMember("replacement"));
    ModalInteractionEvent interaction = submission(modal);

    handler.onModalInteraction(interaction);

    verify(service, never()).swapTeamsForApproval(anyLong(), anyString(), anyString(), any(), any());
    verify(interaction.getHook()).sendMessage(contains("proposed teams changed"));
  }

  @Test
  void rejectsBothBoxesOfTheSamePlayer() {
    ModalInteractionEvent interaction = submission(openSwap(raid(3)));
    when(interaction.getValue("left-position")).thenReturn(selection("left-position", "1:1:dual"));
    when(interaction.getValue("right-position")).thenReturn(selection("right-position", "1:2:dual"));

    handler.onModalInteraction(interaction);

    verify(service, never()).swapTeamsForApproval(anyLong(), anyString(), anyString(), any(), any());
    verify(interaction.getHook()).sendMessage(contains("two different players"));
  }

  @Test
  void rejectsMissingAndForgedChoicesWithAnAcknowledgedResponse() {
    Modal modal = openSwap(raid(3));
    ModalInteractionEvent missing = submission(modal);
    when(missing.getValue("left-position")).thenReturn(null);
    handler.onModalInteraction(missing);
    verify(missing).deferReply();
    verify(missing.getHook()).sendMessage(contains("Choose both boxes"));

    ModalInteractionEvent forged = submission(modal);
    when(forged.getValue("left-position")).thenReturn(selection("left-position", "1:2:another-user"));
    handler.onModalInteraction(forged);
    verify(forged.getHook()).sendMessage(contains("no longer in this proposal"));
    verify(service, never()).swapTeamsForApproval(anyLong(), anyString(), anyString(), any(), any());
  }

  @Test
  void onlyTheEventAdminCanSubmitTheSwap() {
    ModalInteractionEvent interaction = submission(openSwap(raid(3)));
    when(interaction.getUser().getId()).thenReturn("intruder");

    handler.onModalInteraction(interaction);

    verify(interaction).reply("Only the event admin can change these teams.");
    verify(service, never()).swapTeamsForApproval(anyLong(), anyString(), anyString(), any(), any());
  }

  @Test
  void keepsTheOriginalProposalUsableIfTheSwapFails() {
    ModalInteractionEvent interaction = submission(openSwap(raid(3)));
    when(interaction.getValue("left-position")).thenReturn(selection("left-position", "1:2:dual"));
    when(interaction.getValue("right-position")).thenReturn(selection("right-position", "1:3:user-2"));
    Message oldProposal = mock(Message.class, RETURNS_DEEP_STUBS);
    when(interaction.getMessage()).thenReturn(oldProposal);
    org.mockito.Mockito.doThrow(new IllegalArgumentException("This swap conflicts with utility duties."))
        .when(service).swapTeamsForApproval(7L, "dual", "user-2", "1:2", "1:3");

    handler.onModalInteraction(interaction);

    verify(oldProposal, never()).editMessage(anyString());
    verify(interaction.getHook()).sendMessage(contains("conflicts with utility duties"));
  }

  private Event raid(int slotCount) {
    EventStrategy strategy = new EventStrategy("main", "Main");
    strategy.addRole(new EventRole("fire", "Fire", "🔥", slotCount));
    EventTeam team = new EventTeam(strategy);
    for (int index = 0; index < slotCount; index++) {
      team.assign("fire", new GuildMember(index < 2 ? "dual" : "user-" + index));
    }
    Event event = Event.builder().id(7L).approvalPending(true).eventAdmin(new GuildMember("admin")).build();
    event.addTeam(team);
    when(service.findEventByIdOptional(7L)).thenReturn(Optional.of(event));
    return event;
  }

  private Modal openSwap(Event raid) {
    ButtonInteractionEvent button = mock(ButtonInteractionEvent.class, RETURNS_DEEP_STUBS);
    when(button.getButton().getCustomId()).thenReturn("event-approval:" + raid.getId() + ":swap");
    when(button.getUser().getId()).thenReturn("admin");
    handler.onButtonInteraction(button);
    ArgumentCaptor<Modal> modal = ArgumentCaptor.forClass(Modal.class);
    verify(button).replyModal(modal.capture());
    return modal.getValue();
  }

  private ModalInteractionEvent submission(Modal modal) {
    ModalInteractionEvent event = mock(ModalInteractionEvent.class, RETURNS_DEEP_STUBS);
    when(event.getModalId()).thenReturn(modal.getId());
    when(event.getUser().getId()).thenReturn("admin");
    when(event.getMessage()).thenReturn(null);
    return event;
  }

  private ModalMapping selection(String field, String value) {
    return new ModalMapping(null, DataObject.empty(), DataObject.empty()
        .put("id", 1).put("custom_id", field).put("type", Component.Type.STRING_SELECT.getKey())
        .put("values", List.of(value)));
  }

  private ModalMapping text(String field, String value) {
    return new ModalMapping(null, DataObject.empty(), DataObject.empty()
        .put("id", 1).put("custom_id", field).put("type", Component.Type.TEXT_INPUT.getKey()).put("value", value));
  }

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
