package de.qf0xb.raidsignupbot.signup.domain.service;

import de.qf0xb.raidsignupbot.shared.domain.model.EventRole;
import de.qf0xb.raidsignupbot.shared.domain.model.EventUtilityRole;

public record UtilityAssignment(
    EventUtilityRole utility,
    PlayerSignup playerSignup,
    EventRole assignedRole
) {
}
