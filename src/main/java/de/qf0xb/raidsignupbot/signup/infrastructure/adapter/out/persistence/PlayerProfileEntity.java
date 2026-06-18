package de.qf0xb.raidsignupbot.signup.infrastructure.adapter.out.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "player_profile")
@Getter
@Setter
public class PlayerProfileEntity extends PanacheEntityBase {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false, unique = true)
  private String userId;

  @Column(name = "priority_score", nullable = false)
  private int priorityScore;

  public PlayerProfileEntity() {
    this.priorityScore = 0;
  }

  public PlayerProfileEntity(String userId, int priorityScore) {
    this.userId = userId;
    this.priorityScore = priorityScore;
  }
}
