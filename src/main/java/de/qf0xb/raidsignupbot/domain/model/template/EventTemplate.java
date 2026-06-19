package de.qf0xb.raidsignupbot.domain.model.template;

import lombok.*;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
public class EventTemplate {
  private final String key;
  private String name;
  private String details;

  @Getter(AccessLevel.NONE)
  private final Set<EventStrategy> strategies = new LinkedHashSet<>();

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
}
