package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.template;

import de.qf0xb.raidsignupbot.application.port.template.EventTemplateApplicationPort;
import de.qf0xb.raidsignupbot.application.service.template.TemplateImportExportService;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.CommandAccessLevel;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.RaidCommand;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@ApplicationScoped
public class ExportTemplatesCommand extends RaidCommand {
  @Inject
  EventTemplateApplicationPort templateApplicationPort;

  @Inject
  TemplateImportExportService templateImportExportService;

  public ExportTemplatesCommand() {
    super("exporttemplates", "Exports all templates as a JSON file.", CommandAccessLevel.MANAGE_TEMPLATES);
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    if (event.getGuild() == null) {
      event.reply("This command can only be used in a server.").setEphemeral(true).queue();
      return;
    }
    event.deferReply(true).queue(ignored -> {
      String json = templateImportExportService.exportTemplatesAsString(
          templateApplicationPort.findAllByServerId(event.getGuild().getId()));
      String fileName = "templates-export-" + LocalDate.now() + ".json";

      event.getHook()
          .sendFiles(FileUpload.fromData(json.getBytes(StandardCharsets.UTF_8), fileName))
          .queue();
    });
  }

  @Override
  public void autoComplete(CommandAutoCompleteInteractionEvent event) {
  }
}
