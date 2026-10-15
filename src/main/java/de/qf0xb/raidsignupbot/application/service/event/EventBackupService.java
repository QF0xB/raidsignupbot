package de.qf0xb.raidsignupbot.application.service.event;

import de.qf0xb.raidsignupbot.application.TeamGeneratorResult;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.domain.model.event.EventBackupOffer;
import de.qf0xb.raidsignupbot.domain.model.event.EventMemberSignup;
import de.qf0xb.raidsignupbot.domain.model.template.EventRole;
import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.scheduler.Scheduled;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ApplicationScoped
public class EventBackupService {
  private final Map<String, OfferState> offers = new ConcurrentHashMap<>();

  public synchronized void registerOffers(Event event, TeamGeneratorResult result) {
    if (event.getId() == null || event.getTemplate() == null || event.getEnd() == null) {
      return;
    }

    offers.entrySet().removeIf(entry -> entry.getValue().offer().eventId().equals(event.getId()));

    Map<String, String> primaryRoleNames = event.getTemplate().getStrategies().stream()
        .flatMap(strategy -> strategy.getPrimaryRoles().stream())
        .collect(Collectors.toMap(
            EventRole::getKey,
            EventRole::getName,
            (left, right) -> left,
            LinkedHashMap::new));

    for (EventMemberSignup signup : result.unmatchedSignups()) {
      signup.getRoleKeys().stream()
          .filter(primaryRoleNames::containsKey)
          .distinct()
          .sorted(Comparator.comparing(primaryRoleNames::get))
          .forEach(roleKey -> {
            EventBackupOffer offer = new EventBackupOffer(
                UUID.randomUUID().toString(),
                event.getId(),
                signup.getGuildMember().getUserId(),
                roleKey,
                primaryRoleNames.get(roleKey));
            offers.put(offer.offerId(), new OfferState(offer, OfferStatus.PENDING, event.getEnd()));
          });
    }
  }

  public List<EventBackupOffer> findOffers(Long eventId) {
    return offers.values().stream()
        .filter(state -> state.offer().eventId().equals(eventId))
        .map(OfferState::offer)
        .toList();
  }

  public synchronized BackupResponse respond(String offerId, String userId, boolean available) {
    OfferState state = offers.get(offerId);
    if (state == null || state.expiresAt().isBefore(Instant.now())) {
      throw new IllegalStateException("This backup request has expired");
    }
    if (!state.offer().userId().equals(userId)) {
      throw new IllegalArgumentException("This backup request belongs to another user");
    }
    if (state.status() != OfferStatus.PENDING) {
      return BackupResponse.info(state.status() == OfferStatus.ACCEPTED
          ? "You are already confirmed as the backup for **" + state.offer().roleName() + "**."
          : "This backup request has already been declined.");
    }
    if (!available) {
      state.setStatus(OfferStatus.DECLINED);
      return BackupResponse.info("You declined the backup request for **" + state.offer().roleName() + "**.");
    }

    boolean positionAlreadyCovered = offers.values().stream()
        .map(OfferState::offer)
        .filter(offer -> offer.eventId().equals(state.offer().eventId()))
        .filter(offer -> offer.roleKey().equals(state.offer().roleKey()))
        .anyMatch(offer -> offers.get(offer.offerId()).status() == OfferStatus.ACCEPTED);
    if (positionAlreadyCovered) {
      state.setStatus(OfferStatus.DECLINED);
      return BackupResponse.info("Someone else has already accepted the backup position for **" + state.offer().roleName() + "**.");
    }

    state.setStatus(OfferStatus.ACCEPTED);
    return new BackupResponse("Thanks! You are confirmed as the backup for **" + state.offer().roleName() + "**.", state.offer());
  }

  public List<EventBackupOffer> findAcceptedOffers(Long eventId) {
    return offers.values().stream()
        .filter(state -> state.offer().eventId().equals(eventId))
        .filter(state -> state.status() == OfferStatus.ACCEPTED)
        .map(OfferState::offer)
        .toList();
  }

  public synchronized void clearOffers(Long eventId) {
    offers.entrySet().removeIf(entry -> entry.getValue().offer().eventId().equals(eventId));
  }

  @Scheduled(every = "10m", identity = "event-backup-offer-cleanup")
  public void cleanupExpiredOffers() {
    Instant now = Instant.now();
    offers.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
  }

  /** Reply for the responding user; {@code acceptedOffer} is set only when this response newly filled a backup position. */
  public record BackupResponse(String message, EventBackupOffer acceptedOffer) {
    static BackupResponse info(String message) {
      return new BackupResponse(message, null);
    }
  }

  private enum OfferStatus {
    PENDING,
    ACCEPTED,
    DECLINED
  }

  private static final class OfferState {
    private final EventBackupOffer offer;
    private final Instant expiresAt;
    private OfferStatus status;

    private OfferState(EventBackupOffer offer, OfferStatus status, Instant expiresAt) {
      this.offer = offer;
      this.status = status;
      this.expiresAt = expiresAt;
    }

    private EventBackupOffer offer() {
      return offer;
    }

    private OfferStatus status() {
      return status;
    }

    private void setStatus(OfferStatus status) {
      this.status = status;
    }

    private Instant expiresAt() {
      return expiresAt;
    }
  }
}
