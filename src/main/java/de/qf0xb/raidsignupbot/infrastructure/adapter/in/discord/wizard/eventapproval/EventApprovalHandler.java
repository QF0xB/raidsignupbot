package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.eventapproval;

import de.qf0xb.raidsignupbot.application.service.event.EventService;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventTeam;
import de.qf0xb.raidsignupbot.domain.model.event.EventSlot;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.ComponentInteractionHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.Component;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.modals.Modal;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@ApplicationScoped
public class EventApprovalHandler implements ComponentInteractionHandler {
  private static final String PREFIX = "event-approval:";
  private static final String SWAP_MODAL_PREFIX = "event-approval-swap:";
  private static final String LEFT_SLOT_FIELD = "left-position";
  private static final String RIGHT_SLOT_FIELD = "right-position";
  private static final int MAX_SELECT_OPTIONS = 25;

  @Inject
  EventService eventService;

  @Inject
  JDA api;

  @Override
  public boolean supportsButtonId(String buttonId) {
    return buttonId.startsWith(PREFIX);
  }

  @Override
  public void onButtonInteraction(ButtonInteractionEvent event) {
    ParsedApproval parsed = parse(event.getButton().getCustomId());
    var storedEventOptional = eventService.findEventByIdOptional(parsed.eventId());
    if (storedEventOptional.isEmpty()) {
      event.editMessage("This event no longer exists.")
          .setEmbeds(List.of())
          .setComponents(List.of())
          .queue();
      return;
    }
    var storedEvent = storedEventOptional.get();
    if (!storedEvent.isApprovalPending()) {
      event.reply("This approval request is no longer pending.")
          .setEphemeral(true)
          .queue();
      return;
    }

    String expectedUserId = storedEvent.getEventAdmin().getUserId();
    if (!expectedUserId.equals(event.getUser().getId())) {
      event.reply("Only the event admin can approve or reject these teams.")
          .setEphemeral(true)
          .queue();
      return;
    }

    if (parsed.swap()) {
      List<ProposalSlot> slots = proposalSlots(storedEvent);
      if (slots.stream().map(ProposalSlot::userId).distinct().count() < 2) {
        event.reply("At least two different players must be assigned before you can swap boxes.")
            .setEphemeral(true).queue();
        return;
      }
      event.replyModal(buildSwapModal(parsed.eventId(), slots)).queue();
      return;
    }

    event.deferEdit().queue();
    try {
      if (parsed.approve()) {
        eventService.approveGeneratedTeams(parsed.eventId());
        event.getHook().editOriginal("Teams approved and published.")
            .setComponents(List.of())
            .queue();
      } else {
        eventService.rejectGeneratedTeams(parsed.eventId());
        notifyRejection(storedEvent.getSignUpChannelId(), event.getUser().getAsMention());
        event.getHook().editOriginal("Teams rejected. Signups were reopened.")
            .setComponents(List.of())
            .queue();
      }
    } catch (RuntimeException ex) {
      log.error("Failed to handle approval action for event {}", parsed.eventId(), ex);
      event.getHook().editOriginal("Failed to handle approval action: " + failureMessage(ex))
          .setComponents(List.of())
          .queue();
    }
  }

  private void notifyRejection(String signUpChannelId, String moderatorMention) {
    if (signUpChannelId == null || signUpChannelId.isBlank()) {
      return;
    }

    TextChannel textChannel = api.getTextChannelById(signUpChannelId);
    if (textChannel != null) {
      textChannel.sendMessage("Teams were rejected by " + moderatorMention + ". Signups are open again.")
          .queue();
    }
  }

  private ParsedApproval parse(String customId) {
    if (!customId.startsWith(PREFIX)) {
      throw new IllegalArgumentException("Invalid approval custom id: " + customId);
    }

    String[] parts = customId.substring(PREFIX.length()).split(":", 2);
    if (parts.length != 2) {
      throw new IllegalArgumentException("Invalid approval custom id: " + customId);
    }

    String action = parts[1].toLowerCase();
    return new ParsedApproval(
        Long.parseLong(parts[0]),
        "approve".equals(action),
        "swap".equals(action));
  }

