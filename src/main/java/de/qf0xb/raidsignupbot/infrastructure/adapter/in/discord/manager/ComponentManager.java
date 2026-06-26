package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.manager;

import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.ComponentInteractionHandler;
import de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.stepmodalwizard.StepModalWizard;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@ApplicationScoped
public class ComponentManager extends ListenerAdapter {
  private final JDA discordApi;
  private final Instance<ComponentInteractionHandler> componentHandlers;

  private final List<ComponentInteractionHandler> componentInteractionHandlers = new ArrayList<>();

  public ComponentManager(JDA discordApi, Instance<ComponentInteractionHandler> componentHandlers) {
    this.discordApi = discordApi;
    this.componentHandlers = componentHandlers;
  }

  @PostConstruct
  void init() {
    componentHandlers.forEach(this::addComponentInteractionHandler);
  }

  public void addComponentInteractionHandler(ComponentInteractionHandler handler) {
    log.info("Registering component interaction handler: {}", handler.getClass().getSimpleName());
    componentInteractionHandlers.add(handler);
  }

  @Scheduled(every = "1m", identity = "component-manager")
  public void checkForExpiredComponents() {
    componentInteractionHandlers.forEach(ComponentInteractionHandler::cleanupExpiredSessions);
  }

  @Override
  public void onButtonInteraction(@NonNull ButtonInteractionEvent event) {
    log.debug("Received button interaction: {}", event.getButton().getCustomId());
    componentInteractionHandlers.stream()
        .filter(handler -> handler.supportsButtonId(event.getButton().getCustomId()))
        .findFirst()
        .ifPresent(handler -> safeHandle(event, handler, "button interaction", handler::onButtonInteraction));
  }

  @Override
  public void onModalInteraction(@NonNull ModalInteractionEvent event) {
    log.debug("Received modal interaction: {}", event.getModalId());
    componentInteractionHandlers.stream()
        .filter(handler -> handler.supportsModalId(event.getModalId()))
        .findFirst()
        .ifPresent(handler -> safeHandle(event, handler, "modal interaction", handler::onModalInteraction));
  }

  @Override
  public void onStringSelectInteraction(@NonNull StringSelectInteractionEvent event) {
    log.debug("Received string select interaction: {}", event.getCustomId());
    componentInteractionHandlers.stream()
        .filter(handler -> handler.supportsStringSelectId(event.getCustomId()))
        .findFirst()
        .ifPresent(handler -> safeHandle(event, handler, "string select interaction", handler::onStringSelectInteraction));
  }

  @Override
  public void onEntitySelectInteraction(@NonNull EntitySelectInteractionEvent event) {
    log.debug("Received entity select interaction: {}", event.getCustomId());
    componentInteractionHandlers.stream()
        .filter(handler -> handler.supportsEntitySelectId(event.getCustomId()))
        .findFirst()
        .ifPresent(handler -> safeHandle(event, handler, "entity select interaction", handler::onEntitySelectInteraction));
  }

  private void safeHandle(
      ButtonInteractionEvent event,
      ComponentInteractionHandler handler,
      String interactionType,
      java.util.function.Consumer<ButtonInteractionEvent> delegate) {
    try {
      delegate.accept(event);
    } catch (RuntimeException ex) {
      handleFailure(event, handler, interactionType, ex);
    }
  }

  private void safeHandle(
      ModalInteractionEvent event,
      ComponentInteractionHandler handler,
      String interactionType,
      java.util.function.Consumer<ModalInteractionEvent> delegate) {
    try {
      delegate.accept(event);
    } catch (RuntimeException ex) {
      handleFailure(event, handler, interactionType, ex);
    }
  }

  private void safeHandle(
      StringSelectInteractionEvent event,
      ComponentInteractionHandler handler,
      String interactionType,
      java.util.function.Consumer<StringSelectInteractionEvent> delegate) {
    try {
      delegate.accept(event);
    } catch (RuntimeException ex) {
      handleFailure(event, handler, interactionType, ex);
    }
  }

  private void safeHandle(
      EntitySelectInteractionEvent event,
      ComponentInteractionHandler handler,
      String interactionType,
      java.util.function.Consumer<EntitySelectInteractionEvent> delegate) {
    try {
      delegate.accept(event);
    } catch (RuntimeException ex) {
      handleFailure(event, handler, interactionType, ex);
    }
  }

  private void handleFailure(ButtonInteractionEvent event, ComponentInteractionHandler handler, String interactionType, RuntimeException ex) {
    log.error("Failed to handle {} in {}", interactionType, handler.getClass().getSimpleName(), ex);
    replyFailure(event, "Something went wrong while handling this button.");
  }

  private void handleFailure(ModalInteractionEvent event, ComponentInteractionHandler handler, String interactionType, RuntimeException ex) {
    log.error("Failed to handle {} in {}", interactionType, handler.getClass().getSimpleName(), ex);
    replyFailure(event, "Something went wrong while handling this form.");
  }

  private void handleFailure(StringSelectInteractionEvent event, ComponentInteractionHandler handler, String interactionType, RuntimeException ex) {
    log.error("Failed to handle {} in {}", interactionType, handler.getClass().getSimpleName(), ex);
    replyFailure(event, "Something went wrong while handling this selection.");
  }

  private void handleFailure(EntitySelectInteractionEvent event, ComponentInteractionHandler handler, String interactionType, RuntimeException ex) {
    log.error("Failed to handle {} in {}", interactionType, handler.getClass().getSimpleName(), ex);
    replyFailure(event, "Something went wrong while handling this selection.");
  }

  private void replyFailure(net.dv8tion.jda.api.interactions.callbacks.IReplyCallback event, String message) {
    if (event.isAcknowledged()) {
      event.getHook().sendMessage(message).setEphemeral(true).queue();
      return;
    }

    event.reply(message).setEphemeral(true).queue();
  }
}
