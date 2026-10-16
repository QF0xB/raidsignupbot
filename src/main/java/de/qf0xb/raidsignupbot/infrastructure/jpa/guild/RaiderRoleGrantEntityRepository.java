package de.qf0xb.raidsignupbot.infrastructure.jpa.guild;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RaiderRoleGrantEntityRepository implements PanacheRepositoryBase<RaiderRoleGrantEntity, Long> {
}
