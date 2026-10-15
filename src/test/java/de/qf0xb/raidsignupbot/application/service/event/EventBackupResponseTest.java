package de.qf0xb.raidsignupbot.application.service.event;

import de.qf0xb.raidsignupbot.application.TeamGeneratorResult;
import de.qf0xb.raidsignupbot.application.port.EventApplicationPort;
import de.qf0xb.raidsignupbot.application.port.EventUIApplicationPort;
import de.qf0xb.raidsignupbot.application.service.calendar.RaidCalendarService;
import de.qf0xb.raidsignupbot.application.service.guild.GuildMemberService;
import de.qf0xb.raidsignupbot.application.service.guild.GuildService;
import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.application.service.template.TemplateService;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventBackupOffer;
import de.qf0xb.raidsignupbot.domain.model.event.EventMemberSignup;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import de.qf0xb.raidsignupbot.domain.model.template.EventStrategy;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventBackupResponseTest {
  @Test
  void announcesOnlyTheFirstAcceptedBackupPerPosition() {
    EventStrategy strategy = new EventStrategy("main", "Main");
    strategy.addRole(new EventRole("p1", "P1 Feint", ":one:", 1));
    strategy.addRole(new EventRole("p2", "P2 Support", ":two:", 1));
    strategy.addRole(new EventRole("p3", "P3 Fire", ":three:", 1));
    EventTemplate template = new EventTemplate("raid", "Raid", "", "coin");
    template.addStrategy(strategy);
    Event event = Event.builder()
        .id(7L)
        .template(template)
        .hoster(new GuildMember("hoster-1"))
        .start(Instant.now().plusSeconds(3600))
        .end(Instant.now().plusSeconds(7200))
        .build();

    EventBackupService backupService = new EventBackupService();
    backupService.registerOffers(event, new TeamGeneratorResult(List.of(), List.of(
        new EventMemberSignup(new GuildMember("first"), "p1", "p2"),
        new EventMemberSignup(new GuildMember("second"), "p1", "p2"),
        new EventMemberSignup(new GuildMember("partial"), "p1", "p3"),
        new EventMemberSignup(new GuildMember("decliner"), "p1", "p2"))));
    String firstOffer = offerFor(backupService, "first");
    String secondOffer = offerFor(backupService, "second");
    String declinerOffer = offerFor(backupService, "decliner");

    EventApplicationPort eventPort = mock(EventApplicationPort.class);
    when(eventPort.findById(7L)).thenReturn(Optional.of(event));
    EventUIApplicationPort uiPort = mock(EventUIApplicationPort.class);
    EventService service = new EventService(
        eventPort,
        mock(GuildService.class),
        mock(ServerService.class),
        mock(GuildMemberService.class),
        mock(TemplateService.class),
        uiPort,
        mock(RaidCalendarService.class),
        backupService);

    service.respondToBackupOffer(declinerOffer, "decliner", false);
    assertTrue(service.respondToBackupOffer(declinerOffer, "decliner", true).contains("already been declined"));
    verify(uiPort, never()).announceBackup(any(), any());

    String accepted = service.respondToBackupOffer(firstOffer, "first", true);
    assertTrue(accepted.startsWith("Thanks!"), accepted);
    assertTrue(accepted.contains("P1 Feint") && accepted.contains("P2 Support"), accepted);
    verify(uiPort).announceBackup(event, backupService.findAcceptedOffers(7L).stream()
        .sorted(Comparator.comparing(EventBackupOffer::roleName)).toList());

    String tooLate = service.respondToBackupOffer(secondOffer, "second", true);
    assertTrue(tooLate.contains("already accepted"), tooLate);
    service.respondToBackupOffer(firstOffer, "first", true);
    verify(uiPort, times(1)).announceBackup(any(), any());
    assertEquals(List.of("first", "first"), backupService.findAcceptedOffers(7L).stream().map(EventBackupOffer::userId).toList());

    String partial = service.respondToBackupOffer(offerFor(backupService, "partial"), "partial", true);
    assertTrue(partial.contains("P3 Fire") && !partial.contains("P1 Feint"), partial);
    assertEquals(3, backupService.findAcceptedOffers(7L).size());
    verify(uiPort, times(2)).announceBackup(any(), any());
  }

  @Test
  void groupedResponseIsRestrictedToThePlayerAndEvent() {
    EventStrategy strategy = new EventStrategy("main", "Main");
    strategy.addRole(new EventRole("p1", "P1", ":one:", 1));
    strategy.addRole(new EventRole("p2", "P2", ":two:", 1));
    EventTemplate template = new EventTemplate("raid", "Raid", "", "coin");
    template.addStrategy(strategy);
    EventBackupService backupService = new EventBackupService();
    for (long id : List.of(7L, 8L)) {
      Event event = Event.builder().id(id).template(template).end(Instant.now().plusSeconds(7200)).build();
      backupService.registerOffers(event, new TeamGeneratorResult(List.of(), List.of(
          new EventMemberSignup(new GuildMember("player"), "p1", "p2"))));
    }
    String offer = offerFor(backupService, "player");
    assertThrows(IllegalArgumentException.class, () -> backupService.respond(offer, "someone-else", true));
    assertTrue(backupService.findAcceptedOffers(7L).isEmpty());
    assertEquals(2, backupService.respond(offer, "player", true).acceptedOffers().size());
    assertTrue(backupService.findAcceptedOffers(8L).isEmpty());
    for (EventBackupOffer sibling : backupService.findOffers(7L)) {
      assertTrue(backupService.respond(sibling.offerId(), "player", true).acceptedOffers().isEmpty());
    }
  }

  private String offerFor(EventBackupService backupService, String userId) {
    return backupService.findOffers(7L).stream()
        .filter(offer -> offer.userId().equals(userId))
        .findFirst()
        .orElseThrow()
        .offerId();
  }
}
