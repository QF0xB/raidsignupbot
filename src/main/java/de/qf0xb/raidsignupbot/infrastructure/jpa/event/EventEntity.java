package de.qf0xb.raidsignupbot.infrastructure.jpa.event;

import de.qf0xb.raidsignupbot.infrastructure.jpa.guild.GuildEntity;
import de.qf0xb.raidsignupbot.infrastructure.jpa.guild.GuildMemberEntity;
import de.qf0xb.raidsignupbot.infrastructure.jpa.template.EventTemplateEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "event")
public class EventEntity implements Serializable {
  private static final long serialVersionUID = 507737858312214165L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "guild_id", nullable = false)
  private GuildEntity guild;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "hoster_user_id", nullable = false)
  private GuildMemberEntity hoster;

  @Column(name = "category_id")
  private String categoryId;

  @Column(name = "signup_channel_id")
  private String signUpChannelId;

  @Column(name = "message_id")
  private String messageId;

  @Column(name = "voice_channel_id")
  private String voiceChannelId;

  @Column(name = "donation", nullable = false)
  private Integer donation;

  @Column(name = "donation_per_win", nullable = false)
  private Boolean donationPerWin = false;

  @Column(name = "signup_open", nullable = false)
  private Boolean signupOpen = true;

  @Column(name = "teams_generated", nullable = false)
  private Boolean teamsGenerated = false;

  @Column(name = "channels_exist", nullable = false)
  private Boolean channelsExist = false;

  @Column(name = "one_hour_before_emitted", nullable = false)
  private Boolean oneHourBeforeEmitted = false;

  @Column(name = "thirty_minutes_before_emitted", nullable = false)
  private Boolean thirtyMinutesBeforeEmitted = false;

  @Column(name = "fifteen_minutes_before_emitted", nullable = false)
  private Boolean fifteenMinutesBeforeEmitted = false;

  @Column(name = "one_hour_after_emitted", nullable = false)
  private Boolean oneHourAfterEmitted = false;

  @Column(name = "start_at", nullable = false)
  private Instant startAt;

  @Column(name = "end_at", nullable = false)
  private Instant endAt;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "template_key", nullable = false)
  private EventTemplateEntity eventTemplate;

  @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<EventTeamEntity> eventTeams = new LinkedHashSet<>();
}
