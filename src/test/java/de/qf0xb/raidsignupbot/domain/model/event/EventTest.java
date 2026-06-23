package de.qf0xb.raidsignupbot.domain.model.event;

import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventTest {
  @Test
  void managesTeamsAndSignupState() {
    Event event = Event.builder()
        .id(1L)
        .guild(new Guild("guild-1", "Guild"))
        .hoster(new GuildMember("hoster"))
        .template(new EventTemplate("temp", "Template", "Details", "test"))
        .build();
    EventTeam team = new EventTeam(new de.qf0xb.raidsignupbot.domain.model.template.EventStrategy("strat", "Strategy"));

    event.addTeam(team);

    assertEquals(1, event.getTeams().size());
    assertTrue(event.isSignupOpen());
    assertFalse(event.isTeamsGenerated());

    event.closeSignup();

    assertFalse(event.isSignupOpen());
    assertTrue(event.isTeamsGenerated());
    event.removeTeam(team);
    assertEquals(0, event.getTeams().size());
  }
}
