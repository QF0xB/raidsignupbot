package de.qf0xb.raidsignupbot.event.infrastructure.adapter.out.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "event_role")
public class EventRoleEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "strategy_slug", nullable = false)
  private EventStrategyEntity strategy;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "required_count", nullable = false)
  private int requiredCount;

  @Column(name = "utility", nullable = false)
  private boolean utility;

  @ManyToMany
  @JoinTable(
      name = "event_role_eligible_role",
      joinColumns = @JoinColumn(name = "utility_role_id"),
      inverseJoinColumns = @JoinColumn(name = "eligible_role_id")
  )
  private Set<EventRoleEntity> eligibleRoles = new LinkedHashSet<>();
}
