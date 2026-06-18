CREATE TABLE event_role
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    role_template_id BIGINT,
    name             VARCHAR(255) NOT NULL,
    role_count       INT          NOT NULL,
    CONSTRAINT pk_event_role PRIMARY KEY (id),
    CONSTRAINT uc_event_role_template UNIQUE (role_template_id)
);

CREATE TABLE event_team
(
    id            BIGINT AUTO_INCREMENT NOT NULL,
    event_id      BIGINT,
    strategy_id   BIGINT,
    strategy_name VARCHAR(255) NOT NULL,
    CONSTRAINT pk_event_team PRIMARY KEY (id)
);

CREATE TABLE event_assigned_role
(
    id                BIGINT AUTO_INCREMENT NOT NULL,
    event_team_id     BIGINT NOT NULL,
    player_profile_id BIGINT NOT NULL,
    role_id           BIGINT NOT NULL,
    utility_role_id   BIGINT,
    CONSTRAINT pk_event_assigned_role PRIMARY KEY (id)
);

ALTER TABLE event_assigned_role
    ADD CONSTRAINT fk_event_assigned_role_team FOREIGN KEY (event_team_id) REFERENCES event_team (id);

ALTER TABLE event_assigned_role
    ADD CONSTRAINT fk_event_assigned_role_player FOREIGN KEY (player_profile_id) REFERENCES player_profile (id);

ALTER TABLE event_assigned_role
    ADD CONSTRAINT fk_event_assigned_role_role FOREIGN KEY (role_id) REFERENCES event_role (id);

ALTER TABLE event_assigned_role
    ADD CONSTRAINT fk_event_assigned_role_utility_role FOREIGN KEY (utility_role_id) REFERENCES event_role (id);
