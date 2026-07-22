package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.editorwizard;

import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.ComponentInteractionHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericSelectMenuInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.List;

@Slf4j
public abstract class EditorWorkflow<T> implements ComponentInteractionHandler {
  private static final Duration DEFAULT_TTL = Duration.ofMinutes(15);

  private final Map<String, EditorSession<T>> sessions = new LinkedHashMap<>();

  @Getter
  private final String workflowName;

  public EditorWorkflow(String workflowName) {
    this.workflowName = workflowName;
  }

  public abstract EditorScreen<T> initialScreen(EditorSession<T> session);
  public abstract EmbedBuilder onFinish(EditorSession<T> session);

  public final void start(IReplyCallback callback, T initialData) {
    EditorSession<T> session = new EditorSession<>(callback.getUser().getId(), initialData, DEFAULT_TTL);
    session.push(initialScreen(session));
    sessions.put(session.getSessionId(), session);
    sendCurrent(callback, session);
  }

  private final void sendCurrent(IReplyCallback callback, EditorSession<T> session) {
    EditorScreen<T> screen = session.current();

    List<ActionRow> actionRows = new ArrayList<>(screen.actionRows(session));
    if (!actionRows.isEmpty()) {
      actionRows.add(buildUtilityRow(session));
    }

    if (session.getInteractionHook() != null) {
      session.getInteractionHook()
          .editOriginalEmbeds(screen.embed(session).build())
          .setComponents(actionRows)
          .queue();
    } else {
      callback
          .replyEmbeds(screen.embed(session).build())
          .addComponents(actionRows)
          .queue(session::setInteractionHook);
    }
  }

  private ActionRow buildUtilityRow(EditorSession<T> session) {
    Button cancelButton = Button.danger(buildButtonId(session, session.current(), "cancel"), "Cancel");
    Button backButton = Button.secondary(buildButtonId(session, session.current(), "back"), "Back");
    Button finishButton = Button.success(buildButtonId(session, session.current(), "finish"), "Finish");

    if (session.getStack().size() == 1) {
      backButton = backButton.asDisabled();
    }

    if (!session.hasUnsavedChanges()) {
      finishButton = finishButton.asDisabled();
    }

    return ActionRow.of(
        cancelButton,
        backButton,
        finishButton
    );
  }

  private final void finish(EditorSession<T> session, EmbedBuilder completionMessage) {
    InteractionHook hook = session.getInteractionHook();

    hook.editOriginalEmbeds(completionMessage.build())
        .setComponents(List.of())
        .queue();
  }

  @Override
  public void onModalInteraction(ModalInteractionEvent event) {
    log.info("Received modal interaction: {}", event.getModalId());
    ParsedEntity parsed = parseId(event.getModalId()).orElse(null);
    if (parsed == null) {
      return;
    }

    EditorSession<T> session = sessions.get(parsed.sessionId());
    if (!validate(event, session, parsed.screenId())) {
      return;
    }

    String action = parsed.action();
    EditorScreen<T> screen = session.current();
    try {
      screen.onModal(action, event, session);
      session.touch(DEFAULT_TTL);
    } catch (RuntimeException ex) {
      event.reply(ex.getMessage())
          .setEphemeral(true)
          .queue();
      return;
    }

    event.deferEdit().queue(ignored -> sendCurrent(event, session));
  }

  @Override
  public void onButtonInteraction(ButtonInteractionEvent event) {
    log.debug("Received button interaction: {}", event.getButton().getCustomId());
    ParsedEntity parsed = parseId(event.getButton().getCustomId()).orElse(null);
    if (parsed == null) {
      return;
    }

    EditorSession<T> session = sessions.get(parsed.sessionId());
    if (!validate(event, session, parsed.screenId())) {
      return;
    }

    String action = parsed.action();

    if (action.equals("cancel")) {
      sessions.remove(session.getSessionId());
      session.getInteractionHook().deleteOriginal().queue();
      EmbedBuilder embed = buildEmbedBase("Editor cancelled.", session);
      embed.setColor(Color.RED);
      event.deferEdit().queue(ignored -> finish(session, embed));
      return;
    }

    if (action.equals("back")) {
      if (session.getStack().size() > 1) {
        session.pop();
      }
      event.deferEdit().queue(ignored -> sendCurrent(event, session));
      return;
    }

    if (action.equals("finish")) {
      EmbedBuilder result = onFinish(session);
      event.deferEdit().queue(ignored -> finish(session, result));
      return;
    }

    EditorScreen<T> screen = session.current();
    try {
      boolean rerender = screen.onButton(action, event, session);
      if (rerender) {
        event.deferEdit().queue(ignored -> sendCurrent(event, session));
      }
      session.touch(DEFAULT_TTL);
    } catch (RuntimeException ex) {
      event.reply(ex.getMessage())
          .setEphemeral(true)
          .queue();
    }
  }

