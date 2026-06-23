package de.qf0xb.raidsignupbot.application.port;

import de.qf0xb.raidsignupbot.application.service.event.EventLifecycleStage;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventMemberSignup;

import java.util.List;

public interface EventSignupChannelApplicationPort {
  List<EventMemberSignup> getSignupsByMessageId(Event event);
  void createSignupChannel(Event event);
  void closeSignup(Event event);
  void sendReminder(Event event, EventLifecycleStage stage);
  void removeSignupChannel(Event event);
}
