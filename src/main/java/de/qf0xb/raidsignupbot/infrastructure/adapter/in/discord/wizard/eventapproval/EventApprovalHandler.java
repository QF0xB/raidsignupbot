package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.eventapproval;

import de.qf0xb.raidsignupbot.application.service.event.EventService;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.ComponentInteractionHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

import java.util.List;

@Slf4j
@ApplicationScoped
public class EventApprovalHandler implements ComponentInteractionHandler {
  private static final String PREFIX = "event-approval:";

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
    var storedEvent = eventService.findEventById(parsed.eventId());
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

    return new ParsedApproval(Long.parseLong(parts[0]), "approve".equalsIgnoreCase(parts[1]));
  }

  private String failureMessage(RuntimeException ex) {
    return ex.getMessage() == null || ex.getMessage().isBlank() ? ex.getClass().getSimpleName() : ex.getMessage();
  }

  @Override
  public void onModalInteraction(ModalInteractionEvent event) {
  }

  @Override
  public boolean supportsModalId(String modalId) {
    return false;
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

  private record ParsedApproval(long eventId, boolean approve) {}
}
