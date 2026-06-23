package de.qf0xb.raidsignupbot.infrastructure.jpa.event;

import de.qf0xb.raidsignupbot.domain.model.shared.RoleKind;
import de.qf0xb.raidsignupbot.infrastructure.jpa.guild.GuildMemberEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "event_team_assignment")
public class EventTeamAssignmentEntity implements Serializable {
  private static final long serialVersionUID = 7588821460478040886L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_team_id", nullable = false)
  private EventTeamEntity eventTeam;

  @Enumerated(EnumType.STRING)
  @Column(name = "role_kind", nullable = false)
  private RoleKind roleKind;

  @Column(name = "role_key", nullable = false)
  private String roleKey;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "member_user_id", nullable = false)
  private GuildMemberEntity member;
}
