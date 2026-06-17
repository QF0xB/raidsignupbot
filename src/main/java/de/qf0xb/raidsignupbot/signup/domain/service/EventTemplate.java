package de.qf0xb.raidsignupbot.signup.domain.service;

import java.util.ArrayList;
import java.util.List;

public class EventTemplate {
  private final Long id;
  private final String name;
  private final List<StrategyTemplate> strategies = new ArrayList<>();

  public EventTemplate(Long id, String name) {
    this.id = id;
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public List<StrategyTemplate> getStrategies() {
    return strategies;
  }

  public void addStrategy(StrategyTemplate strategy) {
    strategies.add(strategy);
  }

  public void removeStrategy(StrategyTemplate strategy) {
    strategies.remove(strategy);
  }
}
