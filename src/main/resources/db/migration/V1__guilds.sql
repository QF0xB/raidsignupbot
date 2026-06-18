CREATE TABLE guild
(
    guild_id        VARCHAR(255) NOT NULL,
    guild_name      VARCHAR(255) NOT NULL,
    create_events   BIT(1)       NOT NULL,
    add_raider_role BIT(1)       NOT NULL,
    raider_role_id  VARCHAR(255) NULL,
    CONSTRAINT pk_guild PRIMARY KEY (guild_id)
);

CREATE TABLE guild_member
(
    id        BIGINT AUTO_INCREMENT NOT NULL,
    guild_id  VARCHAR(255) NOT NULL,
    player_id VARCHAR(255) NOT NULL,
    priority  INT          NOT NULL,
    CONSTRAINT pk_guild_member PRIMARY KEY (id)
);

ALTER TABLE guild_member
    ADD CONSTRAINT uc_guild_member_guild_player UNIQUE (guild_id, player_id);