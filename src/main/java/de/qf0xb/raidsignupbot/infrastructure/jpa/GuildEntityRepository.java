package de.qf0xb.raidsignupbot.infrastructure.jpa;

import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
public class GuildEntityRepository implements PanacheRepositoryBase<GuildEntity, String> {
}
