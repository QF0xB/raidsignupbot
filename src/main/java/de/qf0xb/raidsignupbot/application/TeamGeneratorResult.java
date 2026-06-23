package de.qf0xb.raidsignupbot.application;

import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventMemberSignup;
import de.qf0xb.raidsignupbot.domain.model.event.EventSlot;
import de.qf0xb.raidsignupbot.domain.model.event.EventTeam;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;

import java.util.List;

public record TeamGeneratorResult(List<EventTeam> teams,List<EventMemberSignup> unmatchedSignups) {
  public void applyPriorityUpdates() {
    teams.stream()
        .flatMap(team -> team.getSlots().stream())
        .filter(EventSlot::isAssigned)
        .map(EventSlot::getMember)
        .distinct()
        .forEach(GuildMember::assignedRole);
    unmatchedSignups.forEach(signup -> signup.getGuildMember().increasePriority());
  }
}
