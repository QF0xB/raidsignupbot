package de.qf0xb.raidsignupbot.domain.model.event;

public record EventBackupOffer(
    String offerId,
    Long eventId,
    String userId,
    String roleKey,
    String roleName) {
}
