package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

public interface ComponentInteractionHandler {
  boolean supportsButtonId(String buttonId);
  void onButtonInteraction(ButtonInteractionEvent event);

  boolean supportsModalId(String modalId);
  void onModalInteraction(ModalInteractionEvent event);

  boolean supportsStringSelectId(String stringSelectId);
  void onStringSelectInteraction(StringSelectInteractionEvent event);

  boolean supportsEntitySelectId(String entitySelectId);
  void onEntitySelectInteraction(EntitySelectInteractionEvent event);

  /**
   * Remove expired interaction sessions owned by this handler.
   */
  void cleanupExpiredSessions();
}
