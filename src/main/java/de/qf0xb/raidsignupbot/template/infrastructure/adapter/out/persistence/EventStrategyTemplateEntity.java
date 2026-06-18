package de.qf0xb.raidsignupbot.template.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "event_strategy_template_entity")
public class EventStrategyTemplateEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "stub", unique = true, nullable = false)
  private String stub;

  @Column(name = "name", nullable = false)
  private String name;


}
