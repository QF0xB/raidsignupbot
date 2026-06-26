package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.template;

import de.qf0xb.raidsignupbot.application.port.template.EventTemplateApplicationPort;
import de.qf0xb.raidsignupbot.domain.model.template.EventTemplate;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.CommandAccessLevel;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.RaidCommand;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.editorwizard.template.TemplateEditorWorkflow;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Optional;

@ApplicationScoped
public class TemplateEditCommand extends RaidCommand {
  private static final String TEMPLATE_KEY = "template";

  @Inject
  EventTemplateApplicationPort templateApplicationPort;

  @Inject
  TemplateEditorWorkflow templateEditorWorkflow;

  public TemplateEditCommand() {
    super("edittemplate", "Edit a template", CommandAccessLevel.MANAGE_TEMPLATES);
    addOption(new OptionData(OptionType.STRING, TEMPLATE_KEY, "The template to edit", true, true));
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    OptionMapping mapping = event.getOption(TEMPLATE_KEY);
    if (mapping == null) {
      event.reply("Missing required option: " + TEMPLATE_KEY).setEphemeral(true).queue();
      return;
    }

    String serverId = event.getGuild() == null ? null : event.getGuild().getId();
    if (serverId == null) {
      event.reply("This command can only be used in a server.").setEphemeral(true).queue();
      return;
    }

    Optional<EventTemplate> template = templateApplicationPort.findByServerIdAndKey(serverId, mapping.getAsString());
    if (template.isEmpty()) {
      event.reply("Template not found: " + mapping.getAsString()).setEphemeral(true).queue();
      return;
    }

    templateEditorWorkflow.start(event, template.get());
  }

  @Override
  public void autoComplete(CommandAutoCompleteInteractionEvent event) {
    if (event.getGuild() == null) {
      return;
    }

    event.replyChoices(templateApplicationPort.findAllByServerId(event.getGuild().getId())
        .stream().map(template -> new Command.Choice(template.getKey(), template.getKey()))
        .toList()).queue();
  }
}
