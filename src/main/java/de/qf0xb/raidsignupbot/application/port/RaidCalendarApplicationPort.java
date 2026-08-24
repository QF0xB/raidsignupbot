package de.qf0xb.raidsignupbot.application.port;

import de.qf0xb.raidsignupbot.domain.model.calendar.RaidCalendarEntry;

import java.util.List;
import java.util.Optional;

public interface RaidCalendarApplicationPort {
  Optional<RaidCalendarEntry> findById(Long id);

  List<RaidCalendarEntry> findByServerId(String serverId);

  RaidCalendarEntry save(RaidCalendarEntry entry);

  void deleteById(Long id);
}
