package de.qf0xb.raidsignupbot.infrastructure.adapter.out.jpa.event;

import de.qf0xb.raidsignupbot.application.port.EventApplicationPort;
import de.qf0xb.raidsignupbot.domain.model.event.Event;
import de.qf0xb.raidsignupbot.infrastructure.adapter.out.jpa.AbstractJpaApplicationAdapter;
import de.qf0xb.raidsignupbot.infrastructure.jpa.event.EventEntity;
import de.qf0xb.raidsignupbot.infrastructure.jpa.event.EventEntityRepository;
import de.qf0xb.raidsignupbot.infrastructure.mapper.jpa.EventEntityMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Optional;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class JpaEventApplicationAdapter
    extends AbstractJpaApplicationAdapter<
        Long, Event, EventEntity, EventEntityMapper, EventEntityRepository>
    implements EventApplicationPort {
  @Inject
  EventEntityMapper mapper;

  @Inject
  EventEntityRepository repository;

  @Override
  protected EventEntityMapper getMapper() {
    return mapper;
  }

  @Override
  protected EventEntityRepository getRepository() {
    return repository;
  }

  @Override
  @Transactional
  public Optional<Event> findById(Long id) {
    return getRepository().findByIdOptional(id)
        .map(getMapper()::toDomain);
  }

  @Override
  @Transactional
  public Set<Event> findAllEvents() {
    return getRepository().listAll().stream()
        .map(getMapper()::toDomain)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  @Override
  @Transactional
  public Set<Event> findAllUpcomingEvents() {
    return getRepository().listAll().stream()
        .map(getMapper()::toDomain)
        .filter(Event::isSignupOpen)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  @Override
  @Transactional
  public Set<Event> findAllChannelExistingEvents() {
    return getRepository().listAll().stream()
        .map(getMapper()::toDomain)
        .filter(Event::isChannelsExist)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  @Override
  @Transactional
  public Set<Event> findByTemplateKey(String templateKey) {
    return getRepository().list("eventTemplate.key", templateKey).stream()
        .map(getMapper()::toDomain)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  @Override
  @Transactional
  public Optional<Event> findByChannelId(String channelId) {
    return getRepository().list("signUpChannelId", channelId).stream()
        .map(getMapper()::toDomain)
        .findFirst();
  }

  @Override
  @Transactional
  public Event save(Event event) {
    return save(event.getId(), event);
  }

  @Override
  @Transactional
  public void deleteById(Long id) {
    // Deleting the entity (not a bulk delete) cascades to teams and their assignments.
    repository.findByIdOptional(id).ifPresent(repository::delete);
  }

  @Override
  @Transactional
  public EventEntity partialUpdate(Event domain, EventEntity entity) {
    return getMapper().partialUpdate(domain, entity);
  }
}
