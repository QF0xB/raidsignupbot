package de.qf0xb.raidsignupbot.infrastructure.jpa.guild;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "guild_member")
public class GuildMemberEntity implements Serializable {
  private static final long serialVersionUID = -6892512541230843800L;

  @Id
  @Column(name = "external_user_id", nullable = false, unique = true)
  private String userId;

  @Column(name = "priority", nullable = false)
  private long priority;

  @ManyToMany
  @JoinTable(
      name = "guild_member_guild",
      joinColumns = @JoinColumn(name = "guild_member_id"),
      inverseJoinColumns = @JoinColumn(name = "guild_id"))
  private Set<GuildEntity> activeGuilds = new HashSet<>();
}
