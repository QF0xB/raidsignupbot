package de.qf0xb.raidsignupbot.domain.model.template;

import de.qf0xb.raidsignupbot.domain.model.server.Server;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@Getter
@Setter
public class EventTemplate {
  private final String key;
  private String name;
  private String details;
  private Server server;
  private String emojiSlug;

  @Getter(AccessLevel.NONE)
  private final Set<EventStrategy> strategies = new LinkedHashSet<>();

  public EventTemplate(String key, String name, String details, String emojiSlug) {
    this.key = key;
    this.name = name;
    this.details = details;
    this.emojiSlug = emojiSlug;
  }

  public String getServerId() {
    return server == null ? null : server.getServerId();
  }

  public void setServerId(String serverId) {
    if (server == null) {
      server = new Server(serverId);
    } else {
      server.setServerId(serverId);
    }
  }

  public void addStrategy(EventStrategy strategy) {
    strategies.add(strategy);
  }

  public void removeStrategy(String strategyKey) {
    strategies.removeIf(s -> s.getKey().equals(strategyKey));
  }

  public Optional<EventStrategy> findStrategy(String strategyKey) {
    return strategies.stream().filter(strategy -> strategy.getKey().equals(strategyKey)).findFirst();
  }

  public EventStrategy requireStrategy(String strategyKey) {
    return findStrategy(strategyKey)
        .orElseThrow(() -> new IllegalStateException("Strategy " + strategyKey + " does not exist"));
  }

  public Set<EventStrategy> getStrategies() {
    return Collections.unmodifiableSet(strategies);
  }

  public Optional<EventStrategy> getStrategy(String strategyKey) {
    return strategies.stream().filter(strategy -> strategy.getKey().equals(strategyKey)).findFirst();
  }
}
