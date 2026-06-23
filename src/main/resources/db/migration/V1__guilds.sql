CREATE TABLE guild
(
    guild_id                VARCHAR(255) NOT NULL,
    name                    VARCHAR(255) NOT NULL,
    bot_admin               BIT(1)       NOT NULL,
    raider_roles_enabled    BIT(1)       NOT NULL,
    raider_role_id          VARCHAR(255) NOT NULL,
    event_templates_enabled BIT(1)       NOT NULL,
    CONSTRAINT pk_guild PRIMARY KEY (guild_id)
);

CREATE TABLE guild_member_entity
(
    external_user_id VARCHAR(255) NOT NULL,
    priority         BIGINT       NOT NULL,
    CONSTRAINT pk_guild_member_entity PRIMARY KEY (external_user_id)
);

CREATE TABLE guild_member_guild
(
    guild_id        VARCHAR(255) NOT NULL,
    guild_member_id VARCHAR(255) NOT NULL,
    CONSTRAINT pk_guild_member_guild PRIMARY KEY (guild_id, guild_member_id)
);

ALTER TABLE guild_member_guild
    ADD CONSTRAINT fk_guimemgui_on_guild_entity FOREIGN KEY (guild_id) REFERENCES guild (guild_id);

ALTER TABLE guild_member_guild
    ADD CONSTRAINT fk_guimemgui_on_guild_member_entity FOREIGN KEY (guild_member_id) REFERENCES guild_member_entity (external_user_id);