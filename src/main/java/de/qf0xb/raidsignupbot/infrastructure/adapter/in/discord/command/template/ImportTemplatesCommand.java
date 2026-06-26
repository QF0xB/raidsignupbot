package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.template;

import de.qf0xb.raidsignupbot.application.service.template.TemplateImportExportService;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.CommandAccessLevel;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.RaidCommand;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@ApplicationScoped
public class ImportTemplatesCommand extends RaidCommand {
  private static final String FILE_OPTION = "file";

  @Inject
  TemplateImportExportService templateImportExportService;

  public ImportTemplatesCommand() {
    super("importtemplates", "Imports templates from a JSON file.", CommandAccessLevel.MANAGE_TEMPLATES);
    addOption(new OptionData(OptionType.ATTACHMENT, FILE_OPTION, "The exported template JSON file", true));
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    if (event.getGuild() == null) {
      event.reply("This command can only be used in a server.").setEphemeral(true).queue();
      return;
    }
    event.deferReply(true).queue(ignored -> {
      OptionMapping mapping = event.getOption(FILE_OPTION);
      if (mapping == null) {
        event.getHook().sendMessage("Missing required option: " + FILE_OPTION).queue();
        return;
      }

      Attachment attachment = mapping.getAsAttachment();
      try (InputStream inputStream = attachment.getProxy().download().join()) {
        List<EventTemplate> importedTemplates = templateImportExportService.importTemplates(event.getGuild().getId(), inputStream);
        event.getHook().sendMessage("Imported " + importedTemplates.size() + " template(s).").queue();
      } catch (IOException e) {
        event.getHook().sendMessage("Could not close the uploaded template file: " + e.getMessage()).queue();
      } catch (Exception e) {
        event.getHook().sendMessage("Could not import templates: " + e.getMessage()).queue();
      }
    });
  }

  @Override
  public void autoComplete(CommandAutoCompleteInteractionEvent event) {
  }
}
