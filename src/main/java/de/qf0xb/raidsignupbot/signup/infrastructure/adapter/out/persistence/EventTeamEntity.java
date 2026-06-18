package de.qf0xb.raidsignupbot.signup.infrastructure.adapter.out.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "event_team")
@Getter
@Setter
public class EventTeamEntity extends PanacheEntityBase {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "event_id")
  private Long eventId;

  @Column(name = "strategy_id")
  private Long strategyId;

  @Column(name = "strategy_name", nullable = false)
  private String strategyName;

  @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  private final List<EventAssignedRoleEntity> assignedRoles = new ArrayList<>();

  public void addAssignedRole(EventAssignedRoleEntity assignedRole) {
    assignedRole.setTeam(this);
    assignedRoles.add(assignedRole);
  }
}
