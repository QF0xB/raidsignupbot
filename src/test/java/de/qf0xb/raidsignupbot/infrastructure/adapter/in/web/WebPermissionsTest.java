package de.qf0xb.raidsignupbot.infrastructure.adapter.in.web;

import de.qf0xb.raidsignupbot.application.port.guild.GuildApplicationPort;
import de.qf0xb.raidsignupbot.application.service.calendar.RaidCalendarService;
import de.qf0xb.raidsignupbot.application.service.guild.GuildMemberSyncService;
import de.qf0xb.raidsignupbot.application.service.guild.GuildService;
import de.qf0xb.raidsignupbot.application.service.server.ServerService;
import de.qf0xb.raidsignupbot.application.service.template.EventStrategyService;
import de.qf0xb.raidsignupbot.application.service.template.TemplateImportExportService;
import de.qf0xb.raidsignupbot.application.service.template.TemplateService;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.calendar.RaidCalendarPublisher;
import jakarta.ws.rs.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Each settings area requires the same access level as its slash commands. */
class WebPermissionsTest {
  private final CurrentUser currentUser = mock(CurrentUser.class);
  private final WebAccessService accessService = mock(WebAccessService.class);
  private final RaidCalendarService calendarService = mock(RaidCalendarService.class);
  private final GuildMemberSyncService syncService = mock(GuildMemberSyncService.class);
  private final TemplateService templateService = mock(TemplateService.class);
  private final CalendarResource calendar = new CalendarResource(currentUser, accessService, calendarService,
      mock(RaidCalendarPublisher.class), mock(ServerService.class), mock(GuildApplicationPort.class), templateService,
      mock(DiscordDirectory.class));
  private final SettingsResource settings = new SettingsResource(currentUser, accessService, mock(ServerService.class),
      mock(GuildService.class), mock(GuildApplicationPort.class), syncService, mock(DiscordDirectory.class));
  private final TemplateResource templates = new TemplateResource(currentUser, accessService, templateService,
      mock(EventStrategyService.class), mock(TemplateImportExportService.class));

  @BeforeEach
  void setUp() {
    when(currentUser.id()).thenReturn("user");
  }

  @Test
  void eventManagersCannotChangeTheCalendarGuildsOrTemplates() {
    allow(true, false, false);

    assertThrows(ForbiddenException.class, () -> calendar.remove("server", 1L));
    assertThrows(ForbiddenException.class, () -> settings.syncMembers("server"));
    assertThrows(ForbiddenException.class, () -> templates.create("server", "key", "Name", "dragon", "Details"));
    verifyNoInteractions(calendarService, syncService, templateService);
  }

  @Test
  void guildManagersManageTheCalendarAndGuilds() {
    allow(false, true, false);

    calendar.remove("server", 1L);
    settings.syncMembers("server");

    verify(calendarService).deleteEntry("server", 1L);
    verify(syncService).syncGuildMembersByServerId("server");
    assertThrows(ForbiddenException.class, () -> templates.create("server", "key", "Name", "dragon", "Details"));
  }

  @Test
  void templateManagersCreateTemplatesWithUniqueKeys() {
    allow(false, false, true);

    templates.create("server", "key", "Name", "dragon", "Details");

    verify(templateService).createTemplate("server", "key", "Name", "Details", "dragon");
  }

  private void allow(boolean events, boolean guilds, boolean templates) {
    when(accessService.serverAccess("server", "user"))
        .thenReturn(new WebAccessService.ServerAccess("server", "Server", events, guilds, templates, false));
  }
}
