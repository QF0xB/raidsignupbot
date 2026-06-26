CREATE TABLE server
(
    server_id               VARCHAR(255) NOT NULL,
    guild_manager_role_id   VARCHAR(255) NULL,
    template_manager_role_id VARCHAR(255) NULL,
    event_manager_role_id   VARCHAR(255) NULL,
    manage_event_channels   BIT(1)       NOT NULL DEFAULT b'1',
    manage_voice_channels   BIT(1)       NOT NULL DEFAULT b'0',
    raid_voice_channel_category VARCHAR(255) NULL,
    CONSTRAINT pk_server PRIMARY KEY (server_id)
);

CREATE TABLE guild
(
    id                    BIGINT AUTO_INCREMENT NOT NULL,
    server_id             VARCHAR(255)          NOT NULL,
    name                  VARCHAR(255)          NOT NULL,
    raider_roles_enabled  BIT(1)                NOT NULL DEFAULT b'1',
    raider_role_id        VARCHAR(255)          NULL,
    event_role_id         VARCHAR(255)          NULL,
    CONSTRAINT pk_guild PRIMARY KEY (id),
    CONSTRAINT uk_guild_server_name UNIQUE (server_id, name)
);

CREATE TABLE guild_member
(
    external_user_id VARCHAR(255) NOT NULL,
    priority         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_guild_member PRIMARY KEY (external_user_id)
);

CREATE TABLE guild_member_guild
(
    guild_id        BIGINT       NOT NULL,
    guild_member_id VARCHAR(255) NOT NULL,
    CONSTRAINT pk_guild_member_guild PRIMARY KEY (guild_id, guild_member_id)
);

CREATE TABLE event_template
(
    template_key VARCHAR(255) NOT NULL,
    server_id    VARCHAR(255) NOT NULL,
    name         VARCHAR(255) NOT NULL,
    details      TINYTEXT     NOT NULL,
    emoji_slug   VARCHAR(255) NOT NULL,
    CONSTRAINT pk_event_template PRIMARY KEY (template_key)
);

CREATE TABLE event_strategy
(
    id                                 BIGINT AUTO_INCREMENT NOT NULL,
    strategy_key                       VARCHAR(255) NOT NULL,
    name                               VARCHAR(255) NOT NULL,
    event_template_entity_template_key VARCHAR(255) NOT NULL,
    CONSTRAINT pk_event_strategy PRIMARY KEY (id),
    CONSTRAINT uk_event_strategy_template_strategy UNIQUE (event_template_entity_template_key, strategy_key)
);

CREATE TABLE event_strategy_entity_guides
(
    owner_id BIGINT NOT NULL,
    guide    VARCHAR(255) NULL
);

CREATE TABLE event_role
(
    id                       BIGINT AUTO_INCREMENT NOT NULL,
    DTYPE                    VARCHAR(31)  NOT NULL,
    role_key                 VARCHAR(255) NOT NULL,
    name                     VARCHAR(255) NOT NULL,
    emoji_slug               VARCHAR(255) NOT NULL,
    required_count           INT          NOT NULL,
    event_strategy_entity_id BIGINT       NOT NULL,
    CONSTRAINT pk_event_role PRIMARY KEY (id),
    CONSTRAINT uk_event_role_strategy_role UNIQUE (event_strategy_entity_id, role_key)
);

CREATE TABLE event_role_eligiblePrimaryRoleKeys
(
    owner_id                  BIGINT       NOT NULL,
    eligible_primary_role_key VARCHAR(255) NOT NULL,
    CONSTRAINT pk_event_role_eligible_primary_role_keys PRIMARY KEY (owner_id, eligible_primary_role_key)
);

