package de.qf0xb.raidsignupbot.application.event.service;

import de.qf0xb.raidsignupbot.application.event.TeamGeneratorResult;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventMemberSignup;

import java.util.List;

public interface TeamGeneratorService {
  TeamGeneratorResult generateTeams(Event event, List<EventMemberSignup> signups);
}
