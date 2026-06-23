package de.qf0xb.raidsignupbot.application.service.event;

import de.qf0xb.raidsignupbot.application.TeamGeneratorResult;
import de.qf0xb.raidsignupbot.application.dto.CreateEventCommand;
import de.qf0xb.raidsignupbot.application.port.EventApplicationPort;
import de.qf0xb.raidsignupbot.application.port.template.EventTemplateApplicationPort;
import de.qf0xb.raidsignupbot.application.service.generator.OrToolsTeamGeneratorService;
import de.qf0xb.raidsignupbot.application.service.guild.GuildMemberService;
import de.qf0xb.raidsignupbot.application.service.guild.GuildService;
import de.qf0xb.raidsignupbot.application.service.template.TemplateService;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventMemberSignup;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class EventService {
  private final EventApplicationPort eventApplicationPort;
  private final GuildService guildService;
  private final GuildMemberService guildMemberService;
  private final TemplateService templateService;
  private final OrToolsTeamGeneratorService teamGeneratorService = new OrToolsTeamGeneratorService();

  public EventService(EventTemplateApplicationPort eventTemplateApplicationPort, EventApplicationPort eventApplicationPort, GuildService guildService, GuildMemberService guildMemberService, TemplateService templateService) {
    this.eventApplicationPort = eventApplicationPort;
    this.guildService = guildService;
    this.guildMemberService = guildMemberService;
    this.templateService = templateService;
  }

  @Transactional
  public Event createEvent(CreateEventCommand command) {
    Event event =
        Event.builder()
            .guild(guildService.findGuildById(command.getGuildId()))
            .hoster(guildMemberService.findGuildMemberById(command.getHosterUserId()))
            .donationPerWin(command.isDonationPerWin())
            .donation(command.getDonation())
            .start(command.getStart())
            .end(command.getEnd())
            .template(templateService.findTemplateByKey(command.getTemplateKey()))
            .categoryId(command.getCategoryId())
            .build();

    return eventApplicationPort.save(event);
  }

  @Transactional
  public Event generateTeamsAndCloseSignup(Long eventId, List<EventMemberSignup> signups) {
    Event event = findEventById(eventId);
    if (!event.isSignupOpen()) {
      return event;
    }
    event.closeSignup();

    TeamGeneratorResult results = teamGeneratorService.generateTeams(event, signups);
    results.teams().forEach(event::addTeam);
    results.unmatchedSignups().forEach(eventMemberSignup -> {
      guildMemberService.couldNotFindRole(eventMemberSignup.getGuildMember().getUserId());
    });
    return eventApplicationPort.save(event);
  }

  public Event findEventById(Long eventId) {
    return eventApplicationPort.findById(eventId)
        .orElseThrow(() ->  new IllegalArgumentException("Event with id " + eventId + " not found"));
  }
}
