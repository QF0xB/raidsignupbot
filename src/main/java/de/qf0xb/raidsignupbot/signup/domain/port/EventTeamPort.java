package de.qf0xb.raidsignupbot.signup.domain.port;

import de.qf0xb.raidsignupbot.shared.domain.model.PlayerProfile;
import de.qf0xb.raidsignupbot.signup.domain.model.EventTeam;

import java.util.List;

public interface EventTeamPort {
  List<EventTeam> getByPlayer(PlayerProfile playerProfile);
  List<EventTeam> getByPlayerId(String playerId);
  List<EventTeam> getByEventId(Long eventId);
  List<EventTeam> getByStrategyId(Long strategyId);
  EventTeam getById(Long id);
  void save(EventTeam eventTeam);
}
