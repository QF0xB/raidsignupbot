package de.qf0xb.raidsignupbot.template.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "event_template_entity")
public class EventTemplateEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "stub", nullable = false, unique = true)
  private String stub;

  @Column(name = "name", nullable = false)
  private String name;

}
