package de.qf0xb.raidsignupbot.application.port.server;

import de.qf0xb.raidsignupbot.domain.model.server.Server;

import java.util.Optional;

public interface ServerApplicationPort {
  Optional<Server> findByServerId(String serverId);

  Server save(Server server);
}