  private String failureMessage(RuntimeException ex) {
    return ex.getMessage() == null || ex.getMessage().isBlank() ? ex.getClass().getSimpleName() : ex.getMessage();
  }

  @Override
  public void onModalInteraction(ModalInteractionEvent event) {
    ParsedSwap parsed = parseSwapModal(event.getModalId());
    long eventId = parsed.eventId();
    var storedEventOptional = eventService.findEventByIdOptional(eventId);
    if (storedEventOptional.isEmpty()) {
      event.reply("This event no longer exists.").setEphemeral(true).queue();
      return;
    }
    var storedEvent = storedEventOptional.get();
    if (!storedEvent.isApprovalPending()) {
      event.reply("This approval request is no longer pending.")
          .setEphemeral(true)
          .queue();
      return;
    }

    String expectedUserId = storedEvent.getEventAdmin().getUserId();
    if (!expectedUserId.equals(event.getUser().getId())) {
      event.reply("Only the event admin can change these teams.")
          .setEphemeral(true)
          .queue();
      return;
    }

    event.deferReply().setEphemeral(true).queue();
    try {
      List<ProposalSlot> slots = proposalSlots(storedEvent);
      if (!fingerprint(slots).equals(parsed.fingerprint())) {
        throw new IllegalArgumentException("The proposed teams changed or this swap form is out of date. Reopen Swap members from the latest proposal.");
      }
      ProposalSlot left = selectedSlot(event, LEFT_SLOT_FIELD, slots);
      ProposalSlot right = selectedSlot(event, RIGHT_SLOT_FIELD, slots);
      if (left.userId().equals(right.userId())) {
        throw new IllegalArgumentException("Choose boxes belonging to two different players.");
      }
      eventService.swapTeamsForApproval(eventId, left.userId(), right.userId(), left.id(), right.id());
      if (event.getMessage() != null) {
        event.getMessage().editMessage("This proposal was updated. Use the latest approval message.")
            .setComponents(List.of()).queue();
      }
      event.getHook().sendMessage("Teams updated. A refreshed approval proposal was sent to you.").queue();
    } catch (RuntimeException ex) {
      if (ex instanceof IllegalArgumentException) {
        log.debug("Could not change approval teams for event {}: {}", eventId, failureMessage(ex));
      } else {
        log.error("Failed to change approval teams for event {}", eventId, ex);
      }
      event.getHook().sendMessage("Failed to change teams: " + failureMessage(ex)).queue();
    }
  }

  @Override
  public boolean supportsModalId(String modalId) {
    return modalId.startsWith(SWAP_MODAL_PREFIX);
  }

  @Override
  public boolean supportsStringSelectId(String stringSelectId) {
    return false;
  }

  @Override
  public void onStringSelectInteraction(StringSelectInteractionEvent event) {
  }

  @Override
  public boolean supportsEntitySelectId(String entitySelectId) {
    return false;
  }

  @Override
  public void onEntitySelectInteraction(EntitySelectInteractionEvent event) {
  }

  @Override
  public void cleanupExpiredSessions() {
  }

