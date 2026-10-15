package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.eventbackup;

import de.qf0xb.raidsignupbot.application.service.event.EventBackupService;
import de.qf0xb.raidsignupbot.application.service.event.EventService;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.ComponentInteractionHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

import java.util.List;

@Slf4j
@ApplicationScoped
public class EventBackupHandler implements ComponentInteractionHandler {
  private static final String PREFIX = "backup-offer:";

  @Inject
  EventBackupService eventBackupService;

  @Inject
  EventService eventService;

  @Override
  public boolean supportsButtonId(String buttonId) {
    return buttonId.startsWith(PREFIX);
  }

  @Override
  public void onButtonInteraction(ButtonInteractionEvent event) {
    ParsedOffer parsed = parse(event.getButton().getCustomId());
    event.deferEdit().queue();
    try {
      String response = eventService.respondToBackupOffer(
          parsed.offerId(),
          event.getUser().getId(),
          parsed.accepted());
      event.getHook().editOriginal(response)
          .setComponents(List.of())
          .queue();
    } catch (RuntimeException ex) {
      log.warn("Could not process backup response {}", parsed.offerId(), ex);
      event.getHook().editOriginal(ex.getMessage() == null ? "This backup request is no longer available." : ex.getMessage())
          .setComponents(List.of())
          .queue();
    }
  }

  private ParsedOffer parse(String customId) {
    String[] parts = customId.substring(PREFIX.length()).split(":", 2);
    if (parts.length != 2) {
      throw new IllegalArgumentException("Invalid backup offer button: " + customId);
    }
    return new ParsedOffer(parts[0], "accept".equalsIgnoreCase(parts[1]));
  }

  @Override
  public void onModalInteraction(ModalInteractionEvent event) {
  }

  @Override
  public boolean supportsModalId(String modalId) {
    return false;
  }

  @Override
  public void onStringSelectInteraction(StringSelectInteractionEvent event) {
  }

  @Override
  public boolean supportsStringSelectId(String stringSelectId) {
    return false;
  }

  @Override
  public void onEntitySelectInteraction(EntitySelectInteractionEvent event) {
  }

  @Override
  public boolean supportsEntitySelectId(String entitySelectId) {
    return false;
  }

  @Override
  public void cleanupExpiredSessions() {
    eventBackupService.cleanupExpiredOffers();
  }

  private record ParsedOffer(String offerId, boolean accepted) {
  }
}
