package de.qf0xb.raidsignupbot.infrastructure.jpa.template;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(
    name = "event_strategy",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_event_strategy_template_strategy",
        columnNames = {"event_template_entity_template_key", "strategy_key"}
    )
)
public class EventStrategyEntity implements Serializable {
  private static final long serialVersionUID = 1141158147512002762L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "strategy_key", nullable = false)
  private String key;

  @Column(name = "name", nullable = false)
  private String name;

  @ElementCollection
  @Column(name = "guide")
  @CollectionTable(name = "event_strategy_entity_guides", joinColumns = @JoinColumn(name = "owner_id"))
  @OrderColumn(name = "guide_order")
  private List<String> guides = new ArrayList<>();

  @OneToMany(mappedBy = "eventStrategyEntity", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id")
  private Set<EventRoleEntity> eventRoleEntities = new LinkedHashSet<>();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_template_entity_template_key", nullable = false)
  private EventTemplateEntity eventTemplateEntity;

}