CREATE TABLE event
(
    id                            BIGINT AUTO_INCREMENT NOT NULL,
    guild_id                      BIGINT       NOT NULL,
    hoster_user_id                VARCHAR(255) NOT NULL,
    category_id                   VARCHAR(255),
    signup_channel_id             VARCHAR(255),
    message_id                    VARCHAR(255),
    teams_message_id              VARCHAR(255),
    voice_channel_id              VARCHAR(255),
    donation                      INT          NOT NULL,
    donation_per_win              BIT(1)       NOT NULL DEFAULT b'0',
    signup_open                   BIT(1)       NOT NULL DEFAULT b'1',
    start_at                      DATETIME     NOT NULL,
    end_at                        DATETIME     NOT NULL,
    template_key                  VARCHAR(255) NOT NULL,
    teams_generated               BIT(1)       NOT NULL DEFAULT b'0',
    channels_exist                BIT(1)       NOT NULL DEFAULT b'0',
    one_hour_before_emitted       BIT(1)       NOT NULL DEFAULT b'0',
    thirty_minutes_before_emitted BIT(1)       NOT NULL DEFAULT b'0',
    fifteen_minutes_before_emitted BIT(1)      NOT NULL DEFAULT b'0',
    one_hour_after_emitted        BIT(1)       NOT NULL DEFAULT b'0',
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

ALTER TABLE guild
    ADD CONSTRAINT fk_guild_on_server FOREIGN KEY (server_id) REFERENCES server (server_id);

ALTER TABLE guild_member_guild
    ADD CONSTRAINT fk_guild_member_guild_on_guild FOREIGN KEY (guild_id) REFERENCES guild (id);

ALTER TABLE guild_member_guild
    ADD CONSTRAINT fk_guild_member_guild_on_member FOREIGN KEY (guild_member_id) REFERENCES guild_member (external_user_id);

ALTER TABLE event_template
    ADD CONSTRAINT fk_event_template_on_server FOREIGN KEY (server_id) REFERENCES server (server_id);

ALTER TABLE event_strategy
    ADD CONSTRAINT fk_event_strategy_on_template FOREIGN KEY (event_template_entity_template_key) REFERENCES event_template (template_key);

ALTER TABLE event_strategy_entity_guides
    ADD CONSTRAINT fk_event_strategy_entity_guides_on_strategy FOREIGN KEY (owner_id) REFERENCES event_strategy (id);

ALTER TABLE event_role
    ADD CONSTRAINT fk_event_role_on_strategy FOREIGN KEY (event_strategy_entity_id) REFERENCES event_strategy (id);

ALTER TABLE event_role_eligiblePrimaryRoleKeys
    ADD CONSTRAINT fk_event_role_eligible_primary_role_keys_on_role FOREIGN KEY (owner_id) REFERENCES event_role (id);

ALTER TABLE event
    ADD CONSTRAINT fk_event_on_guild FOREIGN KEY (guild_id) REFERENCES guild (id);

ALTER TABLE event
    ADD CONSTRAINT fk_event_on_hoster FOREIGN KEY (hoster_user_id) REFERENCES guild_member (external_user_id);

ALTER TABLE event
    ADD CONSTRAINT fk_event_on_template FOREIGN KEY (template_key) REFERENCES event_template (template_key);

ALTER TABLE event_team
    ADD CONSTRAINT fk_event_team_on_event FOREIGN KEY (event_id) REFERENCES event (id);

ALTER TABLE event_team
    ADD CONSTRAINT fk_event_team_on_strategy FOREIGN KEY (event_strategy_id) REFERENCES event_strategy (id);

ALTER TABLE event_team_assignment
    ADD CONSTRAINT fk_event_team_assignment_on_team FOREIGN KEY (event_team_id) REFERENCES event_team (id);

ALTER TABLE event_team_assignment
    ADD CONSTRAINT fk_event_team_assignment_on_member FOREIGN KEY (member_user_id) REFERENCES guild_member (external_user_id);

CREATE INDEX idx_guild_server_id ON guild (server_id);
CREATE INDEX idx_event_template_server_id ON event_template (server_id);
CREATE INDEX idx_event_guild_id ON event (guild_id);
CREATE INDEX idx_event_signup_channel_id ON event (signup_channel_id);
CREATE INDEX idx_event_template_key ON event (template_key);
