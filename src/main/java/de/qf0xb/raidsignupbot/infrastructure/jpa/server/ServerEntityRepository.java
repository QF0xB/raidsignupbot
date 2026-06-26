package de.qf0xb.raidsignupbot.infrastructure.jpa.server;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ServerEntityRepository implements PanacheRepositoryBase<ServerEntity, String> {
}
