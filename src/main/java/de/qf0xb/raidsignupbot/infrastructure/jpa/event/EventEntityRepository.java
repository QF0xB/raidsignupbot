package de.qf0xb.raidsignupbot.infrastructure.jpa.event;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EventEntityRepository implements PanacheRepositoryBase<EventEntity, Long> {
}
