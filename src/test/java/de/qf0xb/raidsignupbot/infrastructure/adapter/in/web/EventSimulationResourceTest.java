package de.qf0xb.raidsignupbot.infrastructure.adapter.in.web;

import de.qf0xb.raidsignupbot.application.service.event.EventLifecycleManager;
import de.qf0xb.raidsignupbot.application.service.event.EventLifecycleStage;
import de.qf0xb.raidsignupbot.application.service.event.EventService;
import de.qf0xb.raidsignupbot.application.service.generator.TeamSimulationService;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.infrastructure.config.WebConfig;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventSimulationResourceTest {
  private final CurrentUser currentUser = mock(CurrentUser.class);
  private final WebAccessService accessService = mock(WebAccessService.class);
  private final EventService eventService = mock(EventService.class);
  private final EventLifecycleManager lifecycleManager = mock(EventLifecycleManager.class);
  private final WebConfig webConfig = mock(WebConfig.class);
  private final EventSimulationResource resource = new EventSimulationResource(currentUser, accessService,
      eventService, lifecycleManager, mock(TeamSimulationService.class), webConfig);
  private final Event event = Event.builder().id(7L).guild(new Guild("Onyx", "server")).build();

  @BeforeEach
  void setUp() {
    when(currentUser.id()).thenReturn("manager");
    when(eventService.findEventByIdOptional(7L)).thenReturn(Optional.of(event));
    when(accessService.eventPermissions(event, "manager"))
        .thenReturn(new WebAccessService.EventPermissions(true, false, false));
  }

  @Test
  void isUnavailableUnlessTestModeIsEnabled() {
    when(webConfig.isSimulationEnabled()).thenReturn(false);

    assertThrows(NotFoundException.class, () -> resource.sendReminder(7L, "1h"));
    verifyNoInteractions(lifecycleManager);
  }

  @Test
  void sendsTheChosenReminderNow() {
    when(webConfig.isSimulationEnabled()).thenReturn(true);

    resource.sendReminder(7L, "1h");
    resource.sendReminder(7L, "30m");
    Response invalid = resource.sendReminder(7L, "after");

    verify(lifecycleManager).sendReminderNow(7L, EventLifecycleStage.ONE_HOUR_BEFORE);
    verify(lifecycleManager).sendReminderNow(7L, EventLifecycleStage.THIRTY_MINUTES_BEFORE);
    verifyNoMoreInteractions(lifecycleManager);
    assertTrue(invalid.getLocation().toString().contains("error="));
  }

  @Test
  void requiresEventManagerAccess() {
    when(webConfig.isSimulationEnabled()).thenReturn(true);
    when(accessService.eventPermissions(event, "manager"))
        .thenReturn(new WebAccessService.EventPermissions(false, false, true));

    assertThrows(ForbiddenException.class, () -> resource.sendReminder(7L, "15m"));
    assertThrows(ForbiddenException.class,
        () -> resource.createTeams(7L, 10, 1, 2, 0, 0, 0, false, false, 1L));
  }
}
