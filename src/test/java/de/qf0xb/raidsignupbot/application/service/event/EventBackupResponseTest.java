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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        new EventMemberSignup(new GuildMember("first"), "p1"),
        new EventMemberSignup(new GuildMember("second"), "p1"),
        new EventMemberSignup(new GuildMember("decliner"), "p1"))));
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
    verify(uiPort, never()).announceBackup(any(), any());

    String accepted = service.respondToBackupOffer(firstOffer, "first", true);
    assertTrue(accepted.startsWith("Thanks!"), accepted);
    verify(uiPort).announceBackup(event, backupService.findAcceptedOffers(7L).getFirst());

    String tooLate = service.respondToBackupOffer(secondOffer, "second", true);
    assertTrue(tooLate.contains("already accepted"), tooLate);
    service.respondToBackupOffer(firstOffer, "first", true);
    verify(uiPort, times(1)).announceBackup(any(), any());
    assertEquals(List.of("first"), backupService.findAcceptedOffers(7L).stream().map(EventBackupOffer::userId).toList());
  }

  private String offerFor(EventBackupService backupService, String userId) {
    return backupService.findOffers(7L).stream()
        .filter(offer -> offer.userId().equals(userId))
        .findFirst()
        .orElseThrow()
        .offerId();
  }
}
