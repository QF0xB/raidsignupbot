package de.qf0xb.raidsignupbot.infrastructure.jpa.template;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EventTemplateEntityRepository implements PanacheRepositoryBase<EventTemplateEntity, String> {
}
