package de.qf0xb.raidsignupbot.infrastructure.adapter.in.web;

import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventSlot;
import de.qf0xb.raidsignupbot.domain.model.event.EventTeam;
import de.qf0xb.raidsignupbot.domain.model.event.EventUtilityAssignment;
import de.qf0xb.raidsignupbot.domain.model.guild.SimulatedPlayer;
import jakarta.enterprise.context.ApplicationScoped;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class EventViewFactory {
  private static final DateTimeFormatter START_FORMAT = DateTimeFormatter.ofPattern("EEE dd.MM.yyyy HH:mm");

  private final JDA api;

  public EventViewFactory(JDA api) {
    this.api = api;
  }

  public EventView create(Event event) {
    Guild server = api.getGuildById(event.getGuild().getServerId());
    List<EventView.TeamView> teams = new ArrayList<>();
    for (int teamIndex = 0; teamIndex < event.getTeams().size(); teamIndex++) {
      EventTeam team = event.getTeams().get(teamIndex);
      List<EventView.SlotView> slots = new ArrayList<>();
      for (int slotIndex = 0; slotIndex < team.getSlots().size(); slotIndex++) {
        EventSlot slot = team.getSlots().get(slotIndex);
        String userId = slot.isAssigned() ? slot.getMember().getUserId() : null;
        List<String> utilities = slot.isAssigned()
            ? team.utilitiesForSlot(slot).stream().map(EventUtilityAssignment::getRoleName).toList()
            : List.of();
        slots.add(new EventView.SlotView((teamIndex + 1) + ":" + (slotIndex + 1), slot.getRoleName(),
            userId, displayName(server, userId), utilities));
      }
      teams.add(new EventView.TeamView(teamIndex + 1, team.getStrategy().getName(), slots));
    }

    return new EventView(
        event.getId(),
        event.getTemplate() == null ? "Raid" : event.getTemplate().getName(),
        event.getGuild().getName(),
        event.getStart() == null ? "—" : START_FORMAT.format(event.getStart().atZone(ZoneId.systemDefault())),
        event.getStart() == null || event.getEnd() == null ? "—" : event.getDuration(),
        status(event),
        event.getEventAdmin() == null ? "—" : displayName(server, event.getEventAdmin().getUserId()),
        event.isSignupOpen(),
        event.isTeamsGenerated(),
        event.isApprovalPending(),
        teams);
  }

  private String status(Event event) {
    if (event.isApprovalPending()) {
      return "Teams awaiting approval";
    }
    if (event.isTeamsGenerated()) {
      return "Teams published";
    }
    return event.isSignupOpen() ? "Signup open" : "Signup closed";
  }

  private String displayName(Guild server, String userId) {
    if (userId == null) {
      return null;
    }
    if (SimulatedPlayer.isSimulated(userId)) {
      return SimulatedPlayer.displayName(userId);
    }
    Member member = server == null ? null : server.getMemberById(userId);
    return member == null ? userId : member.getEffectiveName();
  }
}
