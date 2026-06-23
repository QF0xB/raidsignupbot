package de.qf0xb.raidsignupbot.infrastructure.jpa.template;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(
    name = "event_role",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_event_role_strategy_role",
        columnNames = {"event_strategy_entity_id", "role_key"}
    )
)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "DTYPE")
public class EventRoleEntity implements Serializable {
  private static final long serialVersionUID = 87724346646305889L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "role_key", nullable = false)
  private String key;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "emoji_slug", nullable = false)
  private String emojiSlug;

  @Column(name = "required_count", nullable = false)
  private int requiredCount;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_strategy_entity_id", nullable = false)
  private EventStrategyEntity eventStrategyEntity;

  @Override
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
    Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
    if (thisEffectiveClass != oEffectiveClass) return false;
    EventRoleEntity that = (EventRoleEntity) o;
    return getId() != null && Objects.equals(getId(), that.getId());
  }

  @Override
  public final int hashCode() {
    return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
  }
}
