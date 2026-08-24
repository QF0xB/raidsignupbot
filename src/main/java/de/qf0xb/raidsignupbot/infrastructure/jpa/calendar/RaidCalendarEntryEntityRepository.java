package de.qf0xb.raidsignupbot.infrastructure.jpa.calendar;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RaidCalendarEntryEntityRepository implements PanacheRepositoryBase<RaidCalendarEntryEntity, Long> {
}
