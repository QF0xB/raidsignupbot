package de.qf0xb.raidsignupbot.application.port;

import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventMemberSignup;

import java.util.List;

public interface EventUIApplicationPort {
  void addEvent(Event event);
  List<EventMemberSignup> closeSignup(Event event);
  void deleteEvent(Event event);

}
