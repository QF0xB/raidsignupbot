package de.qf0xb.raidsignupbot.event.infrastructure.adapter.out.jpa;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "event_strategy")
public class EventStrategyEntity {
  @Id
  @Column(name = "slug", nullable = false)
  private String slug;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "template_slug", nullable = false)
  private EventTemplateEntity template;

  @Column(name = "name", nullable = false)
  private String name;

  @ElementCollection
  @CollectionTable(name = "event_strategy_guide", joinColumns = @JoinColumn(name = "strategy_slug"))
  @Column(name = "guide", nullable = false)
  private List<String> guides = new ArrayList<>();

  @OneToMany(mappedBy = "strategy", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<EventRoleEntity> roles = new ArrayList<>();

  public void addRole(EventRoleEntity role) {
    role.setStrategy(this);
    roles.add(role);
  }
}
