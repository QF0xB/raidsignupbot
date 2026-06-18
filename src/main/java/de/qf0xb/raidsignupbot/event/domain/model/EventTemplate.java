package de.qf0xb.raidsignupbot.event.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class EventTemplate {
  private String slug;
  private String name;
  private String details;

  private final List<EventStrategy> strategies = new ArrayList<>();

  public void addStrategy(EventStrategy strategy) {
    strategies.add(strategy);
  }

  public void removeStrategy(EventStrategy strategy) {
    strategies.remove(strategy);
  }

  public boolean hasStrategy(String slug) {
    return strategies.stream().anyMatch(s -> s.getSlug().equals(slug));
  }
}
