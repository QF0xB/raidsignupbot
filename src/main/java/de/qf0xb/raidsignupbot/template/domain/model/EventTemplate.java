package de.qf0xb.raidsignupbot.template.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class EventTemplate {
  private String stub;
  private String name;
  private String description;

  private final List<String> guides = new ArrayList<>();
  private final List<EventStrategyTemplate> strategies = new ArrayList<>();

  public EventTemplate(String stub, String name, String description) {
    this.stub = stub;
    this.name = name;
    this.description = description;
  }

  public void addGuide(String guide) {
    guides.add(guide);
  }

  public void removeGuide(String guide) {
    guides.remove(guide);
  }

  public void clearGuides() {
    guides.clear();
  }

  public void addStrategy(EventStrategyTemplate strategy) {
    strategies.add(strategy);
  }

  public void removeStrategy(EventStrategyTemplate strategy) {
    strategies.remove(strategy);
  }

  public void clearStrategies() {
    strategies.clear();
  }
}
