package de.qf0xb.raidsignupbot.event.infrastructure.adapter.out.jpa;

import de.qf0xb.raidsignupbot.guild.infrastructure.adapter.out.jpa.GuildEntity;
import de.qf0xb.raidsignupbot.guild.infrastructure.adapter.out.jpa.GuildMemberEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "event_instance")
public class EventEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "template_slug", nullable = false)
  private EventTemplateEntity template;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "guild_id", nullable = false)
  private GuildEntity guild;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "hoster_id", nullable = false)
  private GuildMemberEntity hoster;

  @Column(name = "donation", nullable = false)
  private int donation;

  @Column(name = "signup_open", nullable = false)
  private boolean signupOpen;

  @Column(name = "start_time", nullable = false)
  private Instant start;

  @Column(name = "end_time", nullable = false)
  private Instant end;

  @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<EventTeamEntity> teams = new ArrayList<>();

  public void addTeam(EventTeamEntity team) {
    team.setEvent(this);
    teams.add(team);
  }
}