  private Modal buildSwapModal(long eventId, List<ProposalSlot> slots) {
    Modal.Builder modal = Modal.create(SWAP_MODAL_PREFIX + eventId + ":" + fingerprint(slots), "Swap proposed boxes");
    if (slots.size() <= MAX_SELECT_OPTIONS) {
      List<SelectOption> options = slots.stream().map(slot -> {
        User user = api.getUserById(slot.userId());
        String name = user == null ? slot.userId() : user.getEffectiveName();
        return SelectOption.of(truncate(slot.id() + " · " + name + " · " + slot.roleName()), slot.value())
            .withDescription(truncate("Team " + slot.id().split(":")[0] + " · " + slot.strategyName()));
      }).toList();
      modal.addComponents(
          Label.of("First box to swap", StringSelectMenu.create(LEFT_SLOT_FIELD).addOptions(options).setRequired(true).build()),
          Label.of("Second box to swap", StringSelectMenu.create(RIGHT_SLOT_FIELD).addOptions(options).setRequired(true).build()));
    } else {
      // Discord limits each select to 25 options; slot IDs keep every box selectable in large raids.
      modal.addComponents(
          Label.of("First box's slot ID (team:slot)", TextInput.create(LEFT_SLOT_FIELD, TextInputStyle.SHORT)
              .setRequired(true).setPlaceholder("From the latest proposal, e.g. 1:2").build()),
          Label.of("Second box's slot ID (team:slot)", TextInput.create(RIGHT_SLOT_FIELD, TextInputStyle.SHORT)
              .setRequired(true).setPlaceholder("From the latest proposal, e.g. 2:1").build()));
    }
    return modal.build();
  }

  private ProposalSlot selectedSlot(ModalInteractionEvent event, String field, List<ProposalSlot> slots) {
    var mapping = event.getValue(field);
    if (mapping == null) {
      throw new IllegalArgumentException("Choose both boxes to swap.");
    }
    if (mapping.getType() == Component.Type.STRING_SELECT) {
      List<String> values = mapping.getAsStringList();
      if (values.size() != 1) {
        throw new IllegalArgumentException("Choose exactly one box in each selector.");
      }
      return slots.stream().filter(slot -> slot.value().equals(values.getFirst())).findFirst()
          .orElseThrow(() -> new IllegalArgumentException("The selected box is no longer in this proposal."));
    }
    if (mapping.getType() == Component.Type.TEXT_INPUT) {
      String id = mapping.getAsString().trim();
      return slots.stream().filter(slot -> slot.id().equals(id)).findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Slot " + id + " was not found. Use a team:slot ID from the latest proposal."));
    }
    throw new IllegalArgumentException("Reopen Swap members from the latest proposal.");
  }

  private List<ProposalSlot> proposalSlots(Event event) {
    List<ProposalSlot> result = new ArrayList<>();
    for (int teamIndex = 0; teamIndex < event.getTeams().size(); teamIndex++) {
      EventTeam team = event.getTeams().get(teamIndex);
      for (int slotIndex = 0; slotIndex < team.getSlots().size(); slotIndex++) {
        EventSlot slot = team.getSlots().get(slotIndex);
        if (slot.isAssigned()) {
          List<String> utilities = team.utilitiesForSlot(slot).stream().map(utility -> utility.getKey()).toList();
          result.add(new ProposalSlot((teamIndex + 1) + ":" + (slotIndex + 1), slot.getMember().getUserId(),
              slot.getRoleName(), team.getStrategy().getName(), utilities));
        }
      }
    }
    return result;
  }

  private String fingerprint(List<ProposalSlot> slots) {
    return UUID.nameUUIDFromBytes(slots.toString().getBytes(StandardCharsets.UTF_8)).toString();
  }

  private String truncate(String label) {
    return label.length() <= 100 ? label : label.substring(0, 99) + "…";
  }

  private ParsedSwap parseSwapModal(String modalId) {
    if (!modalId.startsWith(SWAP_MODAL_PREFIX)) {
      throw new IllegalArgumentException("Invalid approval modal id: " + modalId);
    }
    String[] parts = modalId.substring(SWAP_MODAL_PREFIX.length()).split(":", 2);
    return new ParsedSwap(Long.parseLong(parts[0]), parts.length == 2 ? parts[1] : null);
  }

  private record ProposalSlot(String id, String userId, String roleName, String strategyName, List<String> utilities) {
    private String value() {
      return id + ":" + userId;
    }
  }

  private record ParsedSwap(long eventId, String fingerprint) {}
  private record ParsedApproval(long eventId, boolean approve, boolean swap) {}
}
