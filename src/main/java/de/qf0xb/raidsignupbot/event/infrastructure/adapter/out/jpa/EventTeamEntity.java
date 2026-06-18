package de.qf0xb.raidsignupbot.event.infrastructure.adapter.out.jpa;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Table(name = "event_team")
public class EventTeamEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_id", nullable = false)
  private EventEntity event;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "strategy_slug", nullable = false)
  private EventStrategyEntity strategy;

  @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<EventRoleAssignmentEntity> assignments = new ArrayList<>();

  public void addAssignment(EventRoleAssignmentEntity assignment) {
    assignment.setTeam(this);
    assignments.add(assignment);
  }
}
