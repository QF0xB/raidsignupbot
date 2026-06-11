package de.qf0xb.raidsignupbot.shared.domain.model;

import java.util.Objects;

public class EventRole {
  private final Long id;
  private String name;

  public EventRole(String name) {
    id = -1L;
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName() {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    EventRole eventRole = (EventRole) o;
    return Objects.equals(id, eventRole.id) && Objects.equals(name, eventRole.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name);
  }
}
