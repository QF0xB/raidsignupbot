CREATE TABLE event
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    guild_id         VARCHAR(255) NOT NULL,
    hoster_user_id   VARCHAR(255) NOT NULL,
    category_id      VARCHAR(255),
    signup_channel_id VARCHAR(255),
    message_id       VARCHAR(255),
    voice_channel_id VARCHAR(255),
    donation         INT          NOT NULL,
    donation_per_win BIT(1)       NOT NULL,
    signup_open      BIT(1)       NOT NULL,
    start_at         datetime     NOT NULL,
    end_at           datetime     NOT NULL,
    template_key     VARCHAR(255) NOT NULL,
    teams_generated                BIT(1) NOT NULL DEFAULT b'0',
    channels_exist                 BIT(1) NOT NULL DEFAULT b'0',
    one_hour_before_emitted        BIT(1) NOT NULL DEFAULT b'0',
    thirty_minutes_before_emitted  BIT(1) NOT NULL DEFAULT b'0',
    fifteen_minutes_before_emitted BIT(1) NOT NULL DEFAULT b'0',
    one_hour_after_emitted         BIT(1) NOT NULL DEFAULT b'0',
    CONSTRAINT pk_event PRIMARY KEY (id)
);

CREATE TABLE event_team
(
    id                BIGINT AUTO_INCREMENT NOT NULL,
    event_id          BIGINT NOT NULL,
    event_strategy_id BIGINT NOT NULL,
    CONSTRAINT pk_event_team PRIMARY KEY (id)
);

CREATE TABLE event_team_assignment
(
    id             BIGINT AUTO_INCREMENT NOT NULL,
    event_team_id  BIGINT       NOT NULL,
    role_kind      VARCHAR(255) NOT NULL,
    role_key       VARCHAR(255) NOT NULL,
    member_user_id VARCHAR(255) NOT NULL,
    CONSTRAINT pk_event_team_assignment PRIMARY KEY (id)
);

ALTER TABLE event
    ADD CONSTRAINT FK_EVENT_ON_GUILD FOREIGN KEY (guild_id) REFERENCES guild (guild_id);

ALTER TABLE event
    ADD CONSTRAINT FK_EVENT_ON_HOSTER_USER FOREIGN KEY (hoster_user_id) REFERENCES guild_member (external_user_id);

ALTER TABLE event
    ADD CONSTRAINT FK_EVENT_ON_TEMPLATE_KEY FOREIGN KEY (template_key) REFERENCES event_template (template_key);

ALTER TABLE event_team
    ADD CONSTRAINT FK_EVENT_TEAM_ON_EVENT FOREIGN KEY (event_id) REFERENCES event (id);

ALTER TABLE event_team
    ADD CONSTRAINT FK_EVENT_TEAM_ON_EVENT_STRATEGY FOREIGN KEY (event_strategy_id) REFERENCES event_strategy (id);

ALTER TABLE event_team_assignment
    ADD CONSTRAINT FK_EVENT_TEAM_ASSIGNMENT_ON_EVENT_TEAM FOREIGN KEY (event_team_id) REFERENCES event_team (id);

ALTER TABLE event_team_assignment
    ADD CONSTRAINT FK_EVENT_TEAM_ASSIGNMENT_ON_MEMBER_USER FOREIGN KEY (member_user_id) REFERENCES guild_member (external_user_id);
