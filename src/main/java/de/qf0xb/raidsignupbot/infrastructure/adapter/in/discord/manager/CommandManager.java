package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.manager;

import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.SlashCommandDefinition;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.command.CommandPermissionService;
import de.qf0xb.raidsignupbot.infrastructure.config.DiscordConfig;
import io.quarkus.runtime.LaunchMode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.enterprise.inject.Instance;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@ApplicationScoped
public class CommandManager extends ListenerAdapter {
  private final JDA discordApi;
  private final Instance<SlashCommandDefinition> commandDefinitions;
  private final CommandPermissionService commandPermissionService;

  @Inject
  DiscordConfig discordConfig;

  private final List<SlashCommandDefinition> commands = new ArrayList<>();

  public CommandManager(
      JDA discordApi,
      Instance<SlashCommandDefinition> commandDefinitions,
      CommandPermissionService commandPermissionService) {
    this.discordApi = discordApi;
    this.commandDefinitions = commandDefinitions;
    this.commandPermissionService = commandPermissionService;
  }

  @jakarta.annotation.PostConstruct
  void init() {
    commandDefinitions.forEach(commands::add);
  }

  public void addAndRegisterCommand(SlashCommandDefinition command) {
    addCommand(command);
    registerCommands();
  }

  public void addCommand(SlashCommandDefinition command) {
    commands.add(command);
  }

  public void registerCommands() {
    Set<CommandData> cmds = new HashSet<>();
    commands.forEach(
        command -> cmds.add(Commands.slash(command.getCommandName(), command.getDescription())
            .addOptions(command.getOptions())));

    if (LaunchMode.current() == LaunchMode.DEVELOPMENT) {
      discordConfig.getGuildId()
          .map(discordApi::getGuildById)
          .ifPresent(guild -> guild.updateCommands().addCommands(cmds).queue());
      log.info("Registered slash commands to guild {}: {}", discordConfig.getGuildId().orElseThrow(), cmds.stream().map(CommandData::getName).toList());
    } else {
      discordApi.updateCommands().addCommands(cmds).queue();
      log.info("Registered slash commands globally: {}", cmds);
    }
  }

  @Override
  public void onSlashCommandInteraction(@NonNull SlashCommandInteractionEvent event) {
    commands.stream()
        .filter(command -> command.hasName(event.getName()))
        .findFirst()
        .ifPresent(command -> {
          try {
            if (!commandPermissionService.isAllowed(event, command.getAccessLevel())) {
              event.reply("You do not have permission to use this command.")
                  .setEphemeral(true)
                  .queue();
              return;
            }
            command.execute(event);
          } catch (RuntimeException ex) {
            handleCommandFailure(event, command.getCommandName(), ex);
          }
        });
  }

  @Override
  public void onCommandAutoCompleteInteraction(@NonNull CommandAutoCompleteInteractionEvent event) {
    commands.stream()
        .filter(command -> command.hasName(event.getName()))
        .findFirst()
        .ifPresent(command -> {
          try {
            command.autoComplete(event);
          } catch (RuntimeException ex) {
            log.error("Failed to handle autocomplete for command {}", command.getCommandName(), ex);
            if (!event.isAcknowledged()) {
              event.replyChoices(List.of()).queue();
            }
          }
        });
  }

  private void handleCommandFailure(SlashCommandInteractionEvent event, String commandName, RuntimeException ex) {
    log.error("Failed to handle slash command {}", commandName, ex);

    String message = "Something went wrong while handling /" + commandName + ".";
    if (event.isAcknowledged()) {
      event.getHook().sendMessage(message).setEphemeral(true).queue();
      return;
    }

    event.reply(message).setEphemeral(true).queue();
  }
}
