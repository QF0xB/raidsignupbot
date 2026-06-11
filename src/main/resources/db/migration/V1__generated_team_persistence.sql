create table event_entity (
    id bigint not null auto_increment,
    name varchar(255) not null,
    guild_id varchar(255) not null,
    category_id varchar(255) not null,
    start_time timestamp(6) not null,
    duration float not null,
    donation int not null,
    host_discord_user_id varchar(255) not null,
    primary key (id)
);

create table generated_teams (
    id bigint not null auto_increment,
    event_entity_id bigint not null,
    strategy_id bigint null,
    strategy_name varchar(255) not null,
    primary key (id),
    constraint fk_generated_teams_event
        foreign key (event_entity_id) references event_entity (id)
        on delete cascade
);

create index idx_generated_teams_event_entity_id
    on generated_teams (event_entity_id);

create table generated_team_slot_assignments (
    id bigint not null auto_increment,
    team_id bigint not null,
    slot_id bigint null,
    slot_info varchar(255) not null,
    slot_emoji_key varchar(255) not null,
    discord_user_id varchar(255) not null,
    primary key (id),
    constraint fk_generated_team_slot_assignments_team
        foreign key (team_id) references generated_teams (id)
        on delete cascade
);

create index idx_generated_team_slot_assignments_team_id
    on generated_team_slot_assignments (team_id);

create table generated_team_utility_assignments (
    id bigint not null auto_increment,
    team_id bigint not null,
    utility_id bigint null,
    utility_info varchar(255) not null,
    utility_emoji_key varchar(255) not null,
    discord_user_id varchar(255) not null,
    assigned_slot_id bigint null,
    assigned_slot_info varchar(255) not null,
    assigned_slot_emoji_key varchar(255) not null,
    primary key (id),
    constraint fk_generated_team_utility_assignments_team
        foreign key (team_id) references generated_teams (id)
        on delete cascade
);

create index idx_generated_team_utility_assignments_team_id
    on generated_team_utility_assignments (team_id);
