CREATE TABLE player_profile
(
    id             BIGINT AUTO_INCREMENT NOT NULL,
    user_id        VARCHAR(255) NOT NULL,
    priority_score INT          NOT NULL,
    CONSTRAINT pk_player_profile PRIMARY KEY (id)
);

ALTER TABLE player_profile
    ADD CONSTRAINT uc_player_profile_user UNIQUE (user_id);