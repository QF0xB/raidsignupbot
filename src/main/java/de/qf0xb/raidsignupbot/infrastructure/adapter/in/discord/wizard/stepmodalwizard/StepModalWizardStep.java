package de.qf0xb.raidsignupbot.infrastructure.adapter.in.discord.wizard.stepmodalwizard;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.modals.Modal;

public interface StepModalWizardStep {
  String prompt(StepModalWizardSession session);

  Modal buildModal(String modalId, StepModalWizardSession session, ButtonInteractionEvent event);

  void submit(ModalInteractionEvent event, StepModalWizardSession session);
}
