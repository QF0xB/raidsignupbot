CREATE TABLE event_role
(
    id                       BIGINT AUTO_INCREMENT NOT NULL,
    DTYPE                    VARCHAR(31)  NOT NULL,
    role_key                 VARCHAR(255) NOT NULL,
    name                     VARCHAR(255) NOT NULL,
    emoji_slug               VARCHAR(255) NOT NULL,
    required_count           INT          NOT NULL,
    event_strategy_entity_id BIGINT       NOT NULL,
    CONSTRAINT pk_event_role PRIMARY KEY (id)
);

CREATE TABLE event_role_eligiblePrimaryRoleKeys
(
    owner_id                  BIGINT       NOT NULL,
    eligible_primary_role_key VARCHAR(255) NULL
);

CREATE TABLE event_strategy
(
    id                                 BIGINT AUTO_INCREMENT NOT NULL,
    strategy_key                       VARCHAR(255) NOT NULL,
    name                               VARCHAR(255) NOT NULL,
    event_template_entity_template_key VARCHAR(255) NOT NULL,
    CONSTRAINT pk_event_strategy PRIMARY KEY (id)
);

CREATE TABLE event_strategy_entity_guides
(
    owner_id BIGINT       NOT NULL,
    guide    VARCHAR(255) NULL
);

CREATE TABLE event_template
(
    template_key VARCHAR(255) NOT NULL,
    name         VARCHAR(255) NOT NULL,
    details      TINYTEXT     NOT NULL,
    emoji_slug   VARCHAR(255) NOT NULL,
    CONSTRAINT pk_event_template PRIMARY KEY (template_key)
);

ALTER TABLE event_role
    ADD CONSTRAINT uk_event_role_strategy_role UNIQUE (event_strategy_entity_id, role_key);

ALTER TABLE event_strategy
    ADD CONSTRAINT uk_event_strategy_template_strategy UNIQUE (event_template_entity_template_key, strategy_key);

ALTER TABLE event_role
    ADD CONSTRAINT FK_EVENT_ROLE_ON_EVENT_STRATEGY_ENTITY FOREIGN KEY (event_strategy_entity_id) REFERENCES event_strategy (id);

ALTER TABLE event_strategy
    ADD CONSTRAINT FK_EVENT_STRATEGY_ON_EVENT_TEMPLATE_ENTITY_TEMPLATE_KEY FOREIGN KEY (event_template_entity_template_key) REFERENCES event_template (template_key);

ALTER TABLE event_strategy_entity_guides
    ADD CONSTRAINT fk_event_strategy_entity_guides_on_event_strategy_entity FOREIGN KEY (owner_id) REFERENCES event_strategy (id);

ALTER TABLE event_role_eligiblePrimaryRoleKeys
    ADD CONSTRAINT fk_eventroleeligibleprimaryrolekeys_on_eventutilityroleentity FOREIGN KEY (owner_id) REFERENCES event_role (id);
