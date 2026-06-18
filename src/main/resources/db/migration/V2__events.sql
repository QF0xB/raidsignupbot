CREATE TABLE event_instance
(
    id            BIGINT AUTO_INCREMENT NOT NULL,
    template_slug VARCHAR(255) NOT NULL,
    guild_id      VARCHAR(255) NOT NULL,
    hoster_id     BIGINT       NOT NULL,
    donation      INT          NOT NULL,
    signup_open   BIT(1)       NOT NULL,
    start_time    datetime     NOT NULL,
    end_time      datetime     NOT NULL,
    CONSTRAINT pk_event_instance PRIMARY KEY (id)
);

CREATE TABLE event_role
(
    id             BIGINT AUTO_INCREMENT NOT NULL,
    strategy_slug  VARCHAR(255) NOT NULL,
    name           VARCHAR(255) NOT NULL,
    required_count INT          NOT NULL,
    utility        BIT(1)       NOT NULL,
    CONSTRAINT pk_event_role PRIMARY KEY (id)
);

CREATE TABLE event_role_assignment
(
    id              BIGINT AUTO_INCREMENT NOT NULL,
    team_id         BIGINT NOT NULL,
    role_id         BIGINT NOT NULL,
    guild_member_id BIGINT NULL,
    CONSTRAINT pk_event_role_assignment PRIMARY KEY (id)
);

CREATE TABLE event_role_eligible_role
(
    eligible_role_id BIGINT NOT NULL,
    utility_role_id  BIGINT NOT NULL,
    CONSTRAINT pk_event_role_eligible_role PRIMARY KEY (eligible_role_id, utility_role_id)
);

CREATE TABLE event_strategy
(
    slug          VARCHAR(255) NOT NULL,
    template_slug VARCHAR(255) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    CONSTRAINT pk_event_strategy PRIMARY KEY (slug)
);

CREATE TABLE event_strategy_guide
(
    strategy_slug VARCHAR(255) NOT NULL,
    guide         VARCHAR(255) NOT NULL
);

CREATE TABLE event_team
(
    id            BIGINT AUTO_INCREMENT NOT NULL,
    event_id      BIGINT       NOT NULL,
    strategy_slug VARCHAR(255) NOT NULL,
    CONSTRAINT pk_event_team PRIMARY KEY (id)
);

CREATE TABLE event_template
(
    slug    VARCHAR(255)  NOT NULL,
    name    VARCHAR(255)  NOT NULL,
    details VARCHAR(4000) NOT NULL,
    CONSTRAINT pk_event_template PRIMARY KEY (slug)
);

ALTER TABLE event_instance
    ADD CONSTRAINT FK_EVENT_INSTANCE_ON_GUILD FOREIGN KEY (guild_id) REFERENCES guild (guild_id);

ALTER TABLE event_instance
    ADD CONSTRAINT FK_EVENT_INSTANCE_ON_HOSTER FOREIGN KEY (hoster_id) REFERENCES guild_member (id);

ALTER TABLE event_instance
    ADD CONSTRAINT FK_EVENT_INSTANCE_ON_TEMPLATE_SLUG FOREIGN KEY (template_slug) REFERENCES event_template (slug);

ALTER TABLE event_role_assignment
    ADD CONSTRAINT FK_EVENT_ROLE_ASSIGNMENT_ON_GUILD_MEMBER FOREIGN KEY (guild_member_id) REFERENCES guild_member (id);

ALTER TABLE event_role_assignment
    ADD CONSTRAINT FK_EVENT_ROLE_ASSIGNMENT_ON_ROLE FOREIGN KEY (role_id) REFERENCES event_role (id);

ALTER TABLE event_role_assignment
    ADD CONSTRAINT FK_EVENT_ROLE_ASSIGNMENT_ON_TEAM FOREIGN KEY (team_id) REFERENCES event_team (id);

ALTER TABLE event_role
    ADD CONSTRAINT FK_EVENT_ROLE_ON_STRATEGY_SLUG FOREIGN KEY (strategy_slug) REFERENCES event_strategy (slug);

ALTER TABLE event_strategy
    ADD CONSTRAINT FK_EVENT_STRATEGY_ON_TEMPLATE_SLUG FOREIGN KEY (template_slug) REFERENCES event_template (slug);

ALTER TABLE event_team
    ADD CONSTRAINT FK_EVENT_TEAM_ON_EVENT FOREIGN KEY (event_id) REFERENCES event_instance (id);

ALTER TABLE event_team
    ADD CONSTRAINT FK_EVENT_TEAM_ON_STRATEGY_SLUG FOREIGN KEY (strategy_slug) REFERENCES event_strategy (slug);

ALTER TABLE guild_member
    ADD CONSTRAINT FK_GUILD_MEMBER_ON_GUILD FOREIGN KEY (guild_id) REFERENCES guild (guild_id);

ALTER TABLE event_strategy_guide
    ADD CONSTRAINT fk_event_strategy_guide_on_event_strategy_entity FOREIGN KEY (strategy_slug) REFERENCES event_strategy (slug);

ALTER TABLE event_role_eligible_role
    ADD CONSTRAINT fk_everolelirol_on_eligible_role FOREIGN KEY (eligible_role_id) REFERENCES event_role (id);

ALTER TABLE event_role_eligible_role
    ADD CONSTRAINT fk_everolelirol_on_utility_role FOREIGN KEY (utility_role_id) REFERENCES event_role (id);