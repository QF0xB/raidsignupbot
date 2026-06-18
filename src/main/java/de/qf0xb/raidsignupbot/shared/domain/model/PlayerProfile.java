package de.qf0xb.raidsignupbot.shared.domain.model;

public class PlayerProfile {
  private final String userId;
  private int priorityScore;

  public PlayerProfile(String userId) {
    this.userId = userId;
    this.priorityScore = 0;
  }

  public String getUserId() {
    return userId;
  }

  public int getPriorityScore() {
    return priorityScore;
  }

  public void setPriorityScore(int priorityScore) {
    this.priorityScore = Math.max(0, priorityScore);
  }

  public void recordMatchedSignup() {
    priorityScore = 0;
  }

  public void recordUnmatchedSignup() {
    priorityScore++;
  }
}
