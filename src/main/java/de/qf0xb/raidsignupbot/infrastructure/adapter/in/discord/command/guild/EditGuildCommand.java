package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.guild;

import de.qf0xb.raidsignupbot.application.port.guild.GuildApplicationPort;
import de.qf0xb.raidsignupbot.domain.model.guild.Guild;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.CommandAccessLevel;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.RaidCommand;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.stepmodalwizard.guild.GuildStepModalWizard;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class EditGuildCommand extends RaidCommand {
  private static final String OPTION_NAME = "guild";

  @Inject
  GuildApplicationPort guildApplicationPort;

  @Inject
  GuildStepModalWizard guildWizard;

  public EditGuildCommand() {
    super("editguild", "Edit an existing guild.", CommandAccessLevel.MANAGE_GUILDS);
    addOption(new OptionData(OptionType.STRING, OPTION_NAME, "The guild to edit", true, true));
  }

  @Override
  public void execute(SlashCommandInteractionEvent event) {
    if (event.getGuild() == null) {
      event.reply("This command can only be used in a guild.")
          .setEphemeral(true)
          .queue();
      return;
    }

    String guildName = Objects.requireNonNull(event.getOption(OPTION_NAME),
        "Missing required option: " + OPTION_NAME).getAsString();
    Guild guild = guildApplicationPort.findByServerIdAndName(event.getGuild().getId(), guildName)
        .orElseThrow(() -> new IllegalArgumentException("Guild with name " + guildName + " not found"));

    guildWizard.startEdit(event, guild);
  }

  @Override
  public void autoComplete(CommandAutoCompleteInteractionEvent event) {
    if (!OPTION_NAME.equals(event.getFocusedOption().getName()) || event.getGuild() == null) {
      return;
    }

    List<Command.Choice> choices = guildApplicationPort.findGuildByServerId(event.getGuild().getId()).stream()
        .map(guild -> new Command.Choice(guild.getName(), guild.getName()))
        .toList();

    event.replyChoices(choices).queue();
  }
}
