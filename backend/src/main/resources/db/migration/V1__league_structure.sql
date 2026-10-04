-- The prepared League structure. Natural keys composed down the hierarchy so the wrong data
-- is unrepresentable rather than detected. Every identifier and name column is text not null
-- with a non-empty check.

create table league (
    id                  text primary key check (length(id) > 0),
    name                text not null check (length(name) > 0),
    current_season_id   text
);

create table season_structure (
    league_id   text not null references league (id) check (length(league_id) > 0),
    season_id   text not null check (length(season_id) > 0),
    primary key (league_id, season_id)
);

alter table league add constraint league_current_season_exists
    foreign key (id, current_season_id) references season_structure (league_id, season_id);

create table club (
    id          text primary key check (length(id) > 0),
    league_id   text not null references league (id) check (length(league_id) > 0),
    name        text not null check (length(name) > 0),
    unique (league_id, id)
);

create table competition (
    league_id   text not null check (length(league_id) > 0),
    season_id   text not null check (length(season_id) > 0),
    id          text not null check (length(id) > 0),
    name        text not null check (length(name) > 0),
    primary key (league_id, season_id, id),
    foreign key (league_id, season_id) references season_structure (league_id, season_id)
);

create table competition_phase (
    league_id       text not null check (length(league_id) > 0),
    season_id       text not null check (length(season_id) > 0),
    competition_id  text not null check (length(competition_id) > 0),
    id              text not null check (length(id) > 0),
    name            text not null check (length(name) > 0),
    ordinal         integer not null check (ordinal > 0),
    primary key (league_id, season_id, competition_id, id),
    unique (league_id, season_id, competition_id, ordinal),
    foreign key (league_id, season_id, competition_id)
        references competition (league_id, season_id, id)
);

create table cup_group (
    league_id       text not null check (length(league_id) > 0),
    season_id       text not null check (length(season_id) > 0),
    competition_id  text not null check (length(competition_id) > 0),
    phase_id        text not null check (length(phase_id) > 0),
    id              text not null check (length(id) > 0),
    name            text not null check (length(name) > 0),
    primary key (league_id, season_id, competition_id, id),
    unique (league_id, season_id, competition_id, name),
    foreign key (league_id, season_id, competition_id, phase_id)
        references competition_phase (league_id, season_id, competition_id, id)
);

create table season_placement (
    league_id   text not null check (length(league_id) > 0),
    season_id   text not null check (length(season_id) > 0),
    club_id     text not null check (length(club_id) > 0),
    conference  text not null check (length(conference) > 0),
    division    text not null check (length(division) > 0),
    primary key (league_id, season_id, club_id),
    foreign key (league_id, season_id) references season_structure (league_id, season_id),
    foreign key (league_id, club_id) references club (league_id, id)
);

create table competition_participation (
    league_id       text not null check (length(league_id) > 0),
    season_id       text not null check (length(season_id) > 0),
    competition_id  text not null check (length(competition_id) > 0),
    club_id         text not null check (length(club_id) > 0),
    cup_group_id    text,
    primary key (league_id, season_id, competition_id, club_id),
    foreign key (league_id, season_id, competition_id)
        references competition (league_id, season_id, id),
    foreign key (league_id, season_id, club_id)
        references season_placement (league_id, season_id, club_id),
    foreign key (league_id, season_id, competition_id, cup_group_id)
        references cup_group (league_id, season_id, competition_id, id)
);
