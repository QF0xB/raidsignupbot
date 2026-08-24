package de.qf0xb.raidsignupbot.application.service.event;

import de.qf0xb.raidsignupbot.application.port.EventApplicationPort;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.adapter.DiscordEventUIApplicationAdapter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventLifecycleManagerTest {
  @Test
  void emitsOneHourBeforeReminderForClosedEvents() {
    EventApplicationPort eventApplicationPort = mock(EventApplicationPort.class);
    EventService eventService = mock(EventService.class);
    @SuppressWarnings("unchecked")
    jakarta.enterprise.event.Event<EventLifecycleNotification> cdiEvent = mock(jakarta.enterprise.event.Event.class);
    DiscordEventUIApplicationAdapter discordEventUIApplicationAdapter = mock(DiscordEventUIApplicationAdapter.class);

    Event event = Event.builder()
        .id(42L)
        .guild(new Guild("guild-1", "Guild"))
        .hoster(new GuildMember("hoster-1"))
        .template(new EventTemplate("template-1", "Template", "Details", "test"))
        .start(Instant.now().plusSeconds(2700))
        .end(Instant.now().plusSeconds(9900))
        .build();
    event.setSignupOpen(false);

    when(eventApplicationPort.findAllUpcomingEvents()).thenReturn(Set.of());
    when(eventApplicationPort.findAllChannelExistingEvents()).thenReturn(new LinkedHashSet<>(Set.of(event)));
    when(eventApplicationPort.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

    EventLifecycleManager manager = new EventLifecycleManager(
        eventApplicationPort,
        eventService,
        cdiEvent,
        discordEventUIApplicationAdapter);

    manager.runLifecycleChecks();

    ArgumentCaptor<EventLifecycleNotification> notificationCaptor =
        ArgumentCaptor.forClass(EventLifecycleNotification.class);
    verify(cdiEvent).fire(notificationCaptor.capture());
    verify(eventApplicationPort).save(eq(event));
    assertTrue(event.isOneHourBeforeEmitted());
    assertEquals(EventLifecycleStage.ONE_HOUR_BEFORE, notificationCaptor.getValue().stage());
  }
}
