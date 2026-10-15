package de.qf0xb.raidsignupbot.infrastructure.jpa.template;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@DiscriminatorValue("EventUtilityRoleEntity")
public class EventUtilityRoleEntity extends EventRoleEntity {


  private static final long serialVersionUID = -5977625908712633538L;

  @ElementCollection
  @Column(name = "eligible_primary_role_key")
  @CollectionTable(name = "event_role_eligiblePrimaryRoleKeys", joinColumns = @JoinColumn(name = "owner_id"))
  @OrderBy
  private Set<String> eligiblePrimaryRoleKeys = new LinkedHashSet<>();
}
