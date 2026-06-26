package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.editorwizard;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.modals.Modal;

import java.time.Instant;
import java.util.List;

public interface EditorScreen<T> {
  String workflowName();

  String id();

  EmbedBuilder embed(EditorSession<T> session);
  List<ActionRow> actionRows(EditorSession<T> session);

  default boolean onButton(String action, ButtonInteractionEvent event, EditorSession<T> session) { return false; }

  default boolean onModal(String action, ModalInteractionEvent event, EditorSession<T> session) { return false; }

  default boolean onStringSelect(String action, StringSelectInteractionEvent event, EditorSession<T> session) { return false; }

  default boolean onEntitySelect(String action, EntitySelectInteractionEvent event, EditorSession<T> session) { return false; }

  default String buildButtonId(EditorSession<T> session, EditorScreen<T> screen, String action) {
    return buildId(session, screen, action, "button");
  }

  default String buildModalId(EditorSession<T> session, EditorScreen<T> screen, String action) {
    return buildId(session, screen, action, "modal");
  }

  default String buildStringSelectId(EditorSession<T> session, EditorScreen<T> screen, String action) {
    return buildId(session, screen, action, "string-select");
  }

  default String buildEntitySelectId(EditorSession<T> session, EditorScreen<T> screen, String action) {
    return buildId(session, screen, action, "entity-select");
  }

  default String buildId(EditorSession<T> session, EditorScreen<T> screen, String action, String componentType) {
    return buildScreenId(session, screen) + ":" + componentType + ":" + action;
  }

  default String buildScreenId(EditorSession<T> session, EditorScreen<T> screen) {
    return workflowName() + ":" + session.getSessionId() + ":" + screen.id();
  }
}
