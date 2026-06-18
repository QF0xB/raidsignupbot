package de.qf0xb.raidsignupbot.signup.infrastructure.adapter.out.persistence;

import de.qf0xb.raidsignupbot.shared.domain.model.EventRole;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class EventRoleRepository implements PanacheRepositoryBase<EventRoleEntity, Long> {
  public EventRoleEntity getOrCreate(EventRole role) {
    if (role.getId() != null) {
      Optional<EventRoleEntity> existingRole = find("roleTemplateId", role.getId()).firstResultOptional();
      if (existingRole.isPresent()) {
        updateFromDomain(existingRole.get(), role);
        return existingRole.get();
      }
    }

    EventRoleEntity roleEntity = new EventRoleEntity();
    updateFromDomain(roleEntity, role);
    persist(roleEntity);
    return roleEntity;
  }

  private void updateFromDomain(EventRoleEntity entity, EventRole role) {
    entity.setRoleTemplateId(role.getId());
    entity.setName(role.getName());
    entity.setCount(role.getCount());
  }
}
