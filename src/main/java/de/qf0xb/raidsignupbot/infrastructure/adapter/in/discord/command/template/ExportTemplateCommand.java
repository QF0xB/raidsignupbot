package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.template;

import de.qf0xb.raidsignupbot.application.port.template.EventTemplateApplicationPort;
import de.qf0xb.raidsignupbot.application.service.template.TemplateImportExportService;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.CommandAccessLevel;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.RaidCommand;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
public class ExportTemplateCommand extends RaidCommand {
  private static final String TEMPLATE_KEY = "template";

  @Inject
  EventTemplateApplicationPort templateApplicationPort;

  @Inject
  TemplateImportExportService templateImportExportService;

  public ExportTemplateCommand() {
    super("exporttemplate", "Exports a single template as a JSON file.", CommandAccessLevel.MANAGE_TEMPLATES);
    addOption(new OptionData(OptionType.STRING, TEMPLATE_KEY, "The template to export", true, true));
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    if (event.getGuild() == null) {
      event.reply("This command can only be used in a server.").setEphemeral(true).queue();
      return;
    }
    event.deferReply(true).queue(ignored -> {
      OptionMapping mapping = event.getOption(TEMPLATE_KEY);
      if (mapping == null) {
        event.getHook().sendMessage("Missing required option: " + TEMPLATE_KEY).queue();
        return;
      }

      Optional<EventTemplate> template = templateApplicationPort.findByServerIdAndKey(event.getGuild().getId(), mapping.getAsString());
      if (template.isEmpty()) {
        event.getHook().sendMessage("Template not found: " + mapping.getAsString()).queue();
        return;
      }

      String json = templateImportExportService.exportTemplateAsString(template.get());
      String fileName = "template-" + template.get().getKey() + ".json";
      event.getHook()
          .sendFiles(FileUpload.fromData(json.getBytes(StandardCharsets.UTF_8), fileName))
          .queue();
    });
  }

  @Override
  public void autoComplete(CommandAutoCompleteInteractionEvent event) {
    if (!Objects.equals(event.getFocusedOption().getName(), TEMPLATE_KEY)) {
      return;
    }

    if (event.getGuild() == null) {
      return;
    }

    event.replyChoices(templateApplicationPort.findAllByServerId(event.getGuild().getId())
        .stream()
        .map(template -> new Command.Choice(template.getKey(), template.getKey()))
        .toList()).queue();
  }
}
