package de.qf0xb.raidsignupbot.infrastructure.jpa.template;

import de.qf0xb.raidsignupbot.infrastructure.jpa.server.ServerEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "event_template")
public class EventTemplateEntity implements Serializable {
  private static final long serialVersionUID = 4152287054673818041L;

  @Id
  @Column(name = "template_key", nullable = false)
  private String key;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "server_id", referencedColumnName = "server_id")
  private ServerEntity server;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "details", nullable = false)
  private String details;

  @Column(name = "emoji_slug", nullable = false)
  private String emojiSlug;

  @OneToMany(mappedBy = "eventTemplateEntity", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id")
  private Set<EventStrategyEntity> eventStrategies = new LinkedHashSet<>();
}
