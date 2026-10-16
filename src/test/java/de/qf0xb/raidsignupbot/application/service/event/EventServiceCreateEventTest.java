package de.qf0xb.raidsignupbot.application.service.event;

import de.qf0xb.raidsignupbot.application.service.guild.RaiderRoleService;
import de.qf0xb.raidsignupbot.application.dto.CreateEventCommand;
import de.qf0xb.raidsignupbot.application.port.EventApplicationPort;
import de.qf0xb.raidsignupbot.application.port.EventUIApplicationPort;
import de.qf0xb.raidsignupbot.application.service.calendar.RaidCalendarService;
import de.qf0xb.raidsignupbot.application.service.guild.GuildMemberService;
import de.qf0xb.raidsignupbot.application.service.guild.GuildService;
import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.application.service.template.TemplateService;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.domain.model.guild.GuildMember;
import de.qf0xb.raidsignupbot.domain.model.server.Server;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventServiceCreateEventTest {
  @Test
  void createsMembersForHostAndAdminThatWereNeverSynced() {
    EventApplicationPort eventApplicationPort = mock(EventApplicationPort.class);
    GuildService guildService = mock(GuildService.class);
    ServerService serverService = mock(ServerService.class);
    GuildMemberService guildMemberService = mock(GuildMemberService.class);
    TemplateService templateService = mock(TemplateService.class);

    Guild guild = new Guild("Onyx Cats", "server-1");
    GuildMember hoster = new GuildMember("hoster-1");
    GuildMember admin = new GuildMember("admin-1");
    when(guildService.findGuildByServerIdAndName("server-1", "Onyx Cats")).thenReturn(guild);
    when(serverService.findServerById("server-1")).thenReturn(new Server("server-1"));
    when(guildMemberService.ensureGuildMember("hoster-1", "server-1", "Onyx Cats")).thenReturn(hoster);
    when(guildMemberService.ensureGuildMember("admin-1", "server-1", "Onyx Cats")).thenReturn(admin);
    when(templateService.findTemplateByKey("server-1", "raid"))
        .thenReturn(new EventTemplate("raid", "Raid", "Details", "coin"));
    when(eventApplicationPort.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

    EventService service = new EventService(
        eventApplicationPort,
        guildService,
        serverService,
        guildMemberService,
        templateService,
        mock(EventUIApplicationPort.class),
        mock(RaidCalendarService.class),
        new EventBackupService(),
        mock(RaiderRoleService.class));

    Event event = service.createEvent(CreateEventCommand.builder()
        .serverId("server-1")
        .calendarEntryId(1L)
        .guildName("Onyx Cats")
        .hosterUserId("hoster-1")
        .eventAdminUserId("admin-1")
        .templateKey("raid")
        .start(Instant.parse("2026-11-01T18:00:00Z"))
        .end(Instant.parse("2026-11-01T20:00:00Z"))
        .build());

    assertSame(hoster, event.getHoster());
    assertSame(admin, event.getEventAdmin());
    verify(guildMemberService, never()).findGuildMemberById(anyString());
  }
}
