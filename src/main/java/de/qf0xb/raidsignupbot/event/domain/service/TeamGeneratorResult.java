package de.qf0xb.raidsignupbot.event.domain.service;

import de.qf0xb.raidsignupbot.event.domain.model.EventMemberSignup;
import de.qf0xb.raidsignupbot.event.domain.model.EventTeam;

import java.util.List;

public class TeamGeneratorResult {
  private final List<EventTeam> teams;
  private final List<EventMemberSignup> unmatchedSignups;

  public TeamGeneratorResult(List<EventTeam> teams, List<EventMemberSignup> unmatchedSignups) {
    this.teams = List.copyOf(teams);
    this.unmatchedSignups = List.copyOf(unmatchedSignups);
  }

  public List<EventTeam> getTeams() {
    return teams;
  }

  public List<EventMemberSignup> getUnmatchedSignups() {
    return unmatchedSignups;
  }

  public void applyPriorityUpdates() {
    teams.stream()
        .flatMap(team -> team.getAssignments().stream())
        .filter(assignment -> assignment.getMember() != null)
        .map(assignment -> assignment.getMember())
        .distinct()
        .forEach(member -> member.assignedRole());
    unmatchedSignups.forEach(signup -> signup.getGuildMember().increasePriority());
  }
}
