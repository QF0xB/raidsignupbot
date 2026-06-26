package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.stepmodalwizard;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.interactions.InteractionHook;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class StepModalWizardSession {
  @Getter
  private final String sessionId;
  @Getter
  private final String userId;
  @Getter
  private final Map<String, String> values = new LinkedHashMap<>();
  @Getter
  private int stepIndex;
  private Instant expiresAt;

  @Getter
  @Setter
  private InteractionHook interactionHook;

  public StepModalWizardSession(String userId, Duration ttl) {
    this.sessionId = UUID.randomUUID().toString().replace("-", "");
    this.userId = Objects.requireNonNull(userId, "userId must not be null");
    this.stepIndex = 0;
    touch(ttl);
  }

  public String requireValue(String key) {
    String value = values.get(key);
    if (value == null) {
      throw new IllegalStateException("Missing wizard value: " + key);
    }
    return value;
  }

  public void advanceStep() {
    this.stepIndex++;
  }

  public InteractionHook requireInteractionHook() {
    return Objects.requireNonNull(interactionHook, "interactionHook must not be null");
  }

  public void putValue(String key, String value) {
    values.put(key, value);
  }

  public void touch(Duration ttl) {
    this.expiresAt = Instant.now().plus(ttl);
  }

  public boolean isExpired() {
    return Instant.now().isAfter(expiresAt);
  }

  public void clear() {
    interactionHook.deleteOriginal().queue();
  }
}
