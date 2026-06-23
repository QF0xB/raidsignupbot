package de.qf0xb.raidsignupbot.infrastructure.jpa.event;

import de.qf0xb.raidsignupbot.infrastructure.jpa.template.EventStrategyEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "event_team")
public class EventTeamEntity implements Serializable {
  private static final long serialVersionUID = -1232618509049004218L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_id", nullable = false)
  private EventEntity event;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_strategy_id", nullable = false)
  private EventStrategyEntity eventStrategy;

  @OneToMany(mappedBy = "eventTeam", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<EventTeamAssignmentEntity> assignments = new LinkedHashSet<>();
}
