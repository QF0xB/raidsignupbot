package de.qf0xb.raidsignupbot.guild.infrastructure.adapter.out.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "guild_member",
    uniqueConstraints = @UniqueConstraint(name = "uc_guild_member_guild_player", columnNames = {"guild_id", "player_id"})
)
public class GuildMemberEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "guild_id", nullable = false)
  private GuildEntity guild;

  @Column(name = "player_id", nullable = false)
  private String playerId;

  @Column(name = "priority", nullable = false)
  private int priority;
}
