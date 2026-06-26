package de.qf0xb.raidsignupbot.application.port;

import de.qf0xb.raidsignupbot.application.TeamGeneratorResult;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventMemberSignup;

import java.util.List;

public interface EventUIApplicationPort {
  void addEvent(Event event);
  void updateEvent(Event existingEvent, Event updatedEvent);
  List<EventMemberSignup> closeSignup(Event event);
  void removeSignup(Event event, String userId);
  void publishTeams(Event event, TeamGeneratorResult result);
  void updateTeams(Event event, TeamGeneratorResult result);
  void deleteEvent(Event event);

}
