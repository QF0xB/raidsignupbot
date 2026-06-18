package de.qf0xb.raidsignupbot.event.infrastructure.adapter.out.jpa;

import de.qf0xb.raidsignupbot.guild.infrastructure.adapter.out.jpa.GuildMemberEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "event_role_assignment")
public class EventRoleAssignmentEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "team_id", nullable = false)
  private EventTeamEntity team;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "role_id", nullable = false)
  private EventRoleEntity role;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "guild_member_id")
  private GuildMemberEntity guildMember;
}
