package de.qf0xb.raidsignupbot.infrastructure.adapter.in.web;

import de.qf0xb.raidsignupbot.application.port.EventApplicationPort;
import de.qf0xb.raidsignupbot.application.service.calendar.RaidCalendarService;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import net.dv8tion.jda.api.JDA;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Path("/")
@Authenticated
@Blocking
@Produces(MediaType.TEXT_HTML)
public class DashboardResource {
  @CheckedTemplate
  static class Templates {
    static native TemplateInstance dashboard(String userName, List<EventView> approvals, List<ServerCard> servers);
  }

  /** One server on the dashboard; {@code hostsEntries} lets hosts without a bot role create their events. */
  public record ServerCard(WebAccessService.ServerAccess access, boolean hostsEntries, List<EventView> events) {
    public boolean canCreateEvent() {
      return access.manageGuilds() || hostsEntries;
    }
  }

  private final CurrentUser currentUser;
  private final WebAccessService accessService;
  private final EventApplicationPort eventApplicationPort;
  private final EventViewFactory eventViewFactory;
  private final RaidCalendarService calendarService;
  private final JDA api;

  public DashboardResource(
      CurrentUser currentUser,
      WebAccessService accessService,
      EventApplicationPort eventApplicationPort,
      EventViewFactory eventViewFactory,
      RaidCalendarService calendarService,
      JDA api) {
    this.currentUser = currentUser;
    this.accessService = accessService;
    this.eventApplicationPort = eventApplicationPort;
    this.eventViewFactory = eventViewFactory;
    this.calendarService = calendarService;
    this.api = api;
  }

  @GET
  public TemplateInstance dashboard() {
    String userId = currentUser.id();
    List<Event> upcoming = eventApplicationPort.findAllUpcomingEvents().stream()
        .sorted(Comparator.comparing(Event::getStart, Comparator.nullsLast(Instant::compareTo)))
        .toList();

    List<EventView> approvals = upcoming.stream()
        .filter(Event::isApprovalPending)
        .filter(event -> event.getEventAdmin() != null && userId.equals(event.getEventAdmin().getUserId()))
        .map(eventViewFactory::create)
        .toList();

    List<ServerCard> servers = api.getGuilds().stream()
        .map(server -> {
          WebAccessService.ServerAccess access = accessService.serverAccess(server.getId(), userId);
          boolean hostsEntries = calendarService.findUpcomingAvailableEntries(server.getId()).stream()
              .anyMatch(entry -> userId.equals(entry.getHosterUserId()));
          if (!access.any() && !hostsEntries) {
            return null;
          }
          List<EventView> events = access.manageEvents()
              ? upcoming.stream().filter(event -> server.getId().equals(event.getGuild().getServerId()))
                  .map(eventViewFactory::create).toList()
              : List.<EventView>of();
          return new ServerCard(access, hostsEntries, events);
        })
        .filter(Objects::nonNull)
        .sorted(Comparator.comparing(card -> card.access().name(), String.CASE_INSENSITIVE_ORDER))
        .toList();
    return Templates.dashboard(currentUser.displayName(), approvals, servers);
  }
}
