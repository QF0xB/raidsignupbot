package de.qf0xb.raidsignupbot.infrastructure.adapter.in.web;

import de.qf0xb.raidsignupbot.application.service.event.EventService;
import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.calendar.RaidCalendarPublisher;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.time.Duration;
import java.util.function.Predicate;

/** Web counterpart of the event slash commands and the approval buttons; changes are mirrored to Discord. */
@Slf4j
@Path("/events/{eventId}")
@Authenticated
@Blocking
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
public class EventResource {
  private static final int MAX_POSTPONE_DAYS = 365;

  @CheckedTemplate
  static class Templates {
    static native TemplateInstance event(
        String userName, EventView event, WebAccessService.EventPermissions can, String message, String error);
  }

  private final CurrentUser currentUser;
  private final WebAccessService accessService;
  private final EventService eventService;
  private final EventViewFactory eventViewFactory;
  private final RaidCalendarPublisher calendarPublisher;
  private final ServerService serverService;

  public EventResource(
      CurrentUser currentUser,
      WebAccessService accessService,
      EventService eventService,
      EventViewFactory eventViewFactory,
      RaidCalendarPublisher calendarPublisher,
      ServerService serverService) {
    this.currentUser = currentUser;
    this.accessService = accessService;
    this.eventService = eventService;
    this.eventViewFactory = eventViewFactory;
    this.calendarPublisher = calendarPublisher;
    this.serverService = serverService;
  }

  @GET
  @Produces(MediaType.TEXT_HTML)
  public TemplateInstance show(
      @PathParam("eventId") Long eventId,
      @QueryParam("message") String message,
      @QueryParam("error") String error) {
    Event event = findEvent(eventId);
    WebAccessService.EventPermissions can = permissions(event, WebAccessService.EventPermissions::view);
    return Templates.event(currentUser.displayName(), eventViewFactory.create(event), can, message, error);
  }

  /**
   * Swaps two boxes, identified by their {@code team:slot} ids. A pending proposal is edited by the
   * event admin like in the approval DM; published teams need event-manager access like /swapteam.
   */
  @POST
  @Path("/swap")
  public Response swap(
      @PathParam("eventId") Long eventId,
      @FormParam("leftSlot") String leftSlot,
      @FormParam("rightSlot") String rightSlot) {
    Event event = findEvent(eventId);
    boolean proposal = event.isApprovalPending();
    permissions(event, can -> proposal ? can.eventAdmin() : can.manage());
    return run(eventId, () -> {
      EventView view = eventViewFactory.create(event);
      String leftUserId = assignedUser(view, leftSlot);
      String rightUserId = assignedUser(view, rightSlot);
      if (proposal) {
        eventService.swapTeamsForApproval(eventId, leftUserId, rightUserId, leftSlot, rightSlot);
      } else {
        eventService.swapTeamMembers(eventId, leftUserId, rightUserId, leftSlot, rightSlot);
      }
      return "Swapped " + leftSlot + " and " + rightSlot + ".";
    });
  }

  @POST
  @Path("/approve")
  public Response approve(@PathParam("eventId") Long eventId) {
    permissions(findEvent(eventId), WebAccessService.EventPermissions::eventAdmin);
    return run(eventId, () -> {
      eventService.approveGeneratedTeams(eventId);
      return "Teams approved and published.";
    });
  }

  @POST
  @Path("/reject")
  public Response reject(@PathParam("eventId") Long eventId) {
    permissions(findEvent(eventId), WebAccessService.EventPermissions::eventAdmin);
    return run(eventId, () -> {
      eventService.rejectGeneratedTeams(eventId, currentUser.id());
      return "Teams rejected. Signups were reopened.";
    });
  }

  @POST
  @Path("/close-signup")
  public Response closeSignup(@PathParam("eventId") Long eventId) {
    permissions(findEvent(eventId), WebAccessService.EventPermissions::manage);
    return run(eventId, () -> {
      eventService.generateTeamsAndCloseSignup(eventId, null);
      return "Signup closed and teams generated.";
    });
  }

