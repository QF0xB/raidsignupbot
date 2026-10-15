package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.eventapproval;

import de.qf0xb.raidsignupbot.application.service.event.EventService;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.ComponentInteractionHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.modals.Modal;

import java.util.List;

@Slf4j
@ApplicationScoped
public class EventApprovalHandler implements ComponentInteractionHandler {
  private static final String PREFIX = "event-approval:";
  private static final String SWAP_MODAL_PREFIX = "event-approval-swap:";
  private static final String LEFT_MEMBER_FIELD = "left-member";
  private static final String RIGHT_MEMBER_FIELD = "right-member";

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
      event.replyModal(buildSwapModal(parsed.eventId())).queue();
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
    long eventId = parseSwapModalEventId(event.getModalId());
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

    String leftUserId = event.getValue(LEFT_MEMBER_FIELD).getAsString();
    String rightUserId = event.getValue(RIGHT_MEMBER_FIELD).getAsString();
    event.deferReply().setEphemeral(true).queue();
    try {
      eventService.swapTeamsForApproval(eventId, leftUserId, rightUserId);
      event.getHook().sendMessage("Teams updated. A refreshed approval proposal was sent to you.").queue();
    } catch (RuntimeException ex) {
      log.error("Failed to change approval teams for event {}", eventId, ex);
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

  private Modal buildSwapModal(long eventId) {
    EntitySelectMenu leftMemberSelect = EntitySelectMenu.create(LEFT_MEMBER_FIELD, EntitySelectMenu.SelectTarget.USER)
        .setPlaceholder("Select the first member")
        .setRequired(true)
        .build();
    EntitySelectMenu rightMemberSelect = EntitySelectMenu.create(RIGHT_MEMBER_FIELD, EntitySelectMenu.SelectTarget.USER)
        .setPlaceholder("Select the second member")
        .setRequired(true)
        .build();

    return Modal.create(SWAP_MODAL_PREFIX + eventId, "Change proposed teams")
        .addComponents(
            Label.of("First assigned member", leftMemberSelect),
            Label.of("Second assigned member", rightMemberSelect))
        .build();
  }

  private long parseSwapModalEventId(String modalId) {
    if (!modalId.startsWith(SWAP_MODAL_PREFIX)) {
      throw new IllegalArgumentException("Invalid approval modal id: " + modalId);
    }
    return Long.parseLong(modalId.substring(SWAP_MODAL_PREFIX.length()));
  }

  private record ParsedApproval(long eventId, boolean approve, boolean swap) {}
}
