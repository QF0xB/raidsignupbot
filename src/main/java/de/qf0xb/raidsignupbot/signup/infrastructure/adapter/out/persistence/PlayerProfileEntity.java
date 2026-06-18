package de.qf0xb.raidsignupbot.signup.infrastructure.adapter.out.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "player_profile")
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

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public int getPriorityScore() {
    return priorityScore;
  }

  public void setPriorityScore(int priorityScore) {
    this.priorityScore = priorityScore;
  }
}
