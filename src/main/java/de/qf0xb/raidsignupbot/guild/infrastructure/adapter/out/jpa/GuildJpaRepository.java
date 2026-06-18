package de.qf0xb.raidsignupbot.guild.infrastructure.adapter.out.jpa;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GuildJpaRepository implements PanacheRepositoryBase<GuildEntity, String> {}
