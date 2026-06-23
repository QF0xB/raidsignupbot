package de.qf0xb.raidsignupbot.infrastructure.jpa.guild;

import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
public class GuildMemberEntityRepository implements PanacheRepositoryBase<GuildMemberEntity, String> {
}
