package de.qf0xb.raidsignupbot.signup.domain.service;

import de.qf0xb.raidsignupbot.shared.domain.model.EventRole;

public record UtilityAssignment(
    EventRole utility,
    PlayerSignup playerSignup,
    EventRole assignedRole
) {
}
