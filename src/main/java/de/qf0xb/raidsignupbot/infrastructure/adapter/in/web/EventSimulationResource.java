package de.qf0xb.raidsignupbot.infrastructure.adapter.in.web;

import de.qf0xb.raidsignupbot.application.service.event.EventLifecycleManager;
import de.qf0xb.raidsignupbot.application.service.event.EventLifecycleStage;
import de.qf0xb.raidsignupbot.application.service.event.EventService;
import de.qf0xb.raidsignupbot.application.service.generator.TeamSimulationService;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventMemberSignup;
import de.qf0xb.raidsignupbot.infrastructure.config.WebConfig;
import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Test mode for one raid: fills it with simulated players and runs the team creation, or sends a
 * reminder early. Everything is posted to the raid's real Discord channels, so it is only available
 * when {@code web.simulation.enabled} is set.
 */
@Path("/events/{eventId}/simulate")
@Authenticated
@Blocking
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
public class EventSimulationResource {
  private static final Map<String, EventLifecycleStage> REMINDERS = Map.of(
      "1h", EventLifecycleStage.ONE_HOUR_BEFORE,
      "30m", EventLifecycleStage.THIRTY_MINUTES_BEFORE,
      "15m", EventLifecycleStage.FIFTEEN_MINUTES_BEFORE);

  private final CurrentUser currentUser;
  private final WebAccessService accessService;
  private final EventService eventService;
  private final EventLifecycleManager lifecycleManager;
  private final TeamSimulationService simulationService;
  private final WebConfig webConfig;

  public EventSimulationResource(
      CurrentUser currentUser,
      WebAccessService accessService,
      EventService eventService,
      EventLifecycleManager lifecycleManager,
      TeamSimulationService simulationService,
      WebConfig webConfig) {
    this.currentUser = currentUser;
    this.accessService = accessService;
    this.eventService = eventService;
    this.lifecycleManager = lifecycleManager;
    this.simulationService = simulationService;
    this.webConfig = webConfig;
  }

  @POST
  @Path("/teams")
  public Response createTeams(
      @PathParam("eventId") Long eventId,
      @FormParam("players") @DefaultValue("40") int players,
      @FormParam("minRoles") @DefaultValue("1") int minRoles,
      @FormParam("maxRoles") @DefaultValue("3") int maxRoles,
      @FormParam("utilityPercent") @DefaultValue("30") int utilityPercent,
      @FormParam("backupPercent") @DefaultValue("10") int backupPercent,
      @FormParam("dualBoxPercent") @DefaultValue("5") int dualBoxPercent,
      @FormParam("randomPriority") boolean randomPriority,
      @FormParam("includeRealSignups") boolean includeRealSignups,
      @FormParam("seed") Long seed) {
    Event event = requireTestMode(eventId);
    return WebForms.run(path(eventId), () -> {
      long usedSeed = seed == null ? ThreadLocalRandom.current().nextLong(1, 1_000_000) : seed;
      List<EventMemberSignup> signups = simulationService.createSignups(event.getTemplate(),
          new TeamSimulationService.Settings(players, minRoles, maxRoles, utilityPercent, backupPercent,
              dualBoxPercent, randomPriority, usedSeed));
      Event result = eventService.simulateTeamCreation(eventId, signups, includeRealSignups);
      return (result.isApprovalPending()
          ? "Teams created from " + players + " simulated players and sent for approval"
          : "Teams created from " + players + " simulated players and published in Discord")
          + " (seed " + usedSeed + ").";
    });
  }

  @POST
  @Path("/reminder")
  public Response sendReminder(@PathParam("eventId") Long eventId, @FormParam("stage") String stage) {
    requireTestMode(eventId);
    return WebForms.run(path(eventId), () -> {
      EventLifecycleStage reminder = REMINDERS.get(stage);
      if (reminder == null) {
        throw new IllegalArgumentException("Choose the 1h, 30 min, or 15 min reminder.");
      }
      lifecycleManager.sendReminderNow(eventId, reminder);
      return "Sent the " + stage + " reminder to Discord.";
    });
  }

  private Event requireTestMode(Long eventId) {
    if (!webConfig.isSimulationEnabled()) {
      throw new NotFoundException();
    }
    Event event = eventService.findEventByIdOptional(eventId).orElseThrow(() -> new NotFoundException("Event not found."));
    if (!accessService.eventPermissions(event, currentUser.id()).manage()) {
      throw new jakarta.ws.rs.ForbiddenException("You are not allowed to do this for this event.");
    }
    return event;
  }

  private static String path(Long eventId) {
    return "/events/" + eventId;
  }
}
