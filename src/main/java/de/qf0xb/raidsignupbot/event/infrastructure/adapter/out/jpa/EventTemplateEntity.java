package de.qf0xb.raidsignupbot.event.infrastructure.adapter.out.jpa;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "event_template")
public class EventTemplateEntity {
  @Id
  @Column(name = "slug", nullable = false)
  private String slug;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "details", nullable = false, length = 4000)
  private String details;

  @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<EventStrategyEntity> strategies = new ArrayList<>();

  public void addStrategy(EventStrategyEntity strategy) {
    strategy.setTemplate(this);
    strategies.add(strategy);
  }
}