  @POST
  @Path("/remove-signup")
  public Response removeSignup(@PathParam("eventId") Long eventId, @FormParam("userId") String userId) {
    permissions(findEvent(eventId), WebAccessService.EventPermissions::manage);
    return run(eventId, () -> {
      if (userId == null || userId.isBlank()) {
        throw new IllegalArgumentException("Select a player to remove.");
      }
      eventService.removeSignupAndRegenerateTeams(eventId, userId);
      return "Removed the player's signup and regenerated the teams.";
    });
  }

  /** Postpones by whole days; /postponeraid works in minutes, both move start and end alike. */
  @POST
  @Path("/postpone")
  public Response postpone(@PathParam("eventId") Long eventId, @FormParam("days") Integer days) {
    Event event = findEvent(eventId);
    permissions(event, WebAccessService.EventPermissions::manage);
    String serverId = event.getGuild().getServerId();
    return run(eventId, () -> {
      if (days == null || days < 1 || days > MAX_POSTPONE_DAYS) {
        throw new IllegalArgumentException("Postpone by 1 to " + MAX_POSTPONE_DAYS + " days.");
      }
      eventService.postponeEvent(serverId, eventId, (int) Duration.ofDays(days).toMinutes());
      return "Postponed by " + days + (days == 1 ? " day." : " days.") + refreshCalendar(serverId);
    });
  }

  @POST
  @Path("/close")
  public Response close(@PathParam("eventId") Long eventId) {
    permissions(findEvent(eventId), WebAccessService.EventPermissions::manage);
    return run(eventId, () -> {
      eventService.closeEvent(eventId);
      return "Event closed and its channels removed.";
    });
  }

  @POST
  @Path("/delete")
  public Response delete(@PathParam("eventId") Long eventId) {
    permissions(findEvent(eventId), WebAccessService.EventPermissions::admin);
    try {
      eventService.deleteEventEverywhere(eventId);
      return Response.seeOther(URI.create("/")).build();
    } catch (RuntimeException ex) {
      log.error("Web delete failed for event {}", eventId, ex);
      return redirect(eventId, "error", "Could not delete the event: " + ex.getMessage());
    }
  }

  private Event findEvent(Long eventId) {
    return eventService.findEventByIdOptional(eventId)
        .orElseThrow(() -> new NotFoundException("Event not found."));
  }

  private WebAccessService.EventPermissions permissions(
      Event event, Predicate<WebAccessService.EventPermissions> required) {
    WebAccessService.EventPermissions can = accessService.eventPermissions(event, currentUser.id());
    if (!required.test(can)) {
      throw new ForbiddenException("You are not allowed to do this for this event.");
    }
    return can;
  }

  private Response run(Long eventId, Action action) {
    try {
      return redirect(eventId, "message", action.run());
    } catch (IllegalArgumentException | IllegalStateException ex) {
      return redirect(eventId, "error", ex.getMessage());
    } catch (RuntimeException ex) {
      log.error("Web action failed for event {}", eventId, ex);
      return redirect(eventId, "error", "Something went wrong: " + ex.getMessage());
    }
  }

  private String refreshCalendar(String serverId) {
    if (serverService.findServerById(serverId).getRaidCalendarChannelId() == null) {
      return "";
    }
    try {
      calendarPublisher.publish(serverId);
      return "";
    } catch (RuntimeException ex) {
      log.warn("Could not refresh the raid calendar of server {}", serverId, ex);
      return " The calendar message could not be refreshed.";
    }
  }

  private String assignedUser(EventView view, String slotId) {
    return view.assignedSlots().stream()
        .filter(slot -> slot.id().equals(slotId))
        .map(EventView.SlotView::userId)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Select two assigned boxes to swap."));
  }

  private Response redirect(Long eventId, String parameter, String text) {
    return Response.seeOther(UriBuilder.fromPath("/events/{id}").queryParam(parameter, text).build(eventId)).build();
  }

  @FunctionalInterface
  private interface Action {
    String run();
  }
}
