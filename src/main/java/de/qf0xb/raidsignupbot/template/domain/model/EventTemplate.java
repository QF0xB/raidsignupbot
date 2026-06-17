package de.qf0xb.raidsignupbot.template.domain.model;

import java.util.List;

public class EventTemplate {
  private Long id; // Created by persistence layer. Used for search
  private String name;
  private String description;
  private List<String> guides;

  private List<EventStrategyTemplate> strategies;

  public EventTemplate(String name, String description, List<String> guides, List<EventStrategyTemplate> strategies) {
    this.name = name;
    this.description = description;
    this.guides = guides;
    this.strategies = strategies;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<String> getGuides() {
    return guides;
  }

  public void addGuide(String guide) {
    guides.add(guide);
  }

  public void removeGuide(String guide) {
    guides.remove(guide);
  }

  public void setGuides(List<String> guides) {
    this.guides = guides;
  }

  public List<EventStrategyTemplate> getStrategies() {
    return strategies;
  }

  public void addStrategy(EventStrategyTemplate strategy) {
    strategies.add(strategy);
  }

  public void removeStrategy(EventStrategyTemplate strategy) {
    strategies.remove(strategy);
  }

  public void setStrategies(List<EventStrategyTemplate> strategies) {
    this.strategies = strategies;
  }
}