  @Override
  public void onStringSelectInteraction(StringSelectInteractionEvent event) {
    log.debug("Received string select interaction: {}", event.getInteraction().getCustomId());
    ParsedEntity parsed = parseId(event.getInteraction().getCustomId()).orElse(null);
    if (parsed == null) {
      return;
    }

    EditorSession<T> session = sessions.get(parsed.sessionId());
    if (!validate(event, session, parsed.screenId())) {
      return;
    }

    String action = parsed.action();

    if (action.equals("cancel")) {
      sessions.remove(session.getSessionId());
      session.getInteractionHook().deleteOriginal().queue();
      EmbedBuilder embed = buildEmbedBase("Editor cancelled.", session);
      embed.setColor(Color.RED);
      event.deferEdit().queue(ignored -> finish(session, embed));
      return;
    }

    if (action.equals("back")) {
      if (session.getStack().size() > 1) {
        session.pop();
      }
      event.deferEdit().queue(ignored -> sendCurrent(event, session));
      return;
    }

    if (action.equals("finish")) {
      EmbedBuilder result = onFinish(session);
      event.deferEdit().queue(ignored -> finish(session, result));
      return;
    }

    EditorScreen<T> screen = session.current();
    try {
      boolean rerender = screen.onStringSelect(action, event, session);
      if (rerender) {
        event.deferEdit().queue(ignored -> sendCurrent(event, session));
      }
      session.touch(DEFAULT_TTL);
    } catch (RuntimeException ex) {
      event.reply(ex.getMessage())
          .setEphemeral(true)
          .queue();
    }
  }

  @Override
  public void onEntitySelectInteraction(EntitySelectInteractionEvent event) {
    log.info("Received entity select interaction: {}", event.getInteraction().getCustomId());
    ParsedEntity parsed = parseId(event.getInteraction().getCustomId()).orElse(null);
    if (parsed == null) {
      return;
    }

    EditorSession<T> session = sessions.get(parsed.sessionId());
    if (!validate(event, session, parsed.screenId())) {
      return;
    }

    String action = parsed.action();

    if (action.equals("cancel")) {
      sessions.remove(session.getSessionId());
      session.getInteractionHook().deleteOriginal().queue();
      EmbedBuilder embed = buildEmbedBase("Editor cancelled.", session);
      embed.setColor(Color.RED);
      event.deferEdit().queue(ignored -> finish(session, embed));
      return;
    }

    if (action.equals("back")) {
      if (session.getStack().size() > 1) {
        session.pop();
      }
      event.deferEdit().queue(ignored -> sendCurrent(event, session));
      return;
    }

    if (action.equals("finish")) {
      EmbedBuilder result = onFinish(session);
      event.deferEdit().queue(ignored -> finish(session, result));
      return;
    }

    EditorScreen<T> screen = session.current();
    try {
      boolean rerender = screen.onEntitySelect(action, event, session);
      if (rerender) {
        event.deferEdit().queue(ignored -> sendCurrent(event, session));
      }
      session.touch(DEFAULT_TTL);
    } catch (RuntimeException ex) {
      event.reply(ex.getMessage())
          .setEphemeral(true)
          .queue();
    }
  }

  private Optional<ParsedEntity> parseId(String id) {
    if (id == null || !id.startsWith(workflowName + ":")) {
      return Optional.empty();
    }

    String[] parts = id.split(":", 5);
    if (parts.length != 5) {
      return Optional.empty();
    }

    try {
      return Optional.of(new ParsedEntity(parts[1], parts[2], parts[4], parts[3]));
    } catch (NumberFormatException ex) {
      return Optional.empty();
    }
  }

  @Override
  public boolean supportsButtonId(String buttonId) {
    Optional<ParsedEntity> parsed = parseId(buttonId);
    return parseId(buttonId).isPresent() && parseId(buttonId).get().type().equalsIgnoreCase("button");
  }

  @Override
  public boolean supportsModalId(String modalId) {
    return parseId(modalId).isPresent() && parseId(modalId).get().type().equalsIgnoreCase("modal");
  }

  @Override
  public boolean supportsEntitySelectId(String entitySelectId) {
    return parseId(entitySelectId).isPresent() && parseId(entitySelectId).get().type().equals("entity-select");
  }

  @Override
  public boolean supportsStringSelectId(String stringSelectId) {
    return parseId(stringSelectId).isPresent() && parseId(stringSelectId).get().type().equals("string-select");
  }

  private final String buildButtonId(EditorSession<T> session, EditorScreen<T> screen, String action) {
    return buildScreenId(session, screen) + ":button:" + action;
  }

  private final String buildScreenId(EditorSession<T> session, EditorScreen<T> screen) {
    return workflowName + ":" + session.getSessionId() + ":" + screen.id();
  }

  private boolean validate(IReplyCallback event, EditorSession<T> session, String screenId) {
    if (session == null) {
      event.reply("This editor session has expired or was already completed.")
          .setEphemeral(true)
          .queue();
      return false;
    }

    if (!session.getUserId().equalsIgnoreCase(event.getUser().getId())) {
      event.reply("This editor session belongs to somebody else.")
          .setEphemeral(true)
          .queue();
      return false;
    }

    if (session.isExpired()) {
      event.reply("This editor session has expired.").setEphemeral(true).queue();
      return false;
    }

    return session.current() != null && Objects.equals(session.current().id(), screenId);
  }

  public EmbedBuilder buildEmbedBase(String title, EditorSession<T> session) {
    EmbedBuilder builder = new EmbedBuilder()
        .setAuthor("Made by onyxguildbot | by StormFox")
        .setTitle(title)
        .setFooter("Session ID: " + session.getSessionId() + (session.hasUnsavedChanges() ? " (Unsaved changes)" : ""))
        .setTimestamp(Instant.now());

    if (session.hasUnsavedChanges()) {
      builder.setColor(Color.YELLOW);
    } else {
      builder.setColor(Color.BLUE);
    }
    return builder;
  }

  @Override
  public void cleanupExpiredSessions() {
    sessions.values().removeIf(session -> {
      if (session.isExpired()) {
        if(session.getInteractionHook() != null) {
          session.getInteractionHook().deleteOriginal().queue();
        }
        return true;
      }
      return false;
    });
  }

  public record ParsedEntity(String sessionId, String screenId, String action, String type) {}
}
