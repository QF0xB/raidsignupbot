package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.editorwizard;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.UUID;

public class EditorSession<T> {
  @Getter private final String sessionId;
  @Getter private final String userId;

  @Getter private final Deque<EditorScreen<T>> stack = new ArrayDeque<>();
  @Getter @Setter private InteractionHook interactionHook;

  private Instant expiresAt;

  @Getter private T draft;

  private boolean unsavedChanges = false;

  public EditorSession(String userId, T draft, Duration ttl) {
    this.sessionId = UUID.randomUUID().toString().replace("-", "");
    this.userId = Objects.requireNonNull(userId, "userId must not be null");
    this.draft = Objects.requireNonNull(draft, "draft must not be null");
    touch(ttl);
  }

  public EditorScreen<T> current() {
    return stack.peek();
  }

  public void push(EditorScreen<T> screen) {
    stack.push(screen);
  }

  public void pop() {
    stack.pop();
  }

  public void touch(Duration ttl) {
    this.expiresAt = Instant.now().plus(ttl);
  }

  public boolean isExpired() {
    return Instant.now().isAfter(expiresAt);
  }

  public boolean hasUnsavedChanges() {
    return unsavedChanges;
  }

  public void draftChanged() {
    unsavedChanges = true;
  }

  public EmbedBuilder buildEmbedBase(String title) {
    EmbedBuilder builder = new EmbedBuilder()
        .setAuthor("Made by onyxguildbot | by StormFox")
        .setTitle(title)
        .setFooter("Session ID: " + getSessionId() + (hasUnsavedChanges() ? " (Unsaved changes)" : ""))
        .setTimestamp(Instant.now());

    if (hasUnsavedChanges()) {
      builder.setColor(Color.YELLOW);
    } else {
      builder.setColor(Color.BLUE);
    }

    return builder;
  }
}
