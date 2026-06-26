package de.qf0xb.raidsignupbot.infrastructure.mapper.jpa;

import de.qf0xb.raidsignupbot.domain.model.server.Server;
import de.qf0xb.raidsignupbot.infrastructure.jpa.server.ServerEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface ServerEntityMapper extends JpaEntityMapper<ServerEntity, Server> {
  ServerEntity toEntity(Server server);

  Server toDomain(ServerEntity serverEntity);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  ServerEntity partialUpdate(Server server, @MappingTarget ServerEntity serverEntity);
}
